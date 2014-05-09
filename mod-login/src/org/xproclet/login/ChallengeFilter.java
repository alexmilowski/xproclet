/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.util.ArrayList;
import java.util.List;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.CookieSetting;
import org.restlet.data.Form;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class ChallengeFilter extends Filter {

   ChallengeScheme challengeScheme;
   String challengeRealm;
   String loginType;
   String loginApp;
   Reference confService;
   IdentityManager idManager;
   String confCookiePath;
   String confCookieName;
   boolean usersDefined;
   
   public ChallengeFilter(Context context)
   {
      super(context);
      
      challengeScheme = ChallengeScheme.HTTP_BASIC;
      String scheme = getContext().getParameters().getFirstValue("challenge.scheme");
      if (scheme!=null) {
         challengeScheme = ChallengeScheme.valueOf(scheme);
      }
      challengeRealm = getContext().getParameters().getFirstValue("challenge.realm");
      if (challengeRealm==null) {
         challengeRealm = "Users";
      }
      confService = null;
      idManager = (IdentityManager)getContext().getAttributes().get(IdentityManager.ATTR);
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
      usersDefined = getContext().getParameters().getFirstValue("login.users")!=null;

      confCookiePath = getContext().getParameters().getFirstValue("cookie.path");
      if (confCookiePath==null) {
         confCookiePath = "/";
      }
      confCookieName = getContext().getParameters().getFirstValue("cookie.name");
   }
   
   protected int beforeHandle(final Request request, final Response response) {
      Identity identity = (Identity)request.getAttributes().get(Identity.IDENTITY_ATTR);
      if (identity==null) {
         ChallengeResponse authResponse = request.getChallengeResponse();
         if (authResponse!=null && idManager!=null && !loginType.equals("openid")) {
            Reference service = ActionResource.getReferenceAttribute(request,"auth-service",confService);
            if (service==null) {
               getLogger().warning("No authentication service has been configured.");
               return Filter.CONTINUE;
            }
            final String username = authResponse.getIdentifier();
            final Identity user = idManager.get(username);
            if (!usersDefined || user!=null) {
               String password = new String(authResponse.getSecret());
               LoginAction.LoginActor actor = new LoginAction.LoginActor() {
                  public void authenticated(Form authForm,Identity loginIdentity) {
                     Identity identity = usersDefined ? user.createSessionIdentity(loginIdentity.getSession()) : loginIdentity;
                     getLogger().info("Authenticated: "+loginIdentity.getAlias()+", "+loginIdentity.getEmail());
                     String name = getCookieName(request);
                     if (name!=null) {
                        CookieSetting cookie = new CookieSetting("I",identity.getSession());
                        cookie.setPath(getCookiePath(request));
                        response.getCookieSettings().add(cookie);
                        idManager.add(identity.getSession(), identity);
                     }
                     IdentityFilter.addIdentity(request, identity);
                  }
                  public void unauthorized() {
                  }
               };

               LoginAction.login(getContext().createChildContext(), service, loginApp, loginType, username, password, null, null, actor);
            } else {
               getLogger().warning("User "+username+" is not allowed.");
            }
         }
      }
      return Filter.CONTINUE;
   }
   
   protected void afterHandle(Request request, Response response) {
      if (response.getStatus()==Status.CLIENT_ERROR_UNAUTHORIZED && !loginType.equals("openid")) {
         List<ChallengeRequest> requests = new ArrayList<ChallengeRequest>();
         requests.add(new ChallengeRequest(challengeScheme,challengeRealm));
         response.setChallengeRequests(requests);
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
