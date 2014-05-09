/*
 * LoginForm.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class CheckAction extends ServerResource
{
   
   /** Creates a new instance of LoginForm */
   public CheckAction()
   {
      setNegotiated(false);
   }
   
   
   public Representation get()
   {
      Identity identity = (Identity)getRequest().getAttributes().get(Identity.IDENTITY_ATTR);
      getResponse().setStatus(identity==null ? Status.CLIENT_ERROR_UNAUTHORIZED : Status.SUCCESS_NO_CONTENT);
      return null;
   }
   
}
