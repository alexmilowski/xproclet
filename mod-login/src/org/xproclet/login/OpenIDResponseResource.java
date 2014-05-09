/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.util.UUID;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class OpenIDResponseResource extends ServerResource {
   
   OpenIDContext openIDState;
   IdentityManager idManager;
   String confCookiePath;
   String confCookieName;
   
   public OpenIDResponseResource() {
   }
   
   protected void doInit() {
      
      openIDState = (OpenIDContext)getContext().getAttributes().get("openid.context");
      idManager = (IdentityManager)getContext().getAttributes().get(IdentityManager.ATTR);
      if (idManager==null) {
         getLogger().severe("No identity manager could be found, context: "+getContext());
      }
      confCookiePath = getContext().getParameters().getFirstValue("cookie.path");
      if (confCookiePath==null) {
         confCookiePath = "/";
      }
      confCookieName = getContext().getParameters().getFirstValue("cookie.name");
   }
   
   protected Representation get() 
   {
      if (!getRequest().getResourceRef().hasQuery()) {
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("No OpenID response was specified.",MediaType.TEXT_PLAIN);
      } else {
         return fromProvider();
      }
   }
   
   protected Representation fromProvider()
   {
      String id = getRequest().getAttributes().get("id").toString();
      
      if (!openIDState.end(id)) {
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("The request is invalid.",MediaType.TEXT_PLAIN);
      }
      
      Reference googleRef = new Reference(OpenIDRequestResource.GOOGLE_PROVIDER);
      Form responseData = getRequest().getResourceRef().getQueryAsForm();
      for (String name : responseData.getNames()) {
         if (!name.equals("openid.mode")) {
            googleRef.addQueryParameter(name,responseData.getFirstValue(name));
         }
      }
      googleRef.addQueryParameter("openid.mode","check_authentication");
      
      getLogger().info("Checking response...");
      Restlet client = getContext().getClientDispatcher();
      
      Request checkRequest = new Request(Method.GET,googleRef);
      Response checkResponse = client.handle(checkRequest);
      
      if (checkResponse.getStatus().isSuccess()) {
         String [] lines = checkResponse.getEntityAsText().split("\n");  
         boolean valid = false;
         for (int i=0; i<lines.length; i++) {
            if (lines[i].equals("is_valid:true")) {
               valid = true;
            }
         }
         if (valid) {
            String axPrefix = null;
            for (String name : responseData.getNames()) {
               if (name.startsWith("openid.ns") && responseData.getFirstValue(name).equals("http://openid.net/srv/ax/1.0")) {
                  axPrefix = "openid."+name.substring(10);
               }
            }
            if (axPrefix==null) {
               getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               return new StringRepresentation("OpenID provider did not return exchange attributes.",MediaType.TEXT_PLAIN);
            }
            String firstName = responseData.getFirstValue(axPrefix+".value.firstname");
            String lastName = responseData.getFirstValue(axPrefix+".value.lastname");
            String email = responseData.getFirstValue(axPrefix+".value.email");
            Identity user = idManager.get(email);
            if (user==null) {
               Reference unauth = new Reference(getRequest().getResourceRef().getParentRef().getParentRef()+"unauthorized");
               unauth.addQueryParameter("first",firstName);
               unauth.addQueryParameter("last",lastName);
               unauth.addQueryParameter("email",email);
               getResponse().redirectSeeOther(unauth);
               return null;
            }
            Identity identity = user.createSessionIdentity(UUID.randomUUID().toString());
            getLogger().info("Authenticated "+firstName+" "+lastName+" <"+email+"> as "+identity.getAlias());
            String name = getCookieName(getRequest());
            if (name!=null) {
               CookieSetting cookie = new CookieSetting("I",identity.getSession());
               cookie.setPath(getCookiePath(getRequest()));
               getResponse().getCookieSettings().add(cookie);
               idManager.add(identity.getSession(), identity);
            }
            IdentityFilter.addIdentity(getRequest(), identity);
            getResponse().redirectSeeOther(getRequest().getResourceRef().getParentRef().getParentRef());
            return null;
         } else {
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("OpenID provider would not validate request.",MediaType.TEXT_PLAIN);
         }
      } else {
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("Response was not valid from OpenID provider.",MediaType.TEXT_PLAIN);
      }
      
      /*
      openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0
      openid.mode=id_res
      openid.op_endpoint=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fud
      openid.response_nonce=2011-09-26T22%3A32%3A31ZYmws56wum4QUww
      openid.return_to=https%3A%2F%2Ftest.milowski.com%3A8081%2Fauth
      openid.assoc_handle=AOQobUf4SZeLEzQiLX-bSaTaiy3qCO51oAzkEkP4bsw4y3wiA0C7LrJN
      openid.signed=op_endpoint%2Cclaimed_id%2Cidentity%2Creturn_to%2Cresponse_nonce%2Cassoc_handle%2Cns.ext1%2Cext1.mode%2Cext1.type.firstname%2Cext1.value.firstname%2Cext1.type.email%2Cext1.value.email%2Cext1.type.lastname%2Cext1.value.lastname
      openid.sig=RzT0857i%2FRKu475u3%2FDdrVQhz2w%3D
      openid.identity=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawkyfUnes8LgECz3k28EPAJ7h8op-2wLaZE
      openid.claimed_id=https%3A%2F%2Fwww.google.com%2Faccounts%2Fo8%2Fid%3Fid%3DAItOawkyfUnes8LgECz3k28EPAJ7h8op-2wLaZE
      openid.ns.ext1=http%3A%2F%2Fopenid.net%2Fsrv%2Fax%2F1.0
      openid.ext1.mode=fetch_response
      openid.ext1.type.firstname=http%3A%2F%2Faxschema.org%2FnamePerson%2Ffirst
      openid.ext1.value.firstname=Alex
      openid.ext1.type.email=http%3A%2F%2Faxschema.org%2Fcontact%2Femail
      openid.ext1.value.email=alexml%40milowski.com
      openid.ext1.type.lastname=http%3A%2F%2Faxschema.org%2FnamePerson%2Flast
      openid.ext1.value.lastname=Milowski      
       * 
       */
   }
   
   protected String getCookiePath(Request request) {
      Object o = request.getAttributes().get("cookie.path");
      return o==null ? confCookiePath : o.toString();
   }
   
   protected String getCookieName(Request request) {
      Object o = request.getAttributes().get("cookie.name");
      return o==null ? confCookieName : o.toString();
   }
}
