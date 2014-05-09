/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.routing.Filter;
import org.restlet.routing.Template;
import org.restlet.routing.Variable;

/**
 *
 * @author alex
 */
public class SecurityGuard extends Filter {

   public class SecureArea {
      
      Reference loginPage;
      Reference notAuthorized;
      Set<URI> requiredRoles;
      
      public SecureArea(Reference loginPage,Reference notAuthorized) {
         this.loginPage = loginPage;
         this.notAuthorized = notAuthorized;
         this.requiredRoles = new TreeSet<URI>();
      }

      public Set<URI> getRequiredRoles() {
         return requiredRoles;
      }
      
      public Reference getLoginReference() {
         return loginPage;
      }
      
      public Reference getNotAuthorizedReference() {
         return notAuthorized;
      }

      int isAuthorized(Request request) {
         Identity id = (Identity)request.getAttributes().get(Identity.IDENTITY_ATTR);
         if (id==null) {
            return -1;
         } else {
            for (URI role : requiredRoles) {
               if (!id.hasRole(role)) {
                  getLogger().info("Identity "+id.getId()+","+id.getAlias()+" is missing role "+role+" , redirecting to "+notAuthorized);
                  return 0;
               }
            }
            return 1;
         }
         
      }
   }
   
   public class SecureRoute {
      Template template;
      SecureArea area;
      SecureRoute(String uriPattern,SecureArea area) {
         this.template = new Template(uriPattern,Template.MODE_STARTS_WITH,Variable.TYPE_URI_SEGMENT,"",true,false);
         this.area = area;
      }
      
      public Template getTemplate() {
         return template;
      }
      
      public SecureArea getArea() {
         return area;
      }
      
      public boolean matches(Request request) {
         String remainingPart = request.getResourceRef().getRemainingPart();
         return template.match(remainingPart)>=0;
      }
      
   }

   List<SecureRoute> routes;
   
   public SecurityGuard(Context context) {
      super(context);
      routes = new ArrayList<SecureRoute>();
   }
   public void start() 
      throws Exception
   {
      if (!isStarted()) {
         getLogger().info("Configuring security gaurd...");
         String pattern = getContext().getParameters().getFirstValue("pattern");
         if (pattern!=null) {
            getLogger().info("Securing: "+pattern);
            String href = getContext().getParameters().getFirstValue("login.href");
            Reference loginPage = href==null ? null : new Reference(href);
            href = getContext().getParameters().getFirstValue("notauthorized.href");
            Reference notAuthorized = href==null ? null : new Reference(href);
            SecureRoute secureRoute = addSecureArea(pattern,loginPage,notAuthorized);
            String rolesSpec = getContext().getParameters().getFirstValue("roles");
            if (rolesSpec!=null) {
               String [] roles = rolesSpec.split("\\s+");
               for (int i=0; i<roles.length; i++) {
                  getLogger().info("  Role: "+roles[i]);
                  secureRoute.area.getRequiredRoles().add(new URI(roles[i]));
               }
            }
         } else {
            getLogger().info("No secure area pattern specified.");
         }
      }
      super.start();
   }
   
   public List<SecureRoute> getRoutes() {
      return routes;
   }
   
   public SecureRoute addSecureArea(String uriPattern,Reference loginPage,Reference notAuthorized)
   {
      SecureArea area = new SecureArea(loginPage,notAuthorized);
      SecureRoute route = new SecureRoute(uriPattern,area);
      routes.add(route);
      return route;
   }

   protected int beforeHandle(Request request,Response response) {
      for (SecureRoute route : routes) {
         getLogger().info("Checking "+route.getTemplate().getPattern()+" against "+request.getResourceRef().getRemainingPart());
         if (route.matches(request)) {
            getLogger().info("Route matched, checking authorization...");
            switch (route.area.isAuthorized(request)) {
               case -1: // No identity
                  if (route.area.getLoginReference()==null) {
                     response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
                  } else {
                     getLogger().info("No identity, redirecting to "+route.area.getLoginReference()+" for login.");
                     response.redirectSeeOther(route.area.getLoginReference()+"?url="+request.getResourceRef());
                  }
                  return Filter.STOP;
               case 0: // Not authorized
                  if (route.area.getNotAuthorizedReference()==null) {
                     response.setStatus(Status.CLIENT_ERROR_FORBIDDEN);
                  } else {
                     getLogger().info("Not authorized , redirecting to "+route.area.getNotAuthorizedReference());
                     response.redirectSeeOther(route.area.getNotAuthorizedReference()+"?url="+request.getResourceRef());
                  }
                  return Filter.STOP;
               case 1: // OK
                  return Filter.CONTINUE;
            }
         }
      }
      return Filter.CONTINUE;
   }

}
