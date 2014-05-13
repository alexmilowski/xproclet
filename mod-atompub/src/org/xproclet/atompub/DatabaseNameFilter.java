/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub;

import java.util.ArrayList;
import java.util.List;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class DatabaseNameFilter extends Filter {
   
   final String DB_NAME_ATTRIBUTE = "app.database";
   final String APP_PATH_ATTRIBUTE = "app.path";
   final String NAMES_PARAMETER = "names";
   List<String> names;
   public DatabaseNameFilter(Context context) {
      super(context);
      String [] values = context.getParameters().getValuesArray(NAMES_PARAMETER);
      names = new ArrayList<String>();
      for (int v=0; values!=null && v<values.length; v++) {
         String [] namesToAdd = values[v].split(",");
         for (int i=0; i<namesToAdd.length; i++) {
            names.add(namesToAdd[i]);
         }
      }
   }
   
   String getValue(String name,String dbName, Request request)
   {
      String key = dbName+"."+name;
      Object o = request.getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      o = getContext().getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      return getContext().getParameters().getFirstValue(key);
   }
   
   protected int beforeHandle(Request request, Response response) {
      Object o = request.getAttributes().get(DB_NAME_ATTRIBUTE);
      String dbName = o==null ? null : o.toString();
      if (dbName!=null) {
         for (String name : names) {
            String value = getValue(name,dbName,request);
            if (value!=null) {
               request.getAttributes().put(name, value);
            }
         }
         request.getAttributes().put(APP_PATH_ATTRIBUTE,request.getResourceRef().getBaseRef().getPath());
      }
      return Filter.CONTINUE;
   }
   
}
