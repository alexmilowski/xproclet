/*
 * Identity.java
 *
 * Created on September 7, 2007, 2:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author alex
 */
public class Identity
{
   
   public static final String IDENTITY_ATTR = "org.xproclet.www.identity";
   public static final String PARAMETER_IDENTITY_ID = "user.id";
   public static final String PARAMETER_IDENTITY_ALIAS = "user.alias";
   public static final String PARAMETER_IDENTITY_NAME = "user.name";
   public static final String PARAMETER_IDENTITY_EMAIL = "user.email";
   public static final String PARAMETER_IDENTITY_SESSION = "user.session";
   String session;
   String id;
   String alias;
   String name;
   String email;
   Set<URI> roles;
   
   /** Creates a new instance of Identity */
   public Identity(String id,String alias,String name,String email)
   {
      this(null,id,alias,name,email);
   }
   /** Creates a new instance of Identity */
   public Identity(String session,String id,String alias,String name,String email)
   {
      this.session = session;
      this.id = id;
      this.alias = alias;
      this.name = name;
      this.email = email;
      this.roles = new TreeSet<URI>();
   }
   
   public Identity createSessionIdentity(String session) {
      Identity sessionIdentity = new Identity(session,id,alias,name,email);
      sessionIdentity.roles = roles;
      return sessionIdentity;
   }
   
   public String getSession() {
      return session;
   }
   
   public String getId() {
      return id;
   }
   
   public String getAlias() {
      return alias;
   }
   
   public String getName() {
      return name;
   }
   
   public String getEmail() {
      return email;
   }
   
   public Set<URI> getRoles() {
      return roles;
   }
   
   public boolean hasRole(URI role) {
      return roles!=null && roles.contains(role);
   }
   
}
