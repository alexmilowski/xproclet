/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xproclet.server.DocumentLoader;

/**
 *
 * @author alex
 */
public class SimpleLocalAuthentication extends Restlet {
   
   static final long RELOAD_INTERVAL = 5*60*1000; // 5 minutes
   
   long passwordsTimestamp;
   long lastModified;
   URI passwordsRef;
   Map<String,String> passwords;
   DocumentLoader docLoader;
   
   public void start() {
      docLoader = new DocumentLoader();
      String location = getContext().getParameters().getFirstValue("login.passwords");
      if (location!=null) {
         passwordsRef = URI.create(location);
         getLogger().fine("Passwords will be loaded from "+passwordsRef);
      } else {
         passwordsRef = null;
      }
   }
   
   public void handle(Request request, Response response) {
      if (!request.getMethod().equals(Method.GET)) {
         response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
         return;
      }
      ChallengeResponse authResponse = request.getChallengeResponse();
      if (passwords==null || passwordsTimestamp<getPasswordsLastModified()) {
         loadPasswords();
      }
      String password = passwords.get(authResponse.getIdentifier());
      if ((new String(authResponse.getSecret())).equals(password)) {
         response.setStatus(Status.SUCCESS_NO_CONTENT);
      } else {
         response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
      }
   }
   
   protected long getPasswordsLastModified() {
      if (passwordsRef.getScheme().equals("file")) {
         File usersFile = new File(passwordsRef.getPath());
         return usersFile.lastModified();
      } else {
         long elapsed = System.currentTimeMillis() - passwordsTimestamp;
         return elapsed>RELOAD_INTERVAL ? passwordsTimestamp : System.currentTimeMillis();
      }
   }
   
   protected void loadPasswords() {
      getLogger().fine("Loading passwords: "+passwordsRef);
      passwords = new TreeMap<String,String>();
      passwordsTimestamp = System.currentTimeMillis();
      lastModified = getPasswordsLastModified();
      try {
         Document doc = docLoader.load(passwordsRef);
         for (Element user : DocumentLoader.getElementsByName(doc.getDocumentElement(),IdentityFilter.userName)) {
            String alias = DocumentLoader.getAttributeValue(user,"alias");
            String email = DocumentLoader.getAttributeValue(user,"email");
            String password = DocumentLoader.getAttributeValue(user,"password");
            if (password==null) {
               getLogger().fine("Ignoring user "+alias+" ("+email+") without password.");
               continue;
            }
            if (alias!=null) {
               passwords.put(email,password);
            }
            if (email!=null) {
               passwords.put(email,password);
            }
            getLogger().fine("Loaded password for "+alias+" ("+email+")");
         }
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot load users document: "+passwordsRef,ex);
      }
   }
   
}
