/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.restlet.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xproclet.server.DocumentLoader;

/**
 *
 * @author alex
 */
public class LocalAccessTokenManager implements AccessTokenManager {
 
   static final long RELOAD_INTERVAL = 5*60*1000; // 5 minutes
   static final DocumentLoader.Name tokenName = new DocumentLoader.Name("token");
   
   Context context;
   Map<String,String> tokens;
   IdentityManager identityManager;
   long tokensTimestamp;
   long lastModified;
   URI tokensRef;
   DocumentLoader docLoader;
   
   public LocalAccessTokenManager(Context context) {
      this.context = context;
      this.tokens = null;
      this.docLoader = new DocumentLoader();
      tokensTimestamp = lastModified = -1;
      String location = context.getParameters().getFirstValue("login.tokens");
      if (location!=null) {
         tokensRef = URI.create(location);
         context.getLogger().info("Tokens will be loaded from "+tokensRef);
      } else {
         tokensRef = null;
      }
   }
   
   public Identity getIdentity(String token) 
   {
      if (tokens==null || lastModified<getLastModified()) {
         tokens = null;
         loadTokens();
      }
      String userId = tokens.get(token);
      if (userId==null) {
         return null;
      }
      if (identityManager==null) {
         identityManager = (IdentityManager)context.getAttributes().get(IdentityManager.ATTR);
      }
      if (identityManager==null) {
         context.getLogger().warning("Identity manager could not be found to load user identity for access token.");
      }
      return identityManager==null ? null : identityManager.get(userId);
   }
   
   protected long getLastModified() {
      if (tokensRef==null) {
         return -1;
      }
      if (tokensRef.getScheme().equals("file")) {
         File usersFile = new File(tokensRef.getPath());
         return usersFile.lastModified();
      } else {
         long elapsed = System.currentTimeMillis() - tokensTimestamp;
         return elapsed>RELOAD_INTERVAL ? tokensTimestamp : tokensTimestamp+1;
      }
   }
   
   protected void loadTokens() {
      tokensTimestamp = System.currentTimeMillis();
      tokens = new TreeMap<String,String>();
      if (tokensRef==null) {
         context.getLogger().log(Level.SEVERE,"There is no login.tokens configured.");
         return;
      }
      context.getLogger().info("Loading tokens: "+tokensRef);
      lastModified = getLastModified();
      try {
         Document doc = docLoader.load(tokensRef);
         for (Element tokenElement : DocumentLoader.getElementsByName(doc.getDocumentElement(),tokenName)) {
            String token = DocumentLoader.getAttributeValue(tokenElement,"value");
            String user = DocumentLoader.getAttributeValue(tokenElement,"user");
            if (token!=null && user!=null) {
               tokens.put(token.trim(), user.trim());
            }
         }
      } catch (Exception ex) {
         context.getLogger().log(Level.SEVERE,"Cannot load users document: "+tokensRef,ex);
      }
   }
   
}
