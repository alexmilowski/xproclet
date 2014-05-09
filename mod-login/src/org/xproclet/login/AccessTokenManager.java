/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

/**
 *
 * @author alex
 */
public interface AccessTokenManager {
   final String ATTR = "org.xproclet.www.access_token.manager";
   
   Identity getIdentity(String token);
   
}
