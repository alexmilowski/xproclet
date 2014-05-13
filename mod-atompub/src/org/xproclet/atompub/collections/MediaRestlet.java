/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.util.DateUtils;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;

/**
 *
 * @author alex
 */
public class MediaRestlet extends CollectionBaseRestlet {
   
   public MediaRestlet(Context context)
   {
      super(context);
   }
   
   public void handle(Request request, Response response) {
      String name = request.getAttributes().get("name").toString();
      String file = request.getAttributes().get("file").toString();
      file = Reference.decode(file);
      //getLogger().info("collection="+name+", file="+file);
      if (request.getMethod().equals(Method.GET)) {
         Form query = request.getResourceRef().getQueryAsForm();
         String scaleParameter = query.getFirstValue("scale");
         if (mediaStorage!=null) {
            Media media = null;
            try {
               media = mediaStorage.get(request.getAttributes(), name, file);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error while getting media file "+file+" in collection "+name,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            if (media!=null) {
               MediaType contentType = media.getContentType();
               if (scaleParameter!=null && !contentType.getMainType().equals("image")) {
                  response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                  response.setEntity(new StringRepresentation("Cannot preview media type "+contentType,MediaType.TEXT_PLAIN));
                  // Some storage systems have an open stream that we must close (e.g. AWS S3)
                  try {
                     media.getRepresentation().getStream().close();
                  } catch (IOException ex) {
                     getLogger().log(Level.SEVERE,"Cannot close entity stream from media storage.",ex);
                  }
                  return;
               }
               if (scaleParameter!=null) {
                  String [] parts = scaleParameter.split("x");
                  int height= -1,width = -1;
                  if (parts.length==2) {
                     width = Integer.parseInt(parts[0]);
                     height = Integer.parseInt(parts[0]);
                  } else {
                     width = Integer.parseInt(parts[0]);
                  }
                  InputStream imageData = null;
                  try {
                     Representation imageEntity = media.getRepresentation();
                     imageData = imageEntity.getStream();
                     BufferedImage image = ImageIO.read(imageData);
                     if (height<0) {
                        double scale = (double)width/image.getWidth();
                        //getLogger().info("Width: "+image.getWidth()+" scaled to "+width+" = "+scale);
                        height = (int)(image.getHeight()*scale);
                     }
                     final BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                     int pos = file.lastIndexOf('.');
                     final String ext = pos<0 ? "jpg" : file.substring(pos+1);
                     scaledImage.createGraphics().drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH),0,0,null);                     
                     response.setStatus(Status.SUCCESS_OK);
                     response.setEntity(new OutputRepresentation(contentType) {
                        public void write(OutputStream os) {
                           try {
                              ImageIO.write(scaledImage, ext, os);
                           } catch (IOException ex) {
                              getLogger().log(Level.SEVERE,"I/O error while writing scaled image.",ex);
                           }
                        }
                     });
                     return;
                  } catch (IOException ex) {
                     response.setStatus(Status.SERVER_ERROR_INTERNAL);
                     response.setEntity(new StringRepresentation("I/O error while reading image."));
                     getLogger().log(Level.SEVERE,"I/O error while reading "+file+" in collection "+name,ex);
                     return;
                  } finally {
                     if (imageData!=null) {
                        try {
                           imageData.close();
                        } catch (IOException ex) {
                           getLogger().log(Level.SEVERE,"I/O error while closing image data input.",ex);
                        }
                     }
                  }
               }
               response.setStatus(Status.SUCCESS_OK);
               response.setEntity(media.getRepresentation());
               return;
            }
         }
      } else if (request.getMethod().equals(Method.HEAD)) {
         if (mediaStorage!=null) {
            MediaMetadata media = null;
            try {
               media = mediaStorage.head(request.getAttributes(), name, file);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error while getting media file "+file+" in collection "+name,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            if (media!=null) {
               final MediaMetadata mediaRef = media;
               MediaType contentType = media.getContentType();
               response.setStatus(Status.SUCCESS_OK);
               Representation entity = new StringRepresentation("",contentType) {
                  public long getSize() {
                     return mediaRef.getSize();
                  }
               };
               entity.setModificationDate(media.getLastModified());
               entity.setSize(media.getSize());
               //getLogger().info("Head on "+mediaFile.getAbsolutePath()+", size="+mediaFile.length()+", "+entity.getModificationDate());
               response.setEntity(entity);
               List<CacheDirective> directives = new ArrayList<CacheDirective>();
               directives.add(CacheDirective.noCache());
               response.setCacheDirectives(null);
               return;
            }
         }
      } else if (request.getMethod().equals(Method.PUT)) {
         if (!request.isEntityAvailable()) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
         }
         if (mediaStorage!=null) {
            MediaMetadata media = null;
            try {
               media = mediaStorage.head(request.getAttributes(), name, file);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error while getting media file "+file+" in collection "+name,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            if (media!=null) {
               int extPos = file.lastIndexOf('.');
               Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
               String xLastModified = headers==null ? null : headers.getFirstValue("X-Last-Modified");
               Date lastModified = xLastModified==null ? null : DateUtils.parse(xLastModified, DateUtils.FORMAT_RFC_1123);

               // Store the media 
               Representation entity = request.getEntity();
               InputStream data = null;
               try {
                  data = entity.getStream();
                  mediaStorage.put(request.getAttributes(), name, file, data, entity.getMediaType(),lastModified);
               } catch (IOException ex) {
                  getLogger().log(Level.SEVERE,"Cannot write to "+file+" in collection "+name,ex);
                  response.setStatus(Status.SERVER_ERROR_INTERNAL);
                  return;
               } finally {
                  if (data!=null) {
                     try {
                        data.close();
                     } catch (IOException ex) {
                        getLogger().log(Level.SEVERE,"Error closing entity input.",ex);
                     }
                  }
               }
               response.setStatus(Status.SUCCESS_NO_CONTENT);
               return;
            }
         }
      } else if (request.getMethod().equals(Method.POST)) {
         if (!request.isEntityAvailable()) {
            response.setEntity(new StringRepresentation("No entity provided in request.",MediaType.TEXT_PLAIN));
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
         }
         MediaType mediaType = request.getEntity().getMediaType();
         if (!MediaType.APPLICATION_ALL_XML.includes(mediaType) && !MediaType.TEXT_XML.equals(mediaType)) {
            response.setEntity(new StringRepresentation("Only XML media is allowed for append.",MediaType.TEXT_PLAIN));
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            return;
         }
         if (mediaStorage!=null) {
            MediaMetadata media = null;
            try {
               media = mediaStorage.head(request.getAttributes(), name, file);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O error while getting media file "+file+" in collection "+name,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            if (media!=null) {
               response.setEntity(new StringRepresentation("Non-XML media was found associated with this entry.",MediaType.TEXT_PLAIN));
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               return;
            }
         }
      }
      super.handle(request, response);
   }
}
