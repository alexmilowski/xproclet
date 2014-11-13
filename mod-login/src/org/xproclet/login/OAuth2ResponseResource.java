/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CookieSetting;
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
public class OAuth2ResponseResource extends ServerResource {
   
   static ChallengeScheme HTTP_BEARER = new ChallengeScheme("HTTP_Bearer","Bearer");
   final static String GOOGLE_TOKEN_PROVIDER = "https://accounts.google.com/o/oauth2/token";
   final static String GOOGLE_USER_INFO = "https://www.googleapis.com/oauth2/v1/userinfo";
   OpenIDContext openIDState;
   IdentityManager idManager;
   String confCookiePath;
   String confCookieName;
   URI redirectURI;
   
   public OAuth2ResponseResource() {
   }
   
   protected void doInit() {
      
      openIDState = (OpenIDContext)getContext().getAttributes().get("oauth2.context");
      idManager = (IdentityManager)getContext().getAttributes().get(IdentityManager.ATTR);
      if (idManager==null) {
         getLogger().severe("No identity manager could be found, context: "+getContext());
      }
      confCookiePath = getContext().getParameters().getFirstValue("cookie.path");
      if (confCookiePath==null) {
         confCookiePath = "/";
      }
      confCookieName = getContext().getParameters().getFirstValue("cookie.name");
      try {
         redirectURI = new URI(getRequest().getResourceRef().toString()).resolve(getContext().getParameters().getFirstValue("oauth2.redirect_uri"));
      } catch (URISyntaxException ex) {
         getLogger().log(Level.SEVERE,"Cannot construct redirect_uri value",ex);
      }
      
   }
   
   protected Representation get() 
   {
      if (!getRequest().getResourceRef().hasQuery()) {
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("No OpenID response was specified.",MediaType.TEXT_PLAIN);
      } else {
         return fromProvider();
      }
   }
   
   protected Representation fromProvider()
   {
      Reference oauth2Ref = new Reference(GOOGLE_TOKEN_PROVIDER);
      
      Form responseData = getRequest().getResourceRef().getQueryAsForm();
      getLogger().info(getRequest().getResourceRef().getQuery());
      String state = responseData.getFirstValue("state");
      if (state==null || !openIDState.end(state)) {
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("The request is invalid.",MediaType.TEXT_PLAIN);
      }

      Form tokenExchange = new Form();
      tokenExchange.add("client_id", getContext().getParameters().getFirstValue("oauth2.client_id"));
      tokenExchange.add("client_secret", getContext().getParameters().getFirstValue("oauth2.client_secret"));
      tokenExchange.add("code",responseData.getFirstValue("code"));
      tokenExchange.add("redirect_uri",redirectURI.toString());
      tokenExchange.add("grant_type","authorization_code");
      
      getLogger().info("Checking response...");
      getLogger().info(oauth2Ref.toString());
      Restlet client = getContext().getClientDispatcher();
      
      Request checkRequest = new Request(Method.POST,oauth2Ref);
      checkRequest.setEntity(tokenExchange.getWebRepresentation());
      Response checkResponse = client.handle(checkRequest);
      
      if (checkResponse.getStatus().isSuccess()) {
         String json = checkResponse.getEntityAsText();
         getLogger().info(json);
         JsonReader jsonReader = Json.createReader(new StringReader(json));
         JsonObject obj = jsonReader.readObject();
         String accessToken = obj.getString("access_token");
         getLogger().info("access_token="+accessToken);
         
         Request infoRequest = new Request(Method.GET,new Reference(GOOGLE_USER_INFO));
         ChallengeResponse tokenResponse = new ChallengeResponse(HTTP_BEARER);
         tokenResponse.setRawValue(accessToken);
         infoRequest.setChallengeResponse(tokenResponse);

         Response infoResponse = client.handle(infoRequest);
         if (!checkResponse.getStatus().isSuccess()) {
            getLogger().severe("Cannot get user information, status="+checkResponse.getStatus());
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return new StringRepresentation("Cannot get user information.",MediaType.TEXT_PLAIN);
         }
         
         String infoJson = infoResponse.getEntityAsText();
         getLogger().info(infoJson);
         jsonReader = Json.createReader(new StringReader(infoJson));
         JsonObject infoObj = jsonReader.readObject();
         String firstName = infoObj.getString("given_name");
         String lastName = infoObj.getString("family_name");
         String email = infoObj.getString("email");
         Identity user = idManager.get(email);
         if (user==null) {
            Reference unauth = new Reference(getRequest().getResourceRef().getParentRef().getParentRef()+"unauthorized");
            unauth.addQueryParameter("first",firstName);
            unauth.addQueryParameter("last",lastName);
            unauth.addQueryParameter("email",email);
            getResponse().redirectSeeOther(unauth);
            return null;
         }
         Identity identity = user.createSessionIdentity(UUID.randomUUID().toString());
         getLogger().info("Authenticated "+firstName+" "+lastName+" <"+email+"> as "+identity.getAlias());
         String name = getCookieName(getRequest());
         if (name!=null) {
            CookieSetting cookie = new CookieSetting("I",identity.getSession());
            cookie.setPath(getCookiePath(getRequest()));
            getResponse().getCookieSettings().add(cookie);
            idManager.add(identity.getSession(), identity);
         }
         IdentityFilter.addIdentity(getRequest(), identity);
         getResponse().redirectSeeOther(getRequest().getResourceRef().getParentRef().getParentRef());
         return null;
         
      } else {
         getLogger().severe("Status: "+checkResponse.getStatus());
         getLogger().severe(checkResponse.getEntityAsText());
         getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
         return new StringRepresentation("Response was not valid from OpenID provider.",MediaType.TEXT_PLAIN);
      }
      
   }
   
   protected String getCookiePath(Request request) {
      Object o = request.getAttributes().get("cookie.path");
      return o==null ? confCookiePath : o.toString();
   }
   
   protected String getCookieName(Request request) {
      Object o = request.getAttributes().get("cookie.name");
      return o==null ? confCookieName : o.toString();
   }
}
