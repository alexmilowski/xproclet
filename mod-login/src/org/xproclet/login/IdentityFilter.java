/*
 * IdentityFilter.java
 *
 * Created on September 7, 2007, 3:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Cookie;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xproclet.server.DocumentLoader;

/**
 *
 * @author alex
 */
public class IdentityFilter extends Filter
{
   static final int MAX_CACHE = 1000;
   static final long MAX_AGE = 5*60*1000;
   static final long RELOAD_INTERVAL = 5*60*1000; // 5 minutes
   
   
   TreeMap<String,Long> createdTimes;
   TreeMap<String,Identity> identities;
   
   static Reference getReferenceAttribute(Request request,String name,Reference defaultValue)
   {
      Object o = request.getAttributes().get(name);
      return o==null ? defaultValue : (Reference)o;
   }
   
   static final DocumentLoader.Name roleName = new DocumentLoader.Name("role");
   static final DocumentLoader.Name userName = new DocumentLoader.Name("user");
   
   public static Identity createIdentity(Element user)
   {
      String session = DocumentLoader.getAttributeValue(user,"session");
      String id = DocumentLoader.getAttributeValue(user,"id");
      String alias = DocumentLoader.getAttributeValue(user,"alias");
      String name = DocumentLoader.getAttributeValue(user,"name");
      String email = DocumentLoader.getAttributeValue(user,"email");
      Identity identity = new Identity(session,id,alias,name,email);
      for (Element role : DocumentLoader.getElementsByName(user, roleName)) {
         String href = DocumentLoader.getAttributeValue(role,"href");
         identity.getRoles().add(DocumentLoader.resolve(role.getBaseURI(),href));
      }
      return identity;
   }
   
   public static Identity createIdentity(Representation entity)
      throws Exception
   {
      DocumentLoader docLoader = new DocumentLoader();
      Document doc = docLoader.load(entity.getReader());
      return createIdentity(doc.getDocumentElement());
   }

   static Identity checkSession(Context context,Reference service,String session)
      throws Exception
   {
      Restlet client = context.getClientDispatcher();
      //Client client = new Client(context,service.getSchemeProtocol());
      //client.getContext().getAttributes().put("hostnameVerifier", org.apache.commons.ssl.HostnameVerifier.DEFAULT);
      Reference ref = new Reference(service+"/"+session);
      Request authRequest = new Request(Method.GET,ref);
      Response response = client.handle(authRequest);
      try {
         if (response.getStatus().isSuccess()) {
            try {
               return createIdentity(response.getEntity());
            } catch (Exception ex) {
               throw new Exception("Exception while processing result from session lookup.",ex);
            }
         } else if (response.getStatus().getCode()==Status.CLIENT_ERROR_UNAUTHORIZED.getCode()) {
            return null;
         } else {
            throw new Exception("Request to realm failed, status="+response.getStatus());
         }
      } finally {
         if (response.isEntityAvailable()) {
            response.getEntity().release();
         }
      }
   }
   boolean lookup;
   long maxAge;
   int maxCache;
   String cookieName;
   Reference confService;
   URI challengeUsersRef;
   long usersTimestamp;
   long lastModified;
   Map<String,Identity> users;
   String loginType;
   DocumentLoader docLoader;
   boolean useEmail;
   AccessTokenManager accessTokenManager;
   
   /** Creates a new instance of IdentityFilter */
   public IdentityFilter(Context context)
   {
      this(context,null,null);
   }
   public IdentityFilter(Context context,Restlet next,Reference service)
   {
      super(context,next);
      docLoader = new DocumentLoader();
      this.confService = service;
      lookup =  "true".equals(context.getParameters().getFirstValue("identity.lookup"));
      createdTimes = new TreeMap<String,Long>();
      identities = new TreeMap<String,Identity>();
      String maxAgeS = context.getParameters().getFirstValue("identity.max.age");
      maxAge = maxAgeS==null ? MAX_AGE : Long.parseLong(maxAgeS)*1000;
      String maxCacheS = context.getParameters().getFirstValue("identity.max.cache");
      maxCache = maxCacheS==null ? MAX_CACHE : Integer.parseInt(maxCacheS);
      cookieName = context.getParameters().getFirstValue("identity.cookie");
      if (cookieName==null) {
         cookieName = "I";
      }
      getLogger().info("IdentityFilter: identity.max.age="+(maxAge/1000)+"s, identity.max.cache="+maxCache+", identity.lookup="+lookup);

      IdentityManager manager = (IdentityManager)context.getAttributes().get(IdentityManager.ATTR);
      if (manager==null) {
         manager = new IdentityManager() {
            public void add(String id,Identity identity) {
               IdentityFilter.this.addIdentity(id, identity);
            }
            
            public boolean remove(String id) {
               return IdentityFilter.this.removeIdentity(id);
            }
            
            public Identity get(String id) {
               return getIdentity(id);
            }
         };
         context.getAttributes().put(IdentityManager.ATTR, manager);
      }
      getLogger().fine("Identity Manager: "+manager);
      
      useEmail = "email".equals(getContext().getParameters().getFirstValue("login.key"));
      String location = getContext().getParameters().getFirstValue("login.users");
      if (location!=null) {
         challengeUsersRef = URI.create(location);
         getLogger().info("Users will be loaded from "+challengeUsersRef);
      } else {
         challengeUsersRef = null;
      }
      users = null;
      usersTimestamp = lastModified = -1;
      loginType = getContext().getParameters().getFirstValue("login.type");
      
      accessTokenManager = (AccessTokenManager)context.getAttributes().get(AccessTokenManager.ATTR);
      if (accessTokenManager!=null) {
         getLogger().info("IdentityFilter: Access tokens enabled.");
      }
   }
   
   public boolean removeIdentity(String id) {
      createdTimes.remove(id);
      return identities.remove(id)!=null;
   }
   
   public void addIdentity(String id,Identity identity)
   {
      identities.put(id,identity);
      createdTimes.put(id,new Long(System.currentTimeMillis()));
   }
   
   protected int beforeHandle(Request request,Response response)
   {
      Reference service = getReferenceAttribute(request,"auth-service",confService);
      Cookie cookie = request.getCookies().getFirst(cookieName);
      if (cookie!=null) {
         getLogger().info("cookie: "+cookieName+"="+cookie.getValue());
         Identity identity = identities.get(cookie.getValue());
         if (identity!=null) {
            getLogger().info("Found "+identity.getAlias()+", checking age...");
            // check age
            Long created = createdTimes.get(cookie.getValue());
            if (created==null) {
               getLogger().info("No creation time, expiring.");
               identities.remove(cookie.getValue());
               identity = null;
            } else if ((System.currentTimeMillis()-created.longValue())>=maxAge) {
               getLogger().info("Login timeout, expiring.");
               removeIdentity(cookie.getValue());
               identity = null;
            }
         }
         if (identity==null) {
            // TODO: make this configurable
            if (!lookup) {
               return Filter.CONTINUE;
            }
            getLogger().info("Looking up identity "+cookie.getValue());
            try {
               identity = checkSession(getContext().createChildContext(),service, cookie.getValue());
            } catch (Exception ex) {
               getLogger().log(Level.SEVERE,"Cannot authenticate against service.",ex);
            }
            if (identity!=null) {
               addIdentity(cookie.getValue(),identity);
            }
         }
         if (identity!=null) {
            addIdentity(request,identity);
         }
      } else {
         getLogger().fine("No I cookie.");
      }
      ChallengeResponse auth = request.getChallengeResponse();
      if (auth!=null) {
         getLogger().fine("Authorization: "+auth.getRawValue());
         boolean isFine = getLogger().isLoggable(Level.FINE); 
         if (accessTokenManager!=null && auth.getScheme().getTechnicalName().equals("Bearer")) {
            if (isFine) {
               getLogger().fine("Checking access token: "+request.getChallengeResponse().getRawValue());
            }
            Identity identity = accessTokenManager.getIdentity(request.getChallengeResponse().getRawValue());
            if (identity!=null) {
               if (isFine) {
                  getLogger().fine("Found user "+identity.getEmail()+" via access token");
               }
               addIdentity(request,identity);
            }
         }
      }
      return Filter.CONTINUE;
   }
   
   public static void addIdentity(Request request,Identity identity) {
      Map<String,Object> atts = request.getAttributes();
      atts.put(Identity.IDENTITY_ATTR,identity);
      if (identity.getId()!=null) {
         atts.put(Identity.PARAMETER_IDENTITY_ID, identity.getId());
      }
      if (identity.getAlias()!=null) {
         atts.put(Identity.PARAMETER_IDENTITY_ALIAS, identity.getAlias());
      }
      if (identity.getName()!=null) {
         atts.put(Identity.PARAMETER_IDENTITY_NAME, identity.getName());
      }
      if (identity.getEmail()!=null) {
         atts.put(Identity.PARAMETER_IDENTITY_EMAIL, identity.getEmail());
      }
      if (identity.getSession()!=null) {
         atts.put(Identity.PARAMETER_IDENTITY_SESSION, identity.getSession());
      }
   }
   
   protected boolean isUserAllowed(String username)
   {
      if (challengeUsersRef==null) {
         return !LoginAction.GOOGLE_CLIENT_LOGIN.equals(loginType);
      }
      if (users==null || usersTimestamp<getUsersLastModified()) {
         users = null;
         loadUsers();
      }
      if (users==null) {
         return false;
      }
      return users.get(username)!=null;
   }
   
   protected long getUsersLastModified() {
      if (challengeUsersRef.getScheme().equals("file")) {
         File usersFile = new File(challengeUsersRef.getPath());
         return usersFile.lastModified();
      } else {
         long elapsed = System.currentTimeMillis() - usersTimestamp;
         return elapsed>RELOAD_INTERVAL ? usersTimestamp : usersTimestamp+1;
      }
   }
   
   protected void loadUsers() {
      getLogger().info("Loading users: "+challengeUsersRef);
      users = new TreeMap<String,Identity>();
      usersTimestamp = System.currentTimeMillis();
      lastModified = getUsersLastModified();
      try {
         Document doc = docLoader.load(challengeUsersRef);
         for (Element user : DocumentLoader.getElementsByName(doc.getDocumentElement(),userName)) {
            Identity identity = IdentityFilter.createIdentity(user);
            String key = useEmail ? identity.getEmail() : identity.getAlias();
            getLogger().info("Loaded user: "+key);
            users.put(key,identity);
         }
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot load users document: "+challengeUsersRef,ex);
      }
   }
   
   protected Identity getIdentity(String id) {
      if (challengeUsersRef==null) {
         return null;
      }
      if (users==null || usersTimestamp<getUsersLastModified()) {
         users = null;
         loadUsers();
      }
      return users.get(id);
   }
   
}
