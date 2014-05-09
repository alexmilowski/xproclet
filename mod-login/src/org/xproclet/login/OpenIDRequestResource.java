/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.util.Map;
import java.util.TreeMap;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
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
public class OpenIDRequestResource extends ServerResource {
   
   // TODO: need to do service resolution
   final static String GOOGLE_PROVIDER = "https://www.google.com/accounts/o8/ud";
   
   Map<String,String> domainMap;
   OpenIDContext openIDState;
   
   public OpenIDRequestResource() {
   }
   
   protected void doInit() {
      
      String [] maps = getContext().getParameters().getValuesArray("login.openid.domain.provider");
      domainMap = new TreeMap<String,String>();
      for (int i=0; maps!=null && i<maps.length; i++) {
         int eq = maps[i].indexOf('=');
         if (eq>0) {
            String domain = maps[i].substring(0,eq).trim();
            String provider = maps[i].substring(eq+1).trim();
            domainMap.put(domain,provider);
         }
                 
      }
      
      openIDState = (OpenIDContext)getContext().getAttributes().get("openid.context");
   }
   
   protected Representation get() 
   {
      if (!getRequest().getResourceRef().hasQuery()) {
         return sendToProvider(GOOGLE_PROVIDER);
      } else {
         String domain = getRequest().getResourceRef().getQueryAsForm().getFirstValue("domain");
         if (domain==null) {
            return sendToProvider(GOOGLE_PROVIDER);
         }
         String provider = domainMap.get(domain);
         if (provider==null) {
            getLogger().info("Unrecognized OpenId domain "+domain);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("Invalid request.",MediaType.TEXT_PLAIN);
         }
         return sendToProvider(provider);
      }
   }
   
   protected Representation sendToProvider(String provider)
   {
      String id = openIDState.start();
      
      Reference openidRef = new Reference(provider);
      openidRef.addQueryParameter("openid.ns", "http://specs.openid.net/auth/2.0");
      openidRef.addQueryParameter("openid.return_to", getRequest().getResourceRef().toString()+"/"+id);
      openidRef.addQueryParameter("openid.realm", getRequest().getHostRef().toString());
      openidRef.addQueryParameter("openid.claimed_id", "http://specs.openid.net/auth/2.0/identifier_select");
      openidRef.addQueryParameter("openid.identity", "http://specs.openid.net/auth/2.0/identifier_select");
      openidRef.addQueryParameter("openid.mode", "checkid_setup");
      openidRef.addQueryParameter("openid.ns.ui", "http://specs.openid.net/extensions/ui/1.0");
      openidRef.addQueryParameter("openid.ui.icon", "true");
      openidRef.addQueryParameter("openid.ui.mode", "x-has-session");
      openidRef.addQueryParameter("openid.ns.ax", "http://openid.net/srv/ax/1.0");
      openidRef.addQueryParameter("openid.ax.mode", "fetch_request");
      openidRef.addQueryParameter("openid.ax.required", "email,firstname,lastname");
      openidRef.addQueryParameter("openid.ax.type.email", "http://axschema.org/contact/email");
      openidRef.addQueryParameter("openid.ax.type.firstname", "http://axschema.org/namePerson/first");
      openidRef.addQueryParameter("openid.ax.type.lastname", "http://axschema.org/namePerson/last");
      getResponse().redirectSeeOther(openidRef);
      //response.setEntity(new StringRepresentation(openidRef.toString(),MediaType.TEXT_PLAIN));
      return null;
   }
}
