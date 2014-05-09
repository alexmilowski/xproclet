/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.login;


/**
 *
 * @author alex
 */
public interface IdentityManager {

   final String ATTR = "org.xproclet.www.identity.manager";

   void add(String value,Identity identity);
   
   boolean remove(String value);
   
   Identity get(String userId);
}
