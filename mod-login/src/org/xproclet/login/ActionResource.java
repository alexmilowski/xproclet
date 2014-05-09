/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import org.restlet.Request;
import org.restlet.data.Reference;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class ActionResource extends ServerResource {

   static Reference getReferenceAttribute(Request request,String name,Reference defaultValue)
   {
      Object o = request.getAttributes().get(name);
      return o==null ? defaultValue : (Reference)o;
   }

   String loginType;
   String confCookiePath;
   String confCookieName;
   String loginApp;
   Reference confService;
   IdentityManager idManager;
   /** Creates a new instance of LoginForm */
   public ActionResource()
   {
      setNegotiated(false);
   }

   protected void doInit() {
      super.doInit();
      confService = null;
      idManager = (IdentityManager)getContext().getAttributes().get(IdentityManager.ATTR);
      if (idManager==null) {
         if (getRequest().isConfidential()) {
            getLogger().warning("No identity manager found for login management.");
         }
      } else {
         getLogger().info("Identity Manager: "+idManager);
      }
      String ref = getContext().getParameters().getFirstValue("login.service");
      if (ref!=null) {
         confService = new Reference(ref);
      } else {
         getLogger().warning("The login.service parameter is missing for "+this.getClass().getName());
      }
      loginType = getContext().getParameters().getFirstValue("login.type");
      loginApp = getContext().getParameters().getFirstValue("login.name");
      if (loginApp==null) {
         loginApp = "restlet-server";
      }
      confCookiePath = getContext().getParameters().getFirstValue("cookie.path");
      if (confCookiePath==null) {
         confCookiePath = "/";
      }
      confCookieName = getContext().getParameters().getFirstValue("cookie.name");
   }
   
   protected String getCookiePath() {
      Object o = getRequest().getAttributes().get("cookie.path");
      return o==null ? confCookiePath : o.toString();
   }
   
   protected String getCookieName() {
      Object o = getRequest().getAttributes().get("cookie.name");
      return o==null ? confCookieName : o.toString();
   }
}
