/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conf;

import java.io.StringWriter;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class DumpResource extends ServerResource {
   
   protected Representation doHandle() {
      StringWriter out = new StringWriter();
      out.write("Context:\n\nAttributes:\n");
      for (String key : getContext().getAttributes().keySet()) {
         out.write(key+"="+getContext().getAttributes().get(key)+"\n");
      }
      out.write("\nParameters:\n");
      for (Parameter param : getContext().getParameters()) {
         out.write(param.getName()+"="+param.getValue()+"\n");
      }
      out.write("\nRequest:\n\nMethod: "+getRequest()+"\nAttributes:\n");
      for (String key : getRequest().getAttributes().keySet()) {
         out.write(key+"="+getRequest().getAttributes().get(key)+"\n");
      }
      return new StringRepresentation(out.toString(),MediaType.TEXT_PLAIN);
   }
   
}
