/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class LogFilter extends Filter {

   Logger logLogger;

   LogFilter(Context context) {
      super(context);
      logLogger = context.getLogger();
   }
   
   public void setLogger(Logger logger) {
      logLogger = logger;
   }

   protected int beforeHandle(Request request, Response response) {
      request.getAttributes().put("org.restlet.startTime",
              System.currentTimeMillis());

      return CONTINUE;
   }

   protected void afterHandle(Request request, Response response) {
      try {
         long startTime = (Long) request.getAttributes().get(
                 "org.restlet.startTime");
         int duration = (int) (System.currentTimeMillis() - startTime);
         this.logLogger.log(Level.INFO, formatDefault(request, response,
                 duration));
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Error formatting access log message.",ex);
      }
   }

   /**
    * Format a log entry using the default format.
    *
    * @param request
    *            The request to log.
    * @param response
    *            The response to log.
    * @param duration
    *            The call duration (in milliseconds).
    * @return The formatted log entry.
    */
   protected String formatDefault(Request request, Response response,int duration) {
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
}
