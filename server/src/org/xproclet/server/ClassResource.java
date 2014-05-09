/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.server;

import java.io.InputStream;
import java.util.logging.Level;
import org.restlet.Application;
import org.restlet.data.MediaType;
import org.restlet.data.Metadata;
import org.restlet.data.Status;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class ClassResource extends ServerResource {
   
   ClassLoader classLoader;
   String path;
   MediaType type;

   /** Creates a new instance of ClassResource */
   public ClassResource(ClassLoader classLoader,String path)
   {
      setNegotiated(false);
      this.classLoader = classLoader;
      this.path = path;
      int extPos = path.lastIndexOf('.');
      Application app = this.getApplication();
      type = app.getMetadataService().getDefaultMediaType(); 
      if (extPos>=0) {
         String ext = path.substring(extPos+1);
         Metadata mdata = this.getApplication().getMetadataService().getMetadata(ext);
         if (mdata!=null) {
            type = MediaType.valueOf(mdata.getName());
         }
      }
   }

   public Representation get()
   {
      if (getLogger().isLoggable(Level.FINE)) {
         getLogger().info("Class resource: "+path);
      }
      InputStream is = classLoader.getResourceAsStream(path);
      if (is==null) {
         getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
         return null;
      } else {
         return new InputRepresentation(is,type);
      }
   }

}
