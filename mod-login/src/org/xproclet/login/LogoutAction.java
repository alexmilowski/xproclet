/*
 * LoginForm.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Cookie;
import org.restlet.data.CookieSetting;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;

/**
 *
 * @author alex
 */
public class LogoutAction extends ActionResource
{
   
   /** Creates a new instance of LoginForm */
   public LogoutAction()
   {
   }

   public Representation get()
   {
      Reference service = ActionResource.getReferenceAttribute(getRequest(),"login.service",confService);
      String name = getCookieName();
      Cookie cookie = null;
      if (name!=null) {
         cookie = getRequest().getCookies().getFirst(name);
         CookieSetting unset = new CookieSetting(name,"");
         unset.setMaxAge(0);
         unset.setPath(getCookiePath());
         getResponse().getCookieSettings().add(unset);
      }
      getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
      if (cookie!=null && service!=null) {
         Restlet client = getContext().getClientDispatcher();
         //Client client = new Client(getContext().createChildContext(),service.getSchemeProtocol());
         //client.getContext().getAttributes().put("hostnameVerifier", org.apache.commons.ssl.HostnameVerifier.DEFAULT);
         Response response = client.handle(new Request(Method.DELETE,service+"/"+cookie.getValue()));
         if (!response.getStatus().isSuccess() && response.getStatus().getCode()!=404) {
            getLogger().warning("Auth service returned "+response.getStatus().getCode()+" for session "+cookie.getValue());
         }
         if (response.isEntityAvailable()) {
            response.getEntity().release();
         }
      }
      return null;
   }   
   
}
