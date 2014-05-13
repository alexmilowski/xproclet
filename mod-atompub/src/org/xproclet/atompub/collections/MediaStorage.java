/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import org.restlet.Context;
import org.restlet.data.MediaType;

/**
 *
 * @author alex
 */
public interface MediaStorage {
   
   MediaMetadata create(Map<String,Object> attributes, String collectionName, String fileName, InputStream data, MediaType contentType, Date lastModified)
      throws IOException;
   
   MediaMetadata head(Map<String,Object> attributes, String collectionName, String fileName)
      throws IOException;
   
   Media get(Map<String,Object> attributes, String collectionName, String fileName)
      throws IOException;
   
   void put(Map<String,Object> attributes, String collectionName, String fileName,InputStream data,MediaType contentType,Date lastModified)
      throws IOException;
   
   boolean delete(Map<String,Object> attributes,  String collectionName, String fileName)
      throws IOException;
   
   String generateFileName(MediaType contentType)
      throws IOException;
   
   void deleteCollection(Map<String,Object> attributes,  String collectionName)
      throws IOException;
}
