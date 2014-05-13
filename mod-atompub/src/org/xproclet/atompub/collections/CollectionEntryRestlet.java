/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;

/**
 *
 * @author alex
 */
public class CollectionEntryRestlet extends CollectionBaseRestlet {
   static Logger LOG = Logger.getLogger(CollectionEntryRestlet.class.getName());
   public CollectionEntryRestlet(Context context)
   {
      super(context);
   }
   
   public void handle(Request request, Response response) {
      super.handle(request, response);
      if (request.getMethod().equals(Method.DELETE) && response.getStatus().isSuccess()) {
         // Check to make sure the collection directory exists.
         String collectionName = getOptionValue(request,CollectionRestlet.NAME_OPTION);

         if (mediaStorage!=null) {
            try {
               mediaStorage.deleteCollection(request.getAttributes(), collectionName);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error while deleting collection.",ex);
            }
         }
      }
   }
}
