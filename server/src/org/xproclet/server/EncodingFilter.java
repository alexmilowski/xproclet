/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Encoding;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.application.EncodeRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class EncodingFilter extends Filter {
   
   protected List<MediaType> mediaTypes;
   protected Encoding encoding;
   
   public EncodingFilter(Context context) {
      super(context);
   }
   
   public void start() 
      throws Exception
   {
      super.start();
      
      String typesSpec = getContext().getParameters().getFirstValue("types");
      mediaTypes = new ArrayList<MediaType>();
      if (typesSpec!=null) {
         String [] types = typesSpec.split(",");
         for (int i=0; i<types.length; i++) {
            try {
               mediaTypes.add(MediaType.valueOf(types[i].trim()));
            } catch (IllegalArgumentException ex) {
               getLogger().severe("Bad media type value "+types[i]+", "+ex.getMessage());
            }
         }
      }
      String encodingName = getContext().getParameters().getFirstValue("encoding");
      encoding = encodingName==null ? Encoding.GZIP : Encoding.valueOf(encodingName);
   }
   
   protected void afterHandle(Request request, Response response) {
      Representation rep = response.getEntity();
      if (response.getStatus().isSuccess() && rep!=null) {
         MediaType type = rep.getMediaType();
         for (MediaType compressable : mediaTypes) {
            if (compressable.includes(type)) {
               response.setEntity(new EncodeRepresentation(encoding,rep));
               return;
            }
         }
      }
   }
}
