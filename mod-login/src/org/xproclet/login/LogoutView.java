/*
 * LoginForm.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import org.restlet.data.Reference;
import org.restlet.representation.Representation;

/**
 *
 * @author alex
 */
public class LogoutView extends LogoutAction
{
   
           
   /** Creates a new instance of LoginForm */
   public LogoutView()
   {
   }
   
   public Representation get()
   {
      super.get();
      Reference referrer = getRequest().getReferrerRef();
      getResponse().redirectSeeOther(referrer==null ? getRequest().getRootRef() : referrer);
      return null;
   }   
   
}
