/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.MetadataService;

/**
 *
 * @author alex
 */
public class FileMediaStorage implements MediaStorage {
   
   static class FileMetadata implements MediaMetadata {
      MediaType contentType;
      File mediaFile;
      FileMetadata(File mediaFile,MediaType contentType) {
         this.mediaFile = mediaFile;
         this.contentType = contentType;
      }
      
      public String getName() {
         return mediaFile.getName();
      }
      
      public Date getLastModified() {
         return new Date(mediaFile.lastModified());
      }

      public MediaType getContentType() {
         return contentType;
      }

      public long getSize() {
         return mediaFile.length();
      }

   }
   static class FileMedia extends FileMetadata implements Media {

      FileMedia(File mediaFile,MediaType contentType) {
         super(mediaFile,contentType);
      }
      
      public Representation getRepresentation() {
         FileRepresentation fileRep = new FileRepresentation(mediaFile,contentType);
         fileRep.setModificationDate(getLastModified());
         fileRep.setSize(getSize());
         return fileRep;
      }
   }
   File defaultMediaDir;
   Context context;
   protected MetadataService metadataService;
   public FileMediaStorage(Context context) {
      this.context = context;
      Object o = context.getAttributes().get(CollectionBaseRestlet.MEDIA_HREF_ATTR);
      if (o!=null) {
         Reference defaultMediaRef = new Reference(o.toString());
         if (defaultMediaRef.getScheme().equals("file")) {
            defaultMediaDir = new File(defaultMediaDir.getPath());
            if (defaultMediaDir.exists()) {
               if (!defaultMediaDir.isDirectory()) {
                  getLogger().severe("Media directory "+defaultMediaDir+" is not a directory.");
                  defaultMediaDir = null;
               }
            } else {
               getLogger().severe("Media directory "+defaultMediaDir+" does not exist.");
               defaultMediaDir = null;
            }
         }
      }
      metadataService = (MetadataService)context.getAttributes().get(CollectionBaseRestlet.METADATA_SERVICE);
      if (metadataService==null) {
         metadataService = new MetadataService();
         metadataService.addCommonExtensions();
         metadataService.addExtension("jpg", MediaType.IMAGE_JPEG, true);
      }
   }
   
   protected Logger getLogger() {
      return context.getLogger();
   }
   
   public MediaMetadata create(Map<String,Object> attributes,  String collectionName, String fileName, InputStream data, MediaType contentType, Date lastModified)
      throws IOException
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      if (!collectionDir.exists()) {
         if (!collectionDir.mkdir()) {
            throw new IOException("Cannot create collection directory "+collectionDir.getAbsolutePath());
         }
      }
      File mediaFile = new File(collectionDir,fileName);
      if (mediaFile.exists()) {
         return null;
      }
      store(mediaFile,data, lastModified);
      return new FileMetadata(mediaFile,getContentType(mediaFile));
   }
   
   public MediaMetadata head(Map<String,Object> attributes,  String collectionName, String fileName)
      throws IOException 
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      File mediaFile = new File(collectionDir,fileName);
      return mediaFile.exists() ? new FileMetadata(mediaFile,getContentType(mediaFile)) : null;
   }
   
   public Media get(Map<String,Object> attributes,  String collectionName, String fileName)
      throws IOException 
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      File mediaFile = new File(collectionDir,fileName);
      return mediaFile.exists() ? new FileMedia(mediaFile,getContentType(mediaFile)) : null;
   }
   
   public void put(Map<String,Object> attributes, String collectionName, String fileName,InputStream data,MediaType contentType,Date lastModified)
      throws IOException 
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      File mediaFile = new File(collectionDir,fileName);
      store(mediaFile,data, lastModified);
   }
   
   public boolean delete(Map<String,Object> attributes,  String collectionName, String fileName)
      throws IOException
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      File mediaFile = new File(collectionDir,fileName);
      return mediaFile.delete();
   }
   
   public String generateFileName(MediaType contentType) {
      String file = UUID.randomUUID().toString();
      String extension = metadataService.getExtension(contentType);
      if (extension!=null) {
         file = file+"."+extension;
      } else {
         file = file+".bin";
      }
      return file;
   }
   public void deleteCollection(Map<String,Object> attributes,  String collectionName)
   {
      File mediaDir = getMediaDir(attributes);
      File collectionDir = new File(mediaDir,collectionName);
      final boolean isFineLog = getLogger().isLoggable(Level.FINE);
      collectionDir.listFiles(new FileFilter() {
         public boolean accept(File file)
         {
            if (file.isFile()) {
               if (!file.delete()) {
                  getLogger().warning("Cannot delete file: "+file.getAbsolutePath());
               } else if (isFineLog) {
                  getLogger().fine("Deleted: "+file.getAbsolutePath());
               }
            }
            return false;
         }
      });
      if (!collectionDir.delete()) {
         getLogger().warning("Cannot collection media directory: "+collectionDir.getAbsolutePath());
      } else if (isFineLog) {
         getLogger().fine("Deleted: "+collectionDir.getAbsolutePath());
      }
      
   }
   protected File getMediaDir(Map<String,Object> attributes) {
      Object o = attributes.get(CollectionBaseRestlet.MEDIA_HREF_ATTR);
      if (o==null) {
         return defaultMediaDir;
      }
      Reference dirMediaRef = new Reference(o.toString());
      if (dirMediaRef.getScheme().equals("file")) {
         File mediaDir = new File(dirMediaRef.getPath());
         if (mediaDir.exists()) {
            if (!mediaDir.isDirectory()) {
               getLogger().severe("Media directory "+mediaDir+" is not a directory.");
               mediaDir = null;
            }
         } else {
            getLogger().severe("Media directory "+mediaDir+" does not exist.");
            mediaDir = null;
         }
         return mediaDir;
      } else {
         return null;
      }
   }
   
   protected void store(File mediaFile,InputStream data,Date lastModified) 
      throws IOException
   {
         boolean replacing = mediaFile.exists();
         File outFile = replacing ? new File(mediaFile.getAbsolutePath()+"."+UUID.randomUUID()) : mediaFile;
         try {
            OutputStream os = new FileOutputStream(outFile);
            byte [] buffer = new byte[CollectionRestlet.BUFFER_SIZE];
            int len;
            while ((len = data.read(buffer))>0) {
               os.write(buffer,0,len);
            }
            os.flush();
            os.close();
         } catch (IOException ex) {
            outFile.delete();
            throw ex;
         }
         if (replacing) {
            mediaFile.delete();
            outFile.renameTo(mediaFile);
         }
         if (lastModified!=null) {
            mediaFile.setLastModified(lastModified.getTime());
         }
   }
   
   protected MediaType getContentType(File mediaFile)
   {
      int pos = mediaFile.getName().lastIndexOf('.');
      if (pos<0) {
         return metadataService.getDefaultMediaType();
      } else {
         String ext = mediaFile.getName().substring(pos+1);
         MediaType contentType = metadataService.getMediaType(ext);
         return contentType==null ? metadataService.getDefaultMediaType() : contentType;
      }
   }
}
