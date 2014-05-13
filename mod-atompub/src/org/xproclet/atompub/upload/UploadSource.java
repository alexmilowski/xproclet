/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.atompub.upload;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *
 * @author alex
 */
public class UploadSource {
   Logger log;
   boolean isFineLog;
   InputStream data;
   byte [] boundary;
   byte [] buffer = new byte[16384];
   byte [] secondary = new byte[16384];
   int start;
   int length;
   int secondaryLength;
   int bytesRead;
   boolean done;
   boolean cancelled;
   boolean boundaryFound;
   public UploadSource(Logger log,InputStream is) {
      this.log = log;
      this.isFineLog = log.isLoggable(Level.FINE);
      this.data = is;
      this.start = 0;
      this.length = 0;
      this.bytesRead = 0;
      this.secondaryLength = -1;
      this.boundaryFound = false;
      this.done = false;
      this.cancelled = false;
   }
   
   public void cancel() {
      cancelled = true;
   }
   
   public int getBytesRead() {
      return bytesRead;
   }
   
   public boolean isFinished() {
      return done;
   }

   // the buffer must be exhausted
   protected void read() 
      throws IOException
   {
      if (secondaryLength>0) {
         length = secondaryLength;
         System.arraycopy(secondary, 0, buffer, 0, secondaryLength);
      } else {
         length = data.read(buffer,0,buffer.length);
      }
      start = 0;
      bytesRead += length;
      scanForBoundary();
   }

   protected void scanForBoundary() 
      throws IOException
   {
      if (isFineLog) {
         log.fine("Scanning for boundary starting at "+start);
         log.fine("Scanning ... start="+start+", length="+length+", boundaryFound="+boundaryFound);
      }
      for (int i=start; !boundaryFound && (i+1)<length; i++) {
         if (buffer[i]==13 && buffer[i+1]==10) {
            if (isFineLog) {
               log.fine("Possible start at "+i);
            }
            boundaryFound = true;
            int blen = 0;
            for (int j=i+2; boundaryFound && j<length && blen<boundary.length; j++) {
               if (buffer[j]!=boundary[j-i-2]) {
                  if (isFineLog) {
                     log.fine("Boundary non-match at "+(j-i-2)+" "+j+"<"+length+" "+buffer[j]+"!="+boundary[j-i-2]);
                  }
                  boundaryFound = false;
               } else {
                  if (isFineLog) {
                     log.fine("Boundary match at "+(j-i-2)+" "+j+"<"+length);
                  }
                  blen++;
               }
            }
            if (boundaryFound && blen<boundary.length) {
               if (isFineLog) {
                  log.fine("Boundary may be broken across reads...");
               }
               secondaryLength = data.read(secondary,0,secondary.length);
               if (isFineLog) {
                  log.fine("Secondary length: "+secondaryLength);
               }
               for (int j=0; boundaryFound && j<secondaryLength && blen<boundary.length; j++) {
                  if (secondary[j]!=boundary[j+blen]) {
                     if (isFineLog) {
                        log.fine("Boundary does not match at "+(j+blen));
                     }
                     boundaryFound = false;
                  } else {
                     blen++;
                  }
               }
               if (boundaryFound && blen<boundary.length) {
                  boundaryFound = false;
               }
            }
            if (boundaryFound) {
               if (isFineLog) {
                  log.fine("Boundary found at "+i);
               }
               length = i;
            }
         }
      }
   }

   public Map<String,String> setup() 
      throws IOException
   {
      Map<String,String> headers = new TreeMap<String,String>();
      length = data.read(buffer,0,buffer.length);
      bytesRead = length;
      int boundaryEnd = 0;
      while (boundaryEnd<length && buffer[boundaryEnd]!=13 && buffer[boundaryEnd+1]!=10) {
         boundaryEnd++;
      }
      boundary = new byte[boundaryEnd];
      if (isFineLog) {
         log.fine("boundary.length="+boundaryEnd);
      }
      System.arraycopy(buffer, 0, boundary, 0, boundaryEnd);
      if (isFineLog) {
         log.fine("boundary: "+new String(boundary));
      }
      start = boundaryEnd+2;
      boolean headersDone = false;
      do {
         int end = start;
         while (end<length && buffer[end]!=13 && buffer[end+1]!=10) {
            end++;
         }
         if (buffer[end]==13 && buffer[end+1]==10) {
            int size = end-start;
            if (size>0) {
               byte [] headerData = new byte[size];
               System.arraycopy(buffer, start, headerData, 0, headerData.length);
               String header = new String(headerData);
               //System.out.println("Header: "+header);
               int colon = header.indexOf(':');
               if (colon>=0) {
                  String name = header.substring(0,colon);
                  String value = (colon+2)<header.length() ? header.substring(colon+2) : "";
                  headers.put(name, value);
               }
            } else {
               headersDone = true;
            }
            start = end+2;
         }
         if (end==length) {
            headersDone = true;
         }
      } while (!headersDone);
      scanForBoundary();
      return headers;
   }

   public InputStream getInputStream() {
      return new InputStream() {
         public int read() 
            throws IOException
         {
            if (cancelled) {
               throw new IOException("Read was cancelled.");
            }
            //System.out.println(start+" "+length);
            if (done) {
               return -1;
            }
            if (start>=length) {
               UploadSource.this.read();
            }
            if (start<length) {
               int value = buffer[start++]&0xff;
               return value;
            } else {
               done = true;
               return -1;
            }
         }
         
         public void close() 
            throws IOException
         {
            data.close();
         }

      };
   }
   
   public static Map<String,String> parseParameters(String value)
   {
      Logger log = Logger.getLogger(UploadSource.class.getName());
      Map<String,String> parameters = new TreeMap<String,String>();
      String [] parts = value.split(";");
      //log.info("parts.length="+parts.length);
      for (int i=1; i<parts.length; i++) {
         //log.info("part="+parts[i]);
         String part = parts[i].trim();
         int equals = part.indexOf('=');
         if (equals>0) {
            String name = part.substring(0,equals);
            String parameterValue = part.substring(equals+1);
            //log.info(name+"="+parameterValue);
            char quote = parameterValue.charAt(0);
            if (quote=='\'' || quote=='"') {
               parameterValue = parameterValue.substring(1,parameterValue.length()-1);
            }
            parameters.put(name,parameterValue);
         }
      }
      return parameters;
   }

static String fineLog =
"handlers= java.util.logging.ConsoleHandler\n"+
".level= FINE\n"+
"java.util.logging.ConsoleHandler.level = FINE\n"+
"java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter\n";
;
   public static void main(String [] args) {
      try {
         LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(fineLog.getBytes()));
      } catch (java.io.IOException ex) {
         ex.printStackTrace();
      }
      try {
         FileInputStream input = new FileInputStream(args[0]);
         UploadSource source = new UploadSource(Logger.getAnonymousLogger(),input);
         Map<String,String> headers = source.setup();
         String filename = null;
         for (String name : headers.keySet()) {
            String value = headers.get(name);
            System.out.println(name+": "+value);
            if (name.equalsIgnoreCase("Content-Disposition")) {
               int pos = value.indexOf("filename=");
               char quote = value.charAt(pos+9);
               int end = value.indexOf(pos+10,quote);
               filename = value.substring(pos+10,end);
            }
         }
         System.out.flush();
         if (filename==null) {
            System.err.println("Cannot find filename in headers.");
         } else {
            System.out.print("Output to "+filename+"\n");
            FileOutputStream output = new FileOutputStream(filename);
            byte [] buffer = new byte[16384];
            int len;
            InputStream data = source.getInputStream();
            while ((len=data.read(buffer,0,buffer.length))>=0) {
               output.write(buffer,0,len);
            }
            output.close();
         }
         input.close();

      } catch (Exception ex) {
         ex.printStackTrace();;
      }
   }

}
