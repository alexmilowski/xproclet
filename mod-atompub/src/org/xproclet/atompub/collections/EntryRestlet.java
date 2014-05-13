/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.io.IOException;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Reference;

/**
 *
 * @author alex
 */
public class EntryRestlet extends CollectionBaseRestlet {
   
   public EntryRestlet(Context context)
   {
      super(context);
   }
   public void handle(Request request, Response response) {
      super.handle(request, response);
      if (request.getMethod().equals(Method.DELETE)) {
         Object o = response.getAttributes().get("org.xproclet.atompub.media.src");
         if (o!=null && mediaStorage!=null) {
            String name = request.getAttributes().get("name").toString();
            String file = o.toString();
            file = Reference.decode(file);
            try {
               mediaStorage.delete(request.getAttributes(), name, file);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"Cannot delete file "+file+" from collection "+name,ex);
            }
         }
      }
   }
}
