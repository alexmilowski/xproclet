/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Reference;
import org.restlet.engine.util.DateUtils;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.service.MetadataService;

/**
 *
 * @author alex
 */
public class S3MediaStorage implements MediaStorage {

   final String S3_CREDENTIALS_ATTR = "app.s3.credentials";
   final String S3_BUCKET_ATTR = "app.s3.bucket";
   final String S3_TMP_DIR = "app.s3.tmp";
   
   static class S3Metadata implements MediaMetadata {

      String name;
      MediaType contentType;
      Date lastModified;
      long size;
      S3Metadata(String name,MediaType contentType,Date lastModified,long size) {
         this.name = name;
         this.contentType = contentType;
         this.lastModified = lastModified;
         this.size = size;
      }
      public String getName() {
         return name;
      }

      public Date getLastModified() {
         return lastModified;
      }

      public MediaType getContentType() {
         return contentType;
      }

      public long getSize() {
         return size;
      }
      
   }
   
   static class S3Media implements Media {
      S3Object object;
      String name;
      Representation entity;
      S3Media(S3Object object,String name) {
         this.object = object;
         this.name = name;
         entity = new InputRepresentation(object.getObjectContent(),MediaType.valueOf(object.getObjectMetadata().getContentType()));
         String lastModifiedValue = object.getObjectMetadata().getUserMetadata().get("last-modified");
         Date lastModified = lastModifiedValue==null ? object.getObjectMetadata().getLastModified() : DateUtils.parse(lastModifiedValue,DateUtils.FORMAT_RFC_3339);
         entity.setModificationDate(lastModified);
         entity.setSize(object.getObjectMetadata().getContentLength());
      }

      @Override
      public Representation getRepresentation() {
         return entity;
      }

      @Override
      public String getName() {
         return name;
      }

      @Override
      public Date getLastModified() {
         return entity.getModificationDate();
      }

      @Override
      public MediaType getContentType() {
         return entity.getMediaType();
      }

      @Override
      public long getSize() {
         return entity.getSize();
      }
   }
   
   String defaultBucket;
   Context context;
   AmazonS3Client s3Client;
   MetadataService metadataService;
   File tmpDir;
   
   public S3MediaStorage(Context context) {
      this.context = context;
      AWSCredentials credentials = (AWSCredentials)context.getAttributes().get(S3_CREDENTIALS_ATTR);
      String key = context.getParameters().getFirstValue("aws.key");
      String secret = context.getParameters().getFirstValue("aws.secret");
      if (key!=null && secret!=null) {
         credentials = new BasicAWSCredentials(key,secret);
      }
      if (credentials==null) {
         getLogger().severe("No AWS credentials for S3 media storage.");
         s3Client = null;
      } else {
         s3Client = new AmazonS3Client(credentials);
      }
      getLogger().fine("s3Client="+s3Client);
      metadataService = (MetadataService)context.getAttributes().get(CollectionBaseRestlet.METADATA_SERVICE);
      if (metadataService==null) {
         metadataService = new MetadataService();
         metadataService.addCommonExtensions();
         metadataService.addExtension("jpg", MediaType.IMAGE_JPEG, true);
      }
      Object o = context.getParameters().getFirstValue(S3_BUCKET_ATTR);
      if (o!=null) {
         defaultBucket = o.toString();
      }
      String tmpDirName = context.getParameters().getFirstValue(S3_TMP_DIR);
      if (tmpDirName!=null) {
         if (tmpDirName.startsWith("file:")) {
            URI tmpURI = URI.create(tmpDirName);
            tmpDir = new File(tmpURI.getPath());
         } else {
            tmpDir = new File(tmpDirName);
         }
      } else {
         tmpDir = null;
      }
   }
   
   protected Logger getLogger() {
      return context.getLogger();
   }
   
   @Override
   public MediaMetadata create(Map<String, Object> attributes, String collectionName, String fileName, InputStream data, MediaType contentType, Date lastModified) throws IOException {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      if (head(attributes,collectionName,fileName)!=null) {
         return null;
      }
      String bucketName = getBucketName(attributes);
      String key = getKey(collectionName,fileName);
      return store(bucketName,key,fileName,data,contentType,lastModified);
   }

   @Override
   public MediaMetadata head(Map<String, Object> attributes, String collectionName, String fileName) 
      throws IOException 
   {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      String bucketName = getBucketName(attributes);
      if (bucketName==null) {
         throw new IOException("S3 Bucket not configured for "+attributes.get("app.database"));
      }
      String key = getKey(collectionName,fileName);
      try {
         ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
         String lastModifiedValue = metadata.getUserMetadata().get("last-modified");
         Date lastModified = lastModifiedValue==null ? metadata.getLastModified() : DateUtils.parse(lastModifiedValue,DateUtils.FORMAT_RFC_3339);
         return new S3Metadata(fileName,MediaType.valueOf(metadata.getContentType()),lastModified,metadata.getContentLength());
      } catch (AmazonServiceException ex) {
         if (ex.getStatusCode()==404) {
            return null;
         } else {
            throw new IOException("Cannot get object due to S3 error.",ex);
         }
      } catch (AmazonClientException ex) {
         throw new IOException("Cannot get object due to S3 error.",ex);
      }
   }

   @Override
   public Media get(Map<String, Object> attributes, String collectionName, String fileName) throws IOException {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      String bucketName = getBucketName(attributes);
      if (bucketName==null) {
         throw new IOException("S3 Bucket not configured for "+attributes.get("app.database"));
      }
      String key = getKey(collectionName,fileName);
      try {
         S3Object object = s3Client.getObject(bucketName, key);
         return new S3Media(object,fileName);
      } catch (AmazonServiceException ex) {
         if (ex.getStatusCode()==404) {
            return null;
         } else {
            throw new IOException("Cannot get object due to S3 error.",ex);
         }
      } catch (AmazonClientException ex) {
         throw new IOException("Cannot get object due to S3 error.",ex);
      }
   }

   @Override
   public void put(Map<String,Object> attributes, String collectionName, String fileName,InputStream data,MediaType contentType,Date lastModified)
      throws IOException
   {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      String bucketName = getBucketName(attributes);
      if (bucketName==null) {
         throw new IOException("S3 Bucket not configured for "+attributes.get("app.database"));
      }
      String key = getKey(collectionName,fileName);
      store(bucketName,key,fileName,data,contentType,lastModified);
   }
   
   @Override
   public boolean delete(Map<String, Object> attributes, String collectionName, String fileName) throws IOException {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      String bucketName = getBucketName(attributes);
      if (bucketName==null) {
         throw new IOException("S3 Bucket not configured for "+attributes.get("app.database"));
      }
      String key = getKey(collectionName,fileName);
      try {
         s3Client.deleteObject(bucketName, key);
         return true;
      } catch (AmazonServiceException ex) {
         if (ex.getStatusCode()==404) {
            return false;
         } else {
            throw new IOException("Cannot get object due to S3 error.",ex);
         }
      } catch (AmazonClientException ex) {
         throw new IOException("Cannot get object due to S3 error.",ex);
      }
   }

   @Override
   public String generateFileName(MediaType contentType) throws IOException {
      String file = UUID.randomUUID().toString();
      String extension = metadataService.getExtension(contentType);
      if (extension!=null) {
         file = file+"."+extension;
      } else {
         file = file+".bin";
      }
      return file;
   }

   @Override
   public void deleteCollection(Map<String, Object> attributes, String collectionName) 
      throws IOException 
   {
      if (s3Client==null) {
         throw new IOException("S3 Client is not available due to configuration error.");
      }
      String bucketName = getBucketName(attributes);
      if (bucketName==null) {
         throw new IOException("S3 Bucket not configured for "+attributes.get("app.database"));
      }
      try {
         ObjectListing listing = null;
         do {
            listing = listing==null ? s3Client.listObjects(bucketName, collectionName+"/") : s3Client.listNextBatchOfObjects(listing);
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
               s3Client.deleteObject(bucketName, summary.getKey());
            }
         } while (listing.isTruncated());
      } catch (AmazonClientException ex) {
         throw new IOException("Cannot delete collection objects due to S3 error.",ex);
      }
   }
   
   protected String getBucketName(Map<String,Object> attributes) {
      Object o = attributes.get(S3_BUCKET_ATTR);
      return o==null ? defaultBucket : o.toString();
   }
   
   protected String getKey(String collectionName, String fileName)
   {
      return collectionName+"/"+Reference.encode(fileName);
   }
   
   protected File cache(InputStream data)
      throws IOException
   {
      File tmpFile = File.createTempFile("data", ".bin",tmpDir);
      
      try {
         OutputStream os = new FileOutputStream(tmpFile);
         byte [] buffer = new byte[CollectionRestlet.BUFFER_SIZE];
         int len;
         while ((len = data.read(buffer))>0) {
            os.write(buffer,0,len);
         }
         os.flush();
         os.close();
         return tmpFile;
      } catch (IOException ex) {
         tmpFile.delete();
         throw ex;
      }
      
   }
   
   protected MediaMetadata store(String bucketName, String key,String fileName,InputStream data, MediaType contentType, Date lastModified)
      throws IOException
   {
      File tmpFile = cache(data);
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentType(contentType.toString());
      metadata.setContentLength(tmpFile.length());
      metadata.setLastModified(lastModified==null ? new Date() : lastModified);
      if (lastModified!=null) {
         metadata.addUserMetadata("last-modified", DateUtils.format(lastModified,DateUtils.FORMAT_RFC_3339.get(0)));
      }
      InputStream is = new FileInputStream(tmpFile);
      try {
         PutObjectResult result = s3Client.putObject(bucketName, key, is, metadata);
         is.close();
         return new S3Metadata(fileName,contentType,lastModified,metadata.getContentLength());
      } catch (AmazonClientException ex) {
         is.close();
         throw new IOException("Cannot create object due to S3 error.",ex);
      } finally {
         tmpFile.delete();
      }
      
   }
}
