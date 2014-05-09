/*
 * Main.java
 *
 * Created on August 15, 2007, 10:09 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.Permission;
import java.text.DateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 *
 * @author alex
 */
public class Main
{
   
static String fineLog =
"handlers= java.util.logging.ConsoleHandler\n"+
".level= FINE\n"+
"java.util.logging.ConsoleHandler.level = FINE\n"+
"java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";
;
static String finerLog =
"handlers= java.util.logging.ConsoleHandler\n"+
".level= FINER\n"+
"java.util.logging.ConsoleHandler.level = FINER\n"+
"java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";
;
static String finestLog =
"handlers= java.util.logging.ConsoleHandler\n"+
".level= FINEST\n"+
"java.util.logging.ConsoleHandler.level = FINEST\n"+
"java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";
;
   class DebugHandler extends Handler {
      public void publish(LogRecord record) {
         if (record.getLevel().intValue()<=Level.FINE.intValue()) {
            DateFormat format = DateFormat.getDateTimeInstance();
            String dateTime = format.format(new Date(record.getMillis()));
            System.err.println(dateTime+" "+record.getSourceClassName()+" "+record.getSourceMethodName());
            System.err.println(record.getLevel()+": "+record.getMessage());
         }
      }
      
      public void flush() {
         
      }
      
      public void close() {
         
      }
   }

   static Logger LOG = Logger.getLogger(Main.class.getName());
   
   File confFile;
   Configuration conf;
   WebComponent www;
   int argIndex;
   Level logLevel;
   boolean setLogLevel;
   String policy;
   
   /** Creates a new instance of Main */
   public Main(String [] args)
   {
      this.argIndex = 0;
      this.logLevel = Level.INFO;
      this.setLogLevel = false;
      this.policy = null;
      while (argIndex<args.length && args[argIndex].charAt(0)=='-') {
         String name = args[argIndex].substring(1);
         argIndex++;
         if (argIndex==args.length) {
            throw new RuntimeException("The argument "+args[argIndex]+" requires an argument.");
         }
         if (name.equals("l") || name.equals("-level")) {
            setLogLevel = true;
            if (args[argIndex].equals("info")) {
               logLevel = Level.INFO;
            } else if (args[argIndex].equals("fine")) {
               logLevel = Level.FINE;
            } else if (args[argIndex].equals("finer")) {
               logLevel = Level.FINER;
            } else if (args[argIndex].equals("finest")) {
               logLevel = Level.FINEST;
            } else if (args[argIndex].equals("config")) {
               logLevel = Level.CONFIG;
            }
         } else if (name.equals("-security-policy")) {
            policy = args[argIndex];
         }

         argIndex++;
      }
      int argCount = args.length-argIndex;
      if (argCount!=1) {
         throw new RuntimeException("The number of arguments is wrong.");
      }
      confFile = new File(args[argIndex]);
   }
   
   public void init() {
      
      //System.out.println("Setting log level to "+logLevel);
      if (setLogLevel) {
         Logger log = Logger.getLogger(WebComponent.LOG_NAME);
         log.setLevel(logLevel);
         log = Logger.getLogger("org.xproclet");
         log.setLevel(logLevel);
         log.addHandler(new DebugHandler());
      }
      
      if (policy!=null) {
         LOG.info("Using security policy "+policy);
         LOG.info("xproclet.home="+System.getProperty("xproclet.home"));
         System.setProperty("java.security.policy",policy);
         System.setSecurityManager(new SecurityManager());
      }

      if (setLogLevel) {
         if (logLevel==Level.FINE) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(fineLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }
         } else if (logLevel==Level.FINER) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(finerLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }
         } else if (logLevel==Level.FINEST) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(finestLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }

         }
      }
      
      try {
         conf = new Configuration();
         LOG.info("Loading configuration from "+confFile.toURI());
         conf.load(confFile.toURI());
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      if (setLogLevel) {
         if (logLevel==Level.FINE) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(fineLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }
         } else if (logLevel==Level.FINER) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(finerLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }
         } else if (logLevel==Level.FINEST) {
            try {
               LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(finestLog.getBytes()));
            } catch (java.io.IOException ex) {
               ex.printStackTrace();
            }

         }
      }
      
      
      www = new WebComponent(conf);
   }
   
   public void run() {
      try {
         www.start();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
   
   public void stop() {
      try {
         www.stop();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
   
   /**
    * @param args the command line arguments
    */
   public static void main(String[] args)
   {
      Main main = new Main(args);
      main.init();
      main.run();
   }
   
}
