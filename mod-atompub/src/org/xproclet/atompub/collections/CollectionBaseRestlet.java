/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import org.xproclet.xproc.XProcRestlet;
import org.restlet.Context;

/**
 *
 * @author alex
 */
public class CollectionBaseRestlet extends XProcRestlet {

   public static final String METADATA_SERVICE = "app.service.metadata";
   public static final String MEDIA_STORAGE = "app.media.storage";
   public static final String MEDIA_HREF_ATTR = "app.media.href";
   
   //protected MetadataService metadataService;
   protected MediaStorage mediaStorage;
   protected CollectionBaseRestlet(Context context) {
      super(context);
      mediaStorage = (MediaStorage)context.getAttributes().get(MEDIA_STORAGE);
   }
   
}
