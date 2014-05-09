/*
 * LoginView.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Template;
import org.restlet.util.Resolver;

/**
 *
 * @author alex
 */
public class LoginView extends LoginAction
{
   
   Reference confSecureBase;
   /**
    * Creates a new instance of LoginView
    */
   public LoginView()
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
            Form form = getRequest().getResourceRef().getQueryAsForm();
            form.add("error","Login failed.");
            getResponse().setEntity(get(null,form));
         }
      };
   }

   protected void doInit() {
      super.doInit();
      String ref = getContext().getParameters().getFirstValue("login.secure.href");
      if (ref!=null) {
         confSecureBase = new Reference(ref);
      } else {
         getLogger().warning("The login.secure.href parameter is missing for "+this.getClass().getName());
      }
   }
   
   
   public Representation get()
   {
      Identity identity = (Identity)getRequest().getAttributes().get(Identity.IDENTITY_ATTR);
      Form form = getRequest().getResourceRef().getQueryAsForm();
      return get(identity,form);
   }
   
   public Representation get(final Identity identity,final Form form)
   {
      if (identity==null) {
         try {
            final Template template = new Template(LoginAction.toString(LoginView.class.getResourceAsStream("templates/login-form.xml")));
            String action = getRequest().getResourceRef().getPath();
            if (!getRequest().isConfidential()) {
               Reference base = ActionResource.getReferenceAttribute(getRequest(), "secure-base", confSecureBase);
               if (base==null) {
                  getLogger().warning("The secure-base link is missing login.");
               } else {
                  action = base+action.substring(1);
               }
            }
            final String formAction = action;
            final String url = form.getFirstValue("url")==null ? getRequest().getResourceRef().toString() : form.getFirstValue("url");
            final String result = template.format(new Resolver<String>() {
               public String resolve(String name) {
                  if (name.equals("action")) {
                     return formAction;
                  } else if (name.equals("url")) {
                     return url;
                  } else {
                     return form.getFirstValue(name);
                  }
               }
            });
            return new OutputRepresentation(MediaType.APPLICATION_XHTML) {
               public void write(OutputStream os) 
                  throws IOException
               {
                  OutputStreamWriter w = new OutputStreamWriter(os,"UTF-8");
                  w.write(result);
                  w.flush();
               }
            };
         } catch (IOException ex) {
            getLogger().log(Level.SEVERE,"Cannot get template.",ex);
            return null;
         }
      } else {
         try {
            final Template template = new Template(LoginAction.toString(LoginView.class.getResourceAsStream("templates/logged-in.xml")));
            final String result = template.format(new Resolver<String>() {
               public String resolve(String name) {
                  if (name.equals("id")) {
                     return identity.getId();
                  } else if (name.equals("alias")) {
                     return identity.getAlias();
                  } else if (name.equals("name")) {
                     return identity.getName();
                  } else if (name.equals("email")) {
                     return identity.getEmail();
                  }
                  return null;
               }
            });
            
            return new OutputRepresentation(MediaType.APPLICATION_XHTML) {
               public void write(OutputStream os) 
                  throws IOException
               {
                  OutputStreamWriter w = new OutputStreamWriter(os,"UTF-8");
                  w.write(result);
                  w.flush();
               }
            };
         } catch (IOException ex) {
            getLogger().log(Level.SEVERE,"Cannot get template.",ex);
            return null;
         }
         
      }
   }
   
}
