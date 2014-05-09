/*
 * LoginApplication.java
 *
 * Created on September 7, 2007, 10:05 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.io.InputStream;
import java.util.logging.Level;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Metadata;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.service.MetadataService;

/**
 *
 * This application uses the following constructs:
 *   * a link called 'auth-service' that points to the authentication service,
 *   * the identity manager attribute in the context to maintain sessions,
 *   * a parameter 'login.type' that can be "google.ClientLogin" or empty,
 *   * a parameter 'login.name' an optional name to be sent to the authentication service,
 *   * a parameter 'login.cookie.name' used to save authentication.  If set, a cookie will be sent to the client,
 *   * a parameter 'login.cookie.path' for the path to set the cookie for authentication,
 *   * a parameter 'login.view' that contains a boolean to control enabling login form.  Defaults to true.
 * @author alex
 */
public class LoginApplication extends Application
{
   static class ClassResource extends ServerResource
   {

      Class baseClass;
      String path;
      MediaType type;

      /** Creates a new instance of ClassResource */
      public ClassResource(Class baseClass,String path)
      {
         setNegotiated(false);
         this.baseClass = baseClass;
         this.path = path;
         int extPos = path.lastIndexOf('.');
         Application app = this.getApplication();
         type = app.getMetadataService().getDefaultMediaType(); 
         if (extPos>=0) {
            String ext = path.substring(extPos+1);
            Metadata mdata = this.getApplication().getMetadataService().getMetadata(ext);
            if (mdata!=null) {
               type = MediaType.valueOf(mdata.getName());
            }
         }
      }

      public Representation get()
      {
         if (getLogger().isLoggable(Level.FINE)) {
            getLogger().info("Class resource: "+path);
         }
         InputStream is = baseClass.getResourceAsStream(path);
         if (is==null) {
            return null;
         } else {
            return new InputRepresentation(is,type);
         }
      }

   }
   
   static class ClassResourceFinder extends Finder
   {

      Class baseClass;
      String packageName;

      /** Creates a new instance of ClassResourceFinder */
      public ClassResourceFinder(Context context,Class baseClass,String path)
      {
         super(context);
         this.baseClass = baseClass;
         this.packageName = path.length()>0 && path.charAt(0)=='/' ? path : "/"+baseClass.getPackage().getName().replace('.','/')+"/"+path;
         if (!this.packageName.endsWith("/")) {
            this.packageName += "/";
         }
      }

      public ServerResource find(Request request,Response response)
      {
         String path = packageName+request.getResourceRef().getRemainingPart();
         ServerResource r = new ClassResource(baseClass,path);
         r.setRequest(request);
         r.setResponse(response);
         return r;
      }

   }
   
   MetadataService metadataService;
   /**
    * Creates a new instance of LoginApplication
    */
   public LoginApplication(Context context)
   {
      super(context);
      getTunnelService().setEnabled(false);
      
      Object idManager = context.getAttributes().get(IdentityManager.ATTR);
      if (idManager==null) {
         getLogger().warning("There is no "+IdentityManager.ATTR+" attribute.");
      } else {
         getContext().getAttributes().put(IdentityManager.ATTR,idManager);
      }
      metadataService = null;
   }
   
   public MetadataService getMetadataService() {
      MetadataService service = super.getMetadataService();
      if (service==null) {
         if (metadataService==null) {
            metadataService = new MetadataService();
            metadataService.addCommonExtensions();
         }
         return metadataService;
      }
      return service;
   }
   
   public Restlet createInboundRoot() {
      String viewValue = getContext().getParameters().getFirstValue("login.view");
      boolean viewEnabled = viewValue==null || "true".equals(viewValue.trim());
      
      Router router = new Router(getContext());
      if (viewEnabled) {
         getLogger().info("Enabling view on login.");
         router.attach("/",LoginView.class);
         router.attach("/js/",new ClassResourceFinder(getContext(),LoginApplication.class,"js")).getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
         router.attach("/logout",LogoutView.class);
      }  
      router.attach("/auth",LoginAction.class);
      router.attach("/status/check",CheckAction.class);
      router.attach("/status",StatusAction.class);
      router.attach("/expire",LogoutAction.class);
      return router;
   }
   
}
