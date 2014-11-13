/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class OAuth2RequestResource extends ServerResource {
   
   // TODO: need to do service resolution
   final static String GOOGLE_PROVIDER = "https://accounts.google.com/o/oauth2/auth";
   
   Map<String,String> domainMap;
   OpenIDContext openIDState;
   
   public OAuth2RequestResource() {
   }
   
   protected void doInit() {
      
      String [] maps = getContext().getParameters().getValuesArray("login.oauth2.domain.provider");
      domainMap = new TreeMap<String,String>();
      for (int i=0; maps!=null && i<maps.length; i++) {
         int eq = maps[i].indexOf('=');
         if (eq>0) {
            String domain = maps[i].substring(0,eq).trim();
            String provider = maps[i].substring(eq+1).trim();
            domainMap.put(domain,provider);
         }
                 
      }
      
      openIDState = (OpenIDContext)getContext().getAttributes().get("oauth2.context");
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
      
      Reference oauth2Ref = new Reference(provider);
      try {
         // Construct a redirect URI with the OAuth2 parameters for the service
         URI redirect = new URI(getRequest().getResourceRef().toString()).resolve(getContext().getParameters().getFirstValue("oauth2.redirect_uri"));
         getLogger().info(redirect.toString());
         oauth2Ref.addQueryParameter("client_id", getContext().getParameters().getFirstValue("oauth2.client_id"));
         oauth2Ref.addQueryParameter("redirect_uri", redirect.toString());
         oauth2Ref.addQueryParameter("response_type", "code");
         oauth2Ref.addQueryParameter("scope", "openid email");
         oauth2Ref.addQueryParameter("state", id);
         getResponse().redirectSeeOther(oauth2Ref);
      } catch (URISyntaxException ex) {
         getLogger().log(Level.SEVERE,"Cannot construct redirect URI",ex);
      }
      return null;
   }
}
