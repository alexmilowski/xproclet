/*
 * WebComponent.java
 *
 * Created on March 26, 2007, 6:04 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.xproclet.server;

import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.restlet.Component;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Server;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

/**
 *
 * @author alex
 */
public class WebComponent extends Component {

   static public String LOG_NAME = "org.xproclet.hosts";
   Configuration config;
   Map<String, Map<String, ConfiguredHost>> ifaceHosts;

   /** Creates a new instance of WebComponent */
   public WebComponent(Configuration config) {
      this.config = config;
      this.ifaceHosts = new TreeMap<String, Map<String, ConfiguredHost>>();
      getLogService().setLoggerName(LOG_NAME);
      
      for (Configuration.App app : config.getApplications()) {
         getLogger().info("Internal application: "+app.getInternalName());
         Context appContext = getContext().createChildContext();
         Router appRouter = new Router(appContext);
         appRouter.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
         app.attach(appRouter);
         getInternalRouter().attach("/"+app.getInternalName(),appRouter);
      }

      for (Configuration.Server serverConf : config.getServers()) {
         getContext().getLogger().info(serverConf.getProtocol().getName()+" listening on " + serverConf.getAddress() + ":" + serverConf.getPort());
         Server server = getServers().add(serverConf.getProtocol(), serverConf.getAddress().equals("*") ? null : serverConf.getAddress(), serverConf.getPort());
         Context targetContext = server.getContext();
         Context sourceContext = serverConf.getContext();
         for (String name : sourceContext.getParameters().getNames()) {
            targetContext.getParameters().add(name,sourceContext.getParameters().getFirstValue(name));
         }
         for (String name : sourceContext.getAttributes().keySet()) {
            targetContext.getAttributes().put(name, sourceContext.getAttributes().get(name));
         }

         Map<String, ConfiguredHost> confHosts = ifaceHosts.get(serverConf.getKey());
         if (confHosts == null) {
            confHosts = new TreeMap<String, ConfiguredHost>();
            ifaceHosts.put(serverConf.getKey(), confHosts);
         }
         // Configure static hosts
         for (String name : serverConf.getHosts().keySet()) {
            Configuration.Host host = serverConf.getHosts().get(name);
            getLogger().info("Host: "+host.getName());
            if (confHosts.get(host.getName()) != null) {
               getLogger().warning("Ignoring duplicate host name " + host.getName());
            } else {
               Context hostContext = getContext().createChildContext();
               
               ConfiguredHost confHost = new ConfiguredHost(hostContext, serverConf, host);
               confHosts.put(host.getName(), confHost);

               getHosts().add(confHost.getVirtualHost());
               for (String alias : host.getAliases()) {
                  getHosts().add(confHost.getVirtualHost(alias));
               }
               getInternalRouter().attach("/"+host.getInternalName(), confHost.getVirtualHost());
            }

         }

      }
      this.getDefaultHost().attach(new Restlet() {

         public void handle(Request request, Response response) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         }
      });

      // Add the clients
      for (Protocol client : config.getClients()) {
         getClients().add(client);
      }

      for (Server server : getServers()) {
         try {
            server.start();
         } catch (Exception ex) {
            getLogger().log(Level.SEVERE,"Cannot start server.",ex);
         }
      }

   }

}
