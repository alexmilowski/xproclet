/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.server;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class ClassResourceFinder extends Finder {
   
   ClassLoader classLoader;
   String packageName;
   String indexName;

   /** Creates a new instance of ClassResourceFinder */
   public ClassResourceFinder(Context context,ClassLoader classLoader,String packageName,String indexName)
   {
      super(context);
      this.classLoader = classLoader;
      this.indexName = indexName;
      this.packageName = packageName.replace('.','/')+"/";
   }

   public ServerResource find(Request request,Response response)
   {
      String path = packageName+request.getResourceRef().getRemainingPart();
      if (path.endsWith("/")) {
         path += indexName;
      }
      getLogger().info("resource: "+path);
      ServerResource r = new ClassResource(classLoader,path);
      r.setRequest(request);
      r.setResponse(response);
      return r;
   }
}
