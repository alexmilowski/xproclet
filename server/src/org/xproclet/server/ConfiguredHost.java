/*
 * ConfiguredHost.java
 *
 * Created on November 2, 2007, 2:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.engine.log.AccessLogFileHandler;
import org.restlet.routing.Filter;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.VirtualHost;

/**
 *
 * @author alex
 */
public class ConfiguredHost
{
   class AppInfo {
      Application app;
      Date edited;
      String match;
      AppInfo(Application app,Date edited) {
         this.app = app;
         this.edited = edited;
         this.match = null;
      }
   }
   
   Context context;
   VirtualHost defaultVHost;
   Configuration.Server server;
   Configuration.Host hostConf;
   Router router;
   Logger hostLog;
   
   /** Creates a new instance of ConfiguredHost */
   public ConfiguredHost(Context context,Configuration.Server server,Configuration.Host hostConf)
   {
      this.context = context;
      this.server = server;
      this.hostConf = hostConf;
      this.router = new Router(context);
      this.router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
      hostConf.attach(router);
      if (hostConf.getLogConfiguration().get("pattern")!=null) {
         String name = hostConf.getName();
         if (name.equals("*")) {
            name = "any";
         }
         name = "host."+name;
         this.hostLog = Logger.getLogger(name);
         setupLog();
      }
      this.defaultVHost = getVirtualHost(hostConf.getName());
      
   }

   public Context getContext() {
      return context;
   }
   
   public Logger getLogger() {
      return context.getLogger();
   }
   
   public VirtualHost getVirtualHost() {
      return defaultVHost;
   }
   
   public VirtualHost getVirtualHost(String name) {
      //context.getAttributes().put(WebComponent.LINKS_ATTR, hostConf.getLinks());
      getLogger().info("Adding host "+name+":"+server.getPort());
      VirtualHost vhost = new VirtualHost(context) {
         public void handle(Request request, Response response) {
            long startTime = System.currentTimeMillis();
            super.handle(request,response);
            if (hostLog!=null) {
               int duration = (int) (System.currentTimeMillis() - startTime);
               hostLog.log(Level.INFO,formatLog(request,response,duration));
            }
         }
      };
      if (!server.getAddress().equals("*")) {
         try {
            InetAddress addr = InetAddress.getByName(server.getAddress());
            String saddr = addr.toString();
            saddr = saddr.substring(saddr.indexOf('/')+1);
            getLogger().info("Restricting "+hostConf.getName()+" to address "+saddr);
            vhost.setServerAddress(saddr);
         } catch (UnknownHostException ex) {
            getLogger().severe("Cannot resolve host name "+server.getAddress());
         }
      }
      if (!name.equals("*")) {
         vhost.setHostDomain(name);
      }
      vhost.setHostPort(Integer.toString(server.getPort()));
      //router = vhost;
      final ServerInfo serverInfo = new ServerInfo();
      serverInfo.setAgent(server.getAgent());
      Filter filter = new Filter(vhost.getContext()) {
         protected int beforeHandle(Request request, Response response) {
            response.setServerInfo(serverInfo);
            return Filter.CONTINUE;
         }
      };
      
      vhost.attachDefault(filter);
      filter.setNext(router);
      return vhost;
   }
   
   protected String formatLog(Request request, Response response,int duration) {
      StringBuilder sb = new StringBuilder();
      long currentTime = System.currentTimeMillis();

      // Append the date of the request
      sb.append(String.format("%tF", currentTime));
      sb.append('\t');

      // Append the time of the request
      sb.append(String.format("%tT", currentTime));
      sb.append('\t');

      // Append the client IP address
      String clientAddress = request.getClientInfo().getUpstreamAddress();
      sb.append((clientAddress == null) ? "-" : clientAddress);
      sb.append('\t');

      // Append the user name (via IDENT protocol)
      if ((request.getChallengeResponse() != null) && (request.getChallengeResponse().getIdentifier() != null)) {
         sb.append(request.getChallengeResponse().getIdentifier());
      } else {
         // [enddef]
         sb.append('-');
      }
      sb.append('\t');

      // Append the server IP address
      String serverAddress = response.getServerInfo().getAddress();
      sb.append((serverAddress == null) ? "-" : serverAddress);
      sb.append('\t');

      // Append the server port
      Integer serverport = response.getServerInfo().getPort();
      sb.append((serverport == null) ? "-" : serverport.toString());
      sb.append('\t');

      // Append the method name
      String methodName = (request.getMethod() == null) ? "-" : request.getMethod().getName();
      sb.append((methodName == null) ? "-" : methodName);

      // Append the resource path
      sb.append('\t');
      String resourcePath = (request.getResourceRef() == null) ? "-"
              : request.getResourceRef().getPath();
      sb.append((resourcePath == null) ? "-" : resourcePath);

      // Append the resource query
      sb.append('\t');
      String resourceQuery = (request.getResourceRef() == null) ? "-"
              : request.getResourceRef().getQuery();
      sb.append((resourceQuery == null) ? "-" : resourceQuery);

      // Append the status code
      sb.append('\t');
      sb.append((response.getStatus() == null) ? "-" : Integer.toString(response.getStatus().getCode()));

      // Append the returned size
      sb.append('\t');

      if (!response.isEntityAvailable()
              || Status.REDIRECTION_NOT_MODIFIED.equals(response.getStatus())
              || Status.SUCCESS_NO_CONTENT.equals(response.getStatus())
              || Method.HEAD.equals(request.getMethod())) {
         sb.append('0');
      } else {
         sb.append((response.getEntity().getSize() == -1) ? "-" : Long.toString(response.getEntity().getSize()));
      }

      // Append the received size
      sb.append('\t');

      if (request.getEntity() == null) {
         sb.append('0');
      } else {
         sb.append((request.getEntity().getSize() == -1) ? "-" : Long.toString(request.getEntity().getSize()));
      }

      // Append the duration
      sb.append('\t');
      sb.append(duration);

      // Append the host reference
      sb.append('\t');
      sb.append((request.getHostRef() == null) ? "-" : request.getHostRef().toString());

      // Append the agent name
      sb.append('\t');
      String agentName = request.getClientInfo().getAgent();
      sb.append((agentName == null) ? "-" : agentName);

      // Append the referrer
      sb.append('\t');
      sb.append((request.getReferrerRef() == null) ? "-" : request.getReferrerRef().getIdentifier());

      return sb.toString();
   }

   public void setupLog() {
      if (hostLog==null) {
         return;
      }
      String pattern = hostConf.getLogConfiguration().get("pattern");
      String value = hostConf.getLogConfiguration().get("limit");
      int limit = 10*1024*1024;
      if (value!=null) {
         try {
            limit = Integer.parseInt(value);
         } catch (NumberFormatException ex) {
            getLogger().log(Level.SEVERE,"Cannot parse limit value "+value+" for log configuration: "+ex.getMessage());
         }
      }
      value = hostConf.getLogConfiguration().get("count");
      int count = 100;
      if (value!=null) {
         try {
            count = Integer.parseInt(value);
         } catch (NumberFormatException ex) {
            getLogger().log(Level.SEVERE,"Cannot parse count value "+value+" for log configuration: "+ex.getMessage());
         }
      }
      boolean append = true;
      value = hostConf.getLogConfiguration().get("append");
      if (value!=null) {
         append = value.equals("true");
      }
      try {
         AccessLogFileHandler handler = new AccessLogFileHandler(pattern,limit,count,append);
         value = hostConf.getLogConfiguration().get("encoding");
         if (value!=null) {
            handler.setEncoding(value);
         }
         value = hostConf.getLogConfiguration().get("level");
         handler.setLevel(Level.ALL);
         if (value!=null) {
            try {
               handler.setLevel(Level.parse(value));
            } catch (Exception ex) {
              getLogger().log(Level.SEVERE,"Cannot parse level value "+value+" for log configuration: "+ex.getMessage());
            }
         }
         hostLog.addHandler(handler);
      } catch (IOException ex) {
         getLogger().log(Level.SEVERE,"Cannot instantiate access log file handler for host.",ex);
      }
   }
   
   
}
