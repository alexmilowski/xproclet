/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.login;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 *
 * @author alex
 */
public class OpenIDContext {
   
   // TODO: make this configurable
   long timeout = 5*60*1000; /* 5 minutes */
   
   Map<String,Date> inProgress;
   public OpenIDContext() {
      inProgress = new TreeMap<String,Date>();
   }
   
   public String start() {
      String id = UUID.randomUUID().toString();
      inProgress.put(id, new Date());
      return id;
   }
   
   public boolean end(String id) {
      Date time = inProgress.get(id);
      if (time==null) {
         return false;
      }
      Date now = new Date();
      if ((now.getTime()-time.getTime())>timeout) {
         inProgress.remove(id);
         return false;
      }
      return true;
   }
   
}
