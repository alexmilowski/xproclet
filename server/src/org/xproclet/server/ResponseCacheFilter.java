/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.server;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class ResponseCacheFilter extends Filter {
   
   protected long duration;
   protected File cacheDir;
   protected List<MediaType> mediaTypes;
   
   class CacheDelete implements Runnable {
      List<File> queue;
      CacheDelete(File dir) {
         this.queue = new ArrayList<File>();
         queue.add(dir);
      }
      public void run() {
         while (!queue.isEmpty()) {
            File dir = queue.remove(0);
            dir.listFiles(new FileFilter() {
               public boolean accept(File f)
               {
                  if (f.isDirectory()) {
                     queue.add(f);
                  } else {
                     f.delete();
                     getLogger().info("Delete: "+f);
                  }
                  return false;
               }
            });
            dir.delete();
            getLogger().info("Delete: "+dir);
         }
         getLogger().info("Cache delete finished.");
      }
   }
   
   public ResponseCacheFilter(Context context) {
      super(context);
      cacheDir = null;
      duration = -1;
   }
   
   protected long parseDuration(String value)
      throws java.text.ParseException
   {
      value = value.trim();
      if (!value.startsWith("PT")) {
         throw new java.text.ParseException("Unrecognized duration: "+value,0);
      }
      if (value.endsWith("M") || value.endsWith("H")) {
         int n = Integer.parseInt(value.substring(2,value.length()-1));
         if (value.charAt(value.length()-1)=='M') {
            return n*60*1000;
         } else {
            return n*60*60*1000;
         }
      } else {
         throw new java.text.ParseException("Unsupported duration: "+value,value.length()-1);
      }
   }
   
   protected File getCacheFile(String path)
   {
      return new File(cacheDir,Reference.encode(path));
   }
   protected File getCacheMetadataFile(String path)
   {
      return new File(cacheDir,Reference.encode(path)+".meta");
   }
   
   protected boolean cleanup(File cacheDir)
   {
      File oldCacheDir = new File(cacheDir.getParentFile(),cacheDir.getName()+"."+System.currentTimeMillis());
      if (cacheDir.renameTo(oldCacheDir)) {
         if (cacheDir.mkdir()) {
            Thread deleteDir = new Thread(new CacheDelete(oldCacheDir));
            deleteDir.start();
            return true;
         }
      }
      return false;
   }
   
   protected boolean isCacheValid(long lastModified, Request request)
   {
      return (System.currentTimeMillis()-lastModified)<duration;
   }
   
   protected void cacheResponse(File cacheFile, File metadataFile, Response response)
      throws IOException
   {
      try {
         Representation entity = response.getEntity();
         InputStream is = entity.getStream();
         OutputStream os = new FileOutputStream(cacheFile);
         byte [] buffer = new byte[8192];
         int len = 0;
         while ((len=is.read(buffer))>0) {
            os.write(buffer,0,len);
         }
         os.close();
         is.close();
         MediaType type = entity.getMediaType();
         Writer w = new OutputStreamWriter(new FileOutputStream(metadataFile),"UTF-8");
         w.write(type.toString());
         w.close();
         response.setEntity(new FileRepresentation(cacheFile,entity.getMediaType()));
      } catch (IOException ex) {
         cacheFile.delete();
         metadataFile.delete();
         throw ex;
      }
   }
   
   protected void makeResponse(File cacheFile,File metadataFile,Response response)
      throws IOException
   {
      Reader metadataReader = new InputStreamReader(new FileInputStream(metadataFile),"UTF-8");
      LineNumberReader lineReader = new LineNumberReader(metadataReader);
      String type = lineReader.readLine();
      response.setEntity(new FileRepresentation(cacheFile,MediaType.valueOf(type)));
      metadataReader.close();
   }
   
   public void start() 
      throws Exception
   {
      super.start();
      String cacheDirURI = getContext().getParameters().getFirstValue("cache.dir");
      if (cacheDirURI!=null) {
         URI uri = URI.create(cacheDirURI);
         if (uri.getScheme().equals("file")) {
            cacheDir = new File(uri.getPath());
            if (cacheDir.exists()) {
               if (!cleanup(cacheDir)) {
                  getLogger().severe("Cannot cleanup cache directory "+cacheDir);
                  cacheDir = null;
                  return;
               }
            } else {
               getLogger().info("Creating cache directory "+cacheDir);
               if (!cacheDir.mkdirs()) {
                  getLogger().severe("Cannot create cache directory: "+cacheDir);
                  cacheDir = null;
                  return;
               }
            }
         } else {
            getLogger().severe("Non-file cache directory: "+cacheDirURI);
            return;
         }
      } else {
         getLogger().severe("Missing 'cache.dir' parameter for caching filter.");
         return;
      }
      String expiry = getContext().getParameters().getFirstValue("expiry");
      if (expiry!=null) {
         try {
            duration = parseDuration(expiry);
         } catch (java.text.ParseException ex) {
            getLogger().severe("Cannot parse duration: "+ex.getMessage());
         }
      } else {
         getLogger().severe("Missing 'expiry' parameter for caching filter.");
      }
      String typesSpec = getContext().getParameters().getFirstValue("types");
      mediaTypes = new ArrayList<MediaType>();
      if (typesSpec!=null) {
         String [] types = typesSpec.split(",");
         for (int i=0; i<types.length; i++) {
            try {
               mediaTypes.add(MediaType.valueOf(types[i].trim()));
            } catch (IllegalArgumentException ex) {
               getLogger().severe("Bad media type value "+types[i]+", "+ex.getMessage());
            }
         }
      }
      if (cacheDir==null || duration<1) {
         getLogger().info("No caching.");
      } else {
         getLogger().info("Caching to directory "+cacheDir);
         getLogger().info("Duration: "+duration);
      }
   }
   
   protected int beforeHandle(Request request, Response response) {
      if (cacheDir==null || duration<1) {
         return Filter.CONTINUE;
      }
      String href = request.getResourceRef().getRemainingPart();
      getLogger().info("Checking cache for "+href);
      request.getAttributes().put("cache.href", href);
      File cacheFile = getCacheFile(href);
      File cacheMetadataFile = getCacheMetadataFile(href);
      if (cacheFile.exists()) {
         if (isCacheValid(cacheFile.lastModified(),request)) {
            getLogger().info("Using cache file: "+cacheFile);
            try {
               makeResponse(cacheFile,cacheMetadataFile,response);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error processing cache files.",ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL, "I/O error processing cache files.");
               return Filter.STOP;
            }
            return Filter.STOP;
         } else {
            cacheFile.delete();
            cacheMetadataFile.delete();
         }
      } else if (cacheMetadataFile.exists()) {
         cacheMetadataFile.delete();
      }
      return Filter.CONTINUE;
   }
   protected void afterHandle(Request request, Response response) {
      if (cacheDir==null || duration<1) {
         return;
      }
      String href = (String)request.getAttributes().get("cache.href");
      if (response.getStatus().isSuccess()) {
         getLogger().info("Caching "+href);
         File cacheFile = getCacheFile(href);
         //getLogger().info("Cache file: "+cacheFile);
         File cacheMetadataFile = getCacheMetadataFile(href);
         try {
            cacheResponse(cacheFile,cacheMetadataFile,response);
         } catch (IOException ex) {
            getLogger().log(Level.SEVERE,"I/O error caching response.",ex);
            response.setStatus(Status.SERVER_ERROR_INTERNAL, "I/O error caching response.");
         }
      }
   }
}
