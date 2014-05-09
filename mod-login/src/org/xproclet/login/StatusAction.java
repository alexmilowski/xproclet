/*
 * LoginForm.java
 *
 * Created on September 7, 2007, 10:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.login;

import java.net.URI;
import java.util.Set;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class StatusAction extends ServerResource
{
   
   /** Creates a new instance of LoginForm */
   public StatusAction()
   {
      setNegotiated(false);
   }
   
   
   public Representation get()
   {
      Identity identity = (Identity)getRequest().getAttributes().get(Identity.IDENTITY_ATTR);
      Representation rep = null;
      if (identity==null) {
         rep = new StringRepresentation("<none/>",MediaType.APPLICATION_XML);
      } else {
         String xml = "<identity id='"+identity.getId()+"' alias='"+identity.getAlias()+"'>";
         if (identity.getName()!=null) {
            xml += "<name>"+identity.getName()+"</name>";
         }
         if (identity.getEmail()!=null) {
            xml += "<email>"+identity.getEmail()+"</email>";
         }
         Set<URI> roles = identity.getRoles();
         if (roles!=null) {
            for (URI role : roles) {
               xml += "<role href='"+role+"'/>";
            }
         }
         xml += "</identity>";
         rep = new StringRepresentation(xml,MediaType.APPLICATION_XML);
      }
      rep.setCharacterSet(CharacterSet.UTF_8);
      return rep;
   }
   
}
