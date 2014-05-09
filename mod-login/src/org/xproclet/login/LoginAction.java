/*
 * LoginForm.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;
import java.util.logging.Level;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.w3c.dom.Document;
import org.xproclet.server.DocumentLoader;

/**
 *
 * @author alex
 */
public class LoginAction extends ActionResource
{
   public static final String GOOGLE_CLIENT_LOGIN = "google.ClientLogin";
   public interface LoginActor {
      public void authenticated(Form authForm,Identity identity);
      public void unauthorized();
   }

   static String toString(InputStream is)
      throws IOException
   {
      if (is==null) {
         return null;
      }
      StringBuilder builder = new StringBuilder();
      Reader r = new InputStreamReader(is,"UTF-8");
      char [] buffer = new char[1024];
      int len;
      while ((len=r.read(buffer))>0) {
         builder.append(buffer,0,len);
      }
      return builder.toString();
   }
   
   protected LoginActor actor;

   /** Creates a new instance of LoginForm */
   public LoginAction()
   {
      actor = new LoginActor() {
         public void authenticated(Form authForm,Identity identity)
         {
            String name = getCookieName();
            if (name!=null) {
               CookieSetting cookie = new CookieSetting("I",identity.getSession());
               cookie.setPath(getCookiePath());
               getResponse().getCookieSettings().add(cookie);
            }
            if (name!=null && idManager!=null) {
               idManager.add(identity.getSession(), identity);
            }
            String redirect = authForm.getFirstValue("redirect");
            if (redirect!=null && redirect.length()!=0) {
               getResponse().redirectSeeOther(redirect);
            } else {
               getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            }
         }
         public void unauthorized() {
            getResponse().setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
         }
      };
   }

   public Representation post(Representation rep) {
      Reference service = getReferenceAttribute(getRequest(),"auth-service",confService);
      if (service==null) {
         getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         return null;
      }
      getLogger().info("Using service "+service);
      final Form form = new Form(rep);
      String username = form.getFirstValue("username");
      String domain = form.getFirstValue("domain");
      String email = username;
      if (domain!=null && domain.length()>0 && email.indexOf('@')<0) {
         email += "@"+domain;
      }
      String password = form.getFirstValue("password");
      Identity user = idManager.get(email);
      if (user==null) {
         getLogger().info("Unknown user "+email);
         actor.unauthorized();
         return null;
      }
      login(getContext().createChildContext(),service,loginApp,loginType,username,password,email,form,actor);
      return null;
   }
   
   public static void login(Context context,Reference service,String application,String type,String username, String password, String email,Form form,LoginActor actor)
   {
      boolean isGoogle = GOOGLE_CLIENT_LOGIN.equals(type);
      //Client client = context.getClientDispatcher();
      Client client = new Client(context.createChildContext(),service.getSchemeProtocol());
      //client.getContext().getAttributes().put("hostnameVerifier", org.apache.commons.ssl.HostnameVerifier.DEFAULT);
      if (isGoogle) {
         Request request = new Request(Method.POST,service);
         Form authForm = new Form();
         context.getLogger().info("Performing google auth for "+username);
         authForm.add("accountType", "HOSTED_OR_GOOGLE");
         authForm.add("service", "apps");
         authForm.add("source", application);
         authForm.add("Email", username);
         authForm.add("Passwd", password);
         request.setEntity(authForm.getWebRepresentation());
         Response response = client.handle(request);
         if (response.getStatus().isSuccess()) {
            context.getLogger().info("Authenticated "+username);
            actor.authenticated(form,new Identity(UUID.randomUUID().toString(),username,username,username,email));
         } else {
            context.getLogger().info("Authorization request for "+username+" returned: "+response.getStatus().getCode());
            actor.unauthorized();
         }
         if (response.isEntityAvailable()) {
            try {
               response.getEntity().exhaust();
            } catch (IOException ex) {
               context.getLogger().log(Level.SEVERE,"Cannot read response due to I/O error.",ex);
            }
            //response.getEntity().release();
         }
      } else {
         Request request = new Request(Method.GET,service);
         request.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC,username,password));
         Response response = client.handle(request);
         if (response.getStatus().isSuccess()) {
            DocumentLoader docLoader = new DocumentLoader();
            try {
               Document doc = docLoader.load(response.getEntity().getReader());
               Identity identity = IdentityFilter.createIdentity(doc.getDocumentElement());
               context.getLogger().info("Authenticated "+username);
               actor.authenticated(form,identity);
            } catch (Exception ex) {
               context.getLogger().log(Level.SEVERE,"Cannot parse auth result.",ex);
               actor.unauthorized();
            }
         } else {
            context.getLogger().info("Authorization request for "+username+" returned: "+response.getStatus().getCode());
            actor.unauthorized();
         }
         if (response.isEntityAvailable()) {
            try {
               response.getEntity().exhaust();
            } catch (IOException ex) {
               context.getLogger().log(Level.SEVERE,"Cannot read response due to I/O error.",ex);
            }
            //response.getEntity().release();
         }
      }
      try {
         client.stop();
      } catch (Exception ex) {
         context.getLogger().log(Level.SEVERE,"Cannot stop client.",ex);
      }
      client = null;
   }
   
}
