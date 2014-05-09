/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conf;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class TestResource extends ServerResource {
   
   protected Representation get() {
      return new StringRepresentation("Hello world!",MediaType.TEXT_PLAIN);
   }
   
}
