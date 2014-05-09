/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conf;

import java.io.StringWriter;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;

/**
 *
 * @author alex
 */
public class DumpRestlet extends Restlet {
   
   public void handle(Request request, Response response) {
      StringWriter out = new StringWriter();
      out.write("Context:\n\nAttributes:\n");
      for (String key : getContext().getAttributes().keySet()) {
         out.write(key+"="+getContext().getAttributes().get(key)+"\n");
      }
      out.write("\nParameters:\n");
      for (Parameter param : getContext().getParameters()) {
         out.write(param.getName()+"="+param.getValue()+"\n");
      }
      out.write("\nRequest:\n\nMethod: "+request.getMethod()+"\nAttributes:\n");
      for (String key : request.getAttributes().keySet()) {
         out.write(key+"="+request.getAttributes().get(key)+"\n");
      }
      response.setEntity(new StringRepresentation(out.toString(),MediaType.TEXT_PLAIN));
      response.setStatus(Status.SUCCESS_OK);
   }
   
}
