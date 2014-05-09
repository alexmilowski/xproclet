/*
 * ContentApplication.java
 *
 * Created on June 25, 2007, 10:36 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.routing.Template;
import org.restlet.util.Series;

/**
 *
 * @author alex
 */
public class ProxyApplication extends Application
{
   static ChallengeScheme HTTP_BEARER = new ChallengeScheme("HTTP_Bearer","Bearer");
   static class HeaderSpec {
      enum Type {
         Replace,
         OnlyIfNotPresent,
         Additive
      }
      Type type;
      String value;
      HeaderSpec(Type type,String value) {
         this.type = type;
         this.value = value;
      }
   }
   String resourceBase;
   String username;
   String password;
   String accessToken;
   Set<Method> allowedMethods;
   List<String> headersToCopy;
   Map<String,HeaderSpec> headersToAdd;
   boolean trimLeadingSlash;
   boolean treatAsTemplate;
   
   static String getStringAttribute(Context context,String name)
   {
      Object obj = context.getAttributes().get(name);
      return obj==null ? null : obj.toString();
   }
   
   /** Creates a new instance of ContentApplication */
   public ProxyApplication(Context context,String resourceBase)
   {
      super(context);
      this.treatAsTemplate = false;
      this.resourceBase = resourceBase;
      this.trimLeadingSlash = resourceBase.charAt(resourceBase.length()-1)=='/';
      this.username = null;
      this.accessToken = null;
      getTunnelService().setEnabled(false);
      allowedMethods = new TreeSet<Method>();
      String [] methods = context.getParameters().getValuesArray("method");
      for (int i=0; i<methods.length; i++) {
         allowedMethods.add(Method.valueOf(methods[i]));
      }
      headersToCopy = new ArrayList<String>();
      String [] headers = context.getParameters().getValuesArray("forward-header");
      for (int i=0; i<headers.length; i++) {
         headersToCopy.add(headers[i]);
      }
      headersToAdd = new TreeMap<String,HeaderSpec>();
      headers = context.getParameters().getValuesArray("header");
      for (int i=0; i<headers.length; i++) {
         int eq = headers[i].indexOf("=");
         if (eq>0) {
            String name = headers[i].substring(0,eq);
            String value = headers[i].substring(eq+1);
            if (name.charAt(0)=='?') {
               headersToAdd.put(name.substring(1),new HeaderSpec(HeaderSpec.Type.OnlyIfNotPresent,value));
            } else if (name.charAt(0)=='!') {
               headersToAdd.put(name.substring(1),new HeaderSpec(HeaderSpec.Type.Replace,value));
            } else {
               headersToAdd.put(name,new HeaderSpec(HeaderSpec.Type.Additive,value));
            }
         }
      }
   }
   
   public void setIdentity(String username,String password)
   {
      this.username = username;
      this.password = password;
   }
   
   public void setAccessToken(String accessToken)
   {
      this.accessToken = accessToken;
   }
   
   public void setTemplateMode(boolean flag)
   {
      this.treatAsTemplate = flag;
   }
   
   public Restlet createInboundRoot() {
      return new Restlet(getContext()) {
         boolean isFineLog = getLogger().isLoggable(Level.FINE);
         Template template = treatAsTemplate ? new Template(resourceBase) : null;
         public void handle(Request request,Response response)
         {
            if (allowedMethods.size()>0 && !allowedMethods.contains(request.getMethod())) {
               response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
               return;
            }
            Reference ref = null;
            if (treatAsTemplate) {
               String uri = template.format(request.getAttributes());
               ref = new Reference(uri);
            } else {
               String path = request.getResourceRef().getRemainingPart();
               if (path==null) {
                  path = request.getResourceRef().getPath();
               }
               if (trimLeadingSlash && path.length()>0 && path.charAt(0)=='/') {
                  path = path.substring(1);
               }
               ref = new Reference(resourceBase+path);
            }
            if (request.getResourceRef().hasQuery()) {
               ref.setQuery(request.getResourceRef().getQuery());
            }
            if (isFineLog) {
               getLogger().fine("Proxy to "+ref);
            }
            Restlet client = getContext().getClientDispatcher();
            client.getContext().getAttributes().put("hostnameVerifier", org.apache.commons.ssl.HostnameVerifier.DEFAULT);
            
            Request appRequest = new Request(request.getMethod(),ref);
            appRequest.setHostRef(ref.getHostIdentifier());
            if (request.isEntityAvailable()) {
               appRequest.setEntity(request.getEntity());
            }
            Series<Header> proxyHeaders = (Series<Header>)appRequest.getAttributes().get("org.restlet.http.headers");
            if (proxyHeaders==null) {
               proxyHeaders = new Series<Header>(Header.class);
               appRequest.getAttributes().put("org.restlet.http.headers",proxyHeaders);
            }
            Series<Header> requestHeaders = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
            if (headersToCopy.size()>0 && requestHeaders!=null) {
               for (String name : headersToCopy) {
                  String [] values = requestHeaders.getValuesArray(name);
                  for (int i=0; i<values.length; i++) {
                     proxyHeaders.add(name,values[i]);
                  }
               }
            }
            if (headersToAdd.size()>0) {
               for (String name : headersToAdd.keySet()) {
                  HeaderSpec spec = headersToAdd.get(name);
                  switch (spec.type) {
                     case Replace:
                        proxyHeaders.removeAll(name);
                        proxyHeaders.add(name,spec.value);
                        break;
                     case OnlyIfNotPresent:
                        if (proxyHeaders.getFirstValue(name)==null) {
                           proxyHeaders.add(name,spec.value);
                        }
                        break;
                     default:
                        proxyHeaders.add(name,spec.value);
                  }
               }
            }
            String forwardedHost = requestHeaders==null ? null : requestHeaders.getFirstValue("X-Forwarded-Host");
            if (forwardedHost==null) {
               Reference hostRef= request.getHostRef();
               if (hostRef==null) {
                  Reference resourceRef = request.getResourceRef();
                  if (resourceRef.getSchemeProtocol().equals(Protocol.RIAP)) {
                     forwardedHost = "riap://"+resourceRef.getAuthority();
                  } else {
                     forwardedHost = resourceRef.getHostIdentifier();
                  }
               } else {
                  forwardedHost = hostRef.toString();
               }
            }
            proxyHeaders.add("X-Forwarded-Host",forwardedHost);
            if (username!=null) {
               appRequest.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC,username,password));
            } else if (accessToken!=null) {
               ChallengeResponse tokenResponse = new ChallengeResponse(HTTP_BEARER);
               tokenResponse.setRawValue(accessToken);
               
               appRequest.setChallengeResponse(tokenResponse);
            } else if (request.getChallengeResponse()!=null) {
               appRequest.setChallengeResponse(request.getChallengeResponse());
            }

            Response appResponse = client.handle(appRequest);
            
            response.setStatus(appResponse.getStatus());
            response.setEntity(appResponse.getEntity());
            if (headersToCopy.size()>0) {
               Series<Header> appResponseHeaders = (Series<Header>)appResponse.getAttributes().get("org.restlet.http.headers");
               Series<Header> responseHeaders = (Series<Header>)response.getAttributes().get("org.restlet.http.headers");
               if (responseHeaders==null) {
                  responseHeaders = new Series<Header>(Header.class);
                  response.getAttributes().put("org.restlet.http.headers",responseHeaders);
               }
               for (String name : headersToCopy) {
                  String [] values = responseHeaders.getValuesArray(name);
                  for (int i=0; i<values.length; i++) {
                     responseHeaders.add(name,values[i]);
                  }
               }
            }
            
         }
      };
   }
   
}
