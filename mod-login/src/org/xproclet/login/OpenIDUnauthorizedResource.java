/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class OpenIDUnauthorizedResource extends ServerResource {

   public OpenIDUnauthorizedResource() {
      
   }
   
   protected Representation get() {
      Form data = getRequest().getResourceRef().getQueryAsForm();
      return new StringRepresentation(data.getFirstValue("first")+" "+data.getFirstValue("last")+" <"+data.getFirstValue("email")+"> is not authorized to access this resource.",MediaType.TEXT_PLAIN);
   }
}
