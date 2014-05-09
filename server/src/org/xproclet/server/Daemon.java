/*
 * Daemon.java
 *
 * Created on June 30, 2007, 12:31 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.server;

/**
 *
 * @author alex
 */
public class Daemon
{

   Main main;
   
   /** Creates a new instance of Daemon */
   public Daemon()
   {
   }
   
   public void init(String[] arguments) {
      // Here open the configuration files, create the trace file, create the ServerSockets, the Threads
      main = new Main(arguments);
      try {
         main.init();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
   
   public void start() {
      // Start the Thread, accept incomming connections
      main.run();
   }
   
   public void stop() {
      // Inform the Thread to live the run(), close the ServerSockets
      try {
         main.stop();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
   
   public void destroy() {
      // Destroy any object created in init()
      main = null;
   }

}
