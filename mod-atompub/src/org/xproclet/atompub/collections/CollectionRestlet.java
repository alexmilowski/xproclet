/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.runtime.XPipeline;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Level;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmSequenceIterator;
import org.xproclet.xproc.XPipelineContext;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.engine.util.DateUtils;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class CollectionRestlet extends CollectionBaseRestlet {
   final static QName COLLECTION_NAME = QName.fromClarkName("{http://www.w3.org/2007/app}collection");
   final static QName CATEGORY_NAME = QName.fromClarkName("{http://www.w3.org/2005/Atom}category");
   final static QName SCHEME_NAME = QName.fromClarkName("scheme");
   final static QName TERM_NAME = QName.fromClarkName("term");
   public static final String BASE_HREF_PARAMETER = "app.base.href";
   public static final String CHECK_MEDIA_PARAMETER = "app.check.media";
   public static final String CREATE_MEDIA_PARAMETER = "app.create.media";
   public static final String CREATE_MEDIA_ENTRY_PARAMETER = "app.create.media.entry";
   
   static int BUFFER_SIZE = 1024*16;
   static QName USER_OPTION = QName.fromClarkName("xdb.user");
   static QName PASSWORD_OPTION = QName.fromClarkName("xdb.password");
   static QName HOST_OPTION = QName.fromClarkName("xdb.host");
   static QName PORT_OPTION = QName.fromClarkName("xdb.port");
   static QName NAME_OPTION = QName.fromClarkName("name");
   static QName FILE_OPTION = QName.fromClarkName("file");
   static QName HIDDEN_OPTION = QName.fromClarkName("hidden");
   static QName MEDIA_TYPE_OPTION = QName.fromClarkName("media-type");

   static QName YES_NAME = QName.fromClarkName("yes");
   static QName ID_NAME = QName.fromClarkName("id");
   static QName OK_NAME = QName.fromClarkName("ok");
   URI checkCollectionFeedMedia;
   URI createCollectionFeedMedia;
   URI createMediaEntry;
   public CollectionRestlet(Context context)
   {
      super(context);
      
      String baseURI = context.getParameters().getFirstValue(BASE_HREF_PARAMETER);
      
      String value = context.getParameters().getFirstValue(CHECK_MEDIA_PARAMETER);
      if (value!=null) {
         try {
            checkCollectionFeedMedia = resolve(baseURI==null ? null : new URI(baseURI),value);
         } catch (URISyntaxException ex) {
            getLogger().log(Level.SEVERE,"Invalid pipeline uri: "+value,ex);
         }
      } else {
         getLogger().severe("Missing "+CHECK_MEDIA_PARAMETER+" parameter.");
      }
      
      value = context.getParameters().getFirstValue(CREATE_MEDIA_PARAMETER);
      if (value!=null) {
         try {
            createCollectionFeedMedia = resolve(baseURI==null ? null : new URI(baseURI),value);
         } catch (URISyntaxException ex) {
            getLogger().log(Level.SEVERE,"Invalid pipeline uri: "+value,ex);
         }
      } else {
         getLogger().severe("Missing "+CREATE_MEDIA_PARAMETER+" parameter.");
      }
      
      value = context.getParameters().getFirstValue(CREATE_MEDIA_ENTRY_PARAMETER);
      if (value!=null) {
         try {
            createMediaEntry = resolve(baseURI==null ? null : new URI(baseURI),value);
         } catch (URISyntaxException ex) {
            getLogger().log(Level.SEVERE,"Invalid pipeline uri: "+value,ex);
         }
      } else {
         getLogger().severe("Missing "+CREATE_MEDIA_ENTRY_PARAMETER+" parameter.");
      }
      
   }
   
   public String getStringAttribute(Request request, String name) {
      Object o = request.getAttributes().get(name);
      if (o==null) {
         o = getContext().getAttributes().get(name);
      }
      return o==null ? null : o.toString();
   }
   
   public void handle(Request request, Response response) {
      String collectionName = getOptionValue(request,NAME_OPTION);
      if (request.getMethod().equals(Method.POST)) {
         if (!request.isEntityAvailable()) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new StringRepresentation("POST requires an entity body."));
            return;
         }            
         Representation entity = request.getEntity();
         MediaType mediaType = entity.getMediaType();
         
         // Entry post, handle with default pipeline
         if (MediaType.APPLICATION_ATOM.equals(mediaType, true)) {
            super.handle(request, response);
            if (response.getStatus().isSuccess()) {
               Object o = response.getAttributes().get("org.xproclet.atompub.id");
               if (o!=null) {
                  Request entryRequest = new Request(Method.GET,new Reference("riap://host/collections/"+collectionName+"/_/"+o+".atom"));
                  entryRequest.setHostRef(request.getHostRef());
                  entryRequest.getAttributes().put("org.xproclet.www.identity",request.getAttributes().get("org.xproclet.www.identity"));
                  Response entryResponse = getContext().getClientDispatcher().handle(entryRequest);
                  getLogger().info("Entry response: "+entryResponse.getStatus());
                  if (entryResponse.getStatus().isSuccess()) {
                     response.setEntity(entryResponse.getEntity());
                  }
               } else {
                  getLogger().warning("No ID returned by pipeline after entry creation.");
               }
            }
            return;
         }
         
         boolean isXML = MediaType.APPLICATION_ALL_XML.includes(mediaType) || MediaType.TEXT_XML.equals(mediaType);
         
         if (checkCollectionFeedMedia==null || createCollectionFeedMedia==null || createMediaEntry==null || (!isXML && mediaStorage==null)) {
            response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
            return;
         }

         Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
         String file = headers==null ? null : headers.getFirstValue("Slug");
         if (file==null) {
            try {
               file = mediaStorage.generateFileName(mediaType);
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"Cannot generate file name.",ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
         } else {
            file = Reference.decode(file);
         }
         String xLastModified = headers==null ? null : headers.getFirstValue("X-Last-Modified");
         Date lastModified = xLastModified==null ? null : DateUtils.parse(xLastModified, DateUtils.FORMAT_RFC_1123);

         // Check to make sure that the slug does not conflict with media stored in the XML DB
         XPipelineContext xproc = null;
         try {
            xproc = getCache().get(checkCollectionFeedMedia);
         } catch (Exception ex) {
            getLogger().log(Level.SEVERE,"Cannot compile pipeline: "+checkCollectionFeedMedia,ex);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            return;
         }
         
         try {

            xproc.getPipeline().passOption(USER_OPTION, new RuntimeValue(getOptionValue(request,USER_OPTION)));
            xproc.getPipeline().passOption(PASSWORD_OPTION, new RuntimeValue(getOptionValue(request,PASSWORD_OPTION)));
            xproc.getPipeline().passOption(HOST_OPTION, new RuntimeValue(getOptionValue(request,HOST_OPTION)));
            xproc.getPipeline().passOption(PORT_OPTION, new RuntimeValue(getOptionValue(request,PORT_OPTION)));
            xproc.getPipeline().passOption(NAME_OPTION, new RuntimeValue(collectionName));
            xproc.getPipeline().passOption(FILE_OPTION, new RuntimeValue(file));

            try {
               xproc.getPipeline().run();
            } catch (Exception ex) {
               getLogger().log(Level.SEVERE,"Cannot run pipeline: "+checkCollectionFeedMedia,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }

            String outputPort = xproc.getPipeline().getOutputs().iterator().next();
            final ReadablePipe rpipe = xproc.getPipeline().readFrom(outputPort);
            if (!rpipe.moreDocuments()) {
               getLogger().severe("No documents produced on check media pipeline.");
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }

            boolean found = false;
            try {
               XdmNode doc = rpipe.read();
               XdmSequenceIterator seq = doc.axisIterator(Axis.CHILD,YES_NAME);
               found = seq.hasNext();
            } catch (Exception ex) {
               getLogger().severe("Cannot traverse results from check media pipeline.");
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }

            if (found) {
               response.setStatus(Status.CLIENT_ERROR_CONFLICT);
               response.setEntity(new StringRepresentation("Media entry already exists for "+file,MediaType.TEXT_PLAIN));
               return;
            }

         } finally {
            getCache().release(xproc);
         }

         boolean hidden = false;
         String database = getStringAttribute(request,"app.database");
         if (database==null) {
            database = "";
         } else {
            database = database+"/";
         }
         if (isXML) {
            // create the XML media in the collection
            try {
               xproc = getCache().get(createCollectionFeedMedia);
            } catch (Exception ex) {
               getLogger().log(Level.SEVERE,"Cannot compile pipeline: "+createCollectionFeedMedia,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            try {

               InputSource isource = null;
               if (isXML) {
                  try {
                     isource = new InputSource(entity.getReader());
                  } catch (IOException ex) {
                     getLogger().warning("I/O error while getting reader for XML input: "+ex.getMessage());
                     response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                     response.setEntity(new StringRepresentation("I/O error while getting ready to read the client XML."));
                     return;
                  }
               }

               XPipeline pipeline = xproc.getPipeline();
               SAXSource source = new SAXSource(isource);
               DocumentBuilder builder = getCache().getRuntime().getProcessor().newDocumentBuilder();
               try {
                  XdmNode doc = builder.build(source);
                  String port = pipeline.getInputs().iterator().next();
                  pipeline.clearInputs(port);
                  pipeline.writeTo(port,doc);
               } catch (SaxonApiException ex) {
                  getLogger().warning("Syntax error in XML from client: "+ex.getMessage());
                  response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                  response.setEntity(new StringRepresentation("I/O error while reading the client XML."));
                  getCache().release(xproc);
                  return;
               }
               try {
                  isource.getCharacterStream().close();
               } catch (IOException ex) {
                  getLogger().log(Level.WARNING,"I/O exception on close of client stream.",ex);
               }
               
               pipeline.passOption(USER_OPTION, new RuntimeValue(getOptionValue(request,USER_OPTION)));
               pipeline.passOption(PASSWORD_OPTION, new RuntimeValue(getOptionValue(request,PASSWORD_OPTION)));
               pipeline.passOption(HOST_OPTION, new RuntimeValue(getOptionValue(request,HOST_OPTION)));
               pipeline.passOption(PORT_OPTION, new RuntimeValue(getOptionValue(request,PORT_OPTION)));
               pipeline.passOption(NAME_OPTION, new RuntimeValue(collectionName));
               pipeline.passOption(FILE_OPTION, new RuntimeValue(file));
               pipeline.passOption(MEDIA_TYPE_OPTION, new RuntimeValue(mediaType.toString()));

               try {
                  xproc.getPipeline().run();
               } catch (Exception ex) {
                  getLogger().log(Level.SEVERE,"Cannot run pipeline: "+createMediaEntry,ex);
                  response.setStatus(Status.SERVER_ERROR_INTERNAL);
                  return;
               }

               String outputPort = xproc.getPipeline().getOutputs().iterator().next();
               final ReadablePipe rpipe = xproc.getPipeline().readFrom(outputPort);
               if (!rpipe.moreDocuments()) {
                  getLogger().severe("No documents produced on create media entry pipeline.");
                  response.setStatus(Status.SERVER_ERROR_INTERNAL);
                  return;
               }
               boolean ok = false;
               try {
                  XdmNode doc = rpipe.read();
                  XdmSequenceIterator seq = doc.axisIterator(Axis.CHILD,OK_NAME);
                  if (seq.hasNext()) {
                     ok = true;
                  }
               } catch (Exception ex) {
                  getLogger().severe("Cannot traverse results from check media pipeline.");
                  response.setStatus(Status.SERVER_ERROR_INTERNAL);
                  return;
               }
               if (!ok) {
                  getLogger().severe("Cannot insert XML media into collection "+collectionName);
                  response.setStatus(Status.SERVER_ERROR_INTERNAL);
                  response.setEntity(new StringRepresentation("Cannot insert XML media into collection.",MediaType.TEXT_PLAIN));
                  return;
               }
            } finally {
               getCache().release(xproc);
            }
         } else {
            if (getLogger().isLoggable(Level.FINE)) {
               getLogger().fine("Getting collection options for riap://host/"+database+"collections/"+collectionName+".col");
            }
            Request colRequest = new Request(Method.GET,new Reference("riap://host/"+database+"collections/"+collectionName+".col"));
            colRequest.setHostRef(request.getHostRef());
            colRequest.getAttributes().put("org.xproclet.www.identity",request.getAttributes().get("org.xproclet.www.identity"));
            Response colResponse = getContext().getClientDispatcher().handle(colRequest);
            if (colResponse.getStatus().isSuccess()) {
               try {
                  String xml = colResponse.getEntity().getText();
                  if (getLogger().isLoggable(Level.FINE)) {
                     getLogger().fine(xml);
                  }
                  try {
                     DocumentBuilder builder = getCache().getRuntime().getProcessor().newDocumentBuilder();
                     XdmNode doc = builder.build(new StreamSource(new StringReader(xml)));
                     XdmSequenceIterator seq = doc.axisIterator(Axis.CHILD,COLLECTION_NAME);
                     XdmNode collection = (XdmNode)seq.next();
                     XdmSequenceIterator categoryElements = collection.axisIterator(Axis.CHILD,CATEGORY_NAME);
                     while (!hidden && categoryElements.hasNext()) {
                        XdmNode category = (XdmNode)categoryElements.next();
                        String scheme = category.getAttributeValue(SCHEME_NAME);
                        String term = category.getAttributeValue(TERM_NAME);
                        if (!hidden && "http://www.atomojo.org/O/options/media/".equals(scheme) && "hidden".equals(term)) {
                           MediaType checkType = MediaType.valueOf(category.getStringValue());
                           hidden = checkType.includes(mediaType);
                           if (getLogger().isLoggable(Level.FINE)) {
                              getLogger().fine("Checked "+mediaType+" against "+category.getStringValue()+", hidden="+hidden);
                           }
                        }
                     }
                  } catch (SaxonApiException ex) {
                     getLogger().log(Level.SEVERE,"Syntax error in collection XML.",ex);
                     response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                     response.setEntity(new StringRepresentation("I/O error while reading collection XML."));
                     return;
                  }
               } catch (IOException ex) {
                  getLogger().warning("I/O error while getting reader for collection XML input: "+ex.getMessage());
                  response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                  response.setEntity(new StringRepresentation("I/O error while getting ready to read the collection XML."));
                  return;
               }
            }
            if (hidden && getLogger().isLoggable(Level.FINE)) {
               getLogger().fine("Hiding media entry.");
            }
            InputStream data = null;
            try {
               data = entity.getStream();
               MediaMetadata media = mediaStorage.create(request.getAttributes(), collectionName, file, data, mediaType, lastModified);
               if (media==null) {
                  response.setStatus(Status.CLIENT_ERROR_CONFLICT);
                  response.setEntity(new StringRepresentation("Media entry already exists for "+file,MediaType.TEXT_PLAIN));
                  return;
               }
            } catch (IOException ex) {
               getLogger().log(Level.SEVERE,"I/O exception while storing media.",ex);
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
         }

         // create the entry in the feed
         try {
            xproc = getCache().get(createMediaEntry);
         } catch (Exception ex) {
            getLogger().log(Level.SEVERE,"Cannot compile pipeline: "+createMediaEntry,ex);
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            try {
               mediaStorage.delete(request.getAttributes(), collectionName, file);
            } catch (IOException dex) {
               getLogger().log(Level.SEVERE,"Cannot delete file on exception handling.",dex);
            }
            return;
         }
         try {

            xproc.getPipeline().passOption(USER_OPTION, new RuntimeValue(getOptionValue(request,USER_OPTION)));
            xproc.getPipeline().passOption(PASSWORD_OPTION, new RuntimeValue(getOptionValue(request,PASSWORD_OPTION)));
            xproc.getPipeline().passOption(HOST_OPTION, new RuntimeValue(getOptionValue(request,HOST_OPTION)));
            xproc.getPipeline().passOption(PORT_OPTION, new RuntimeValue(getOptionValue(request,PORT_OPTION)));
            xproc.getPipeline().passOption(NAME_OPTION, new RuntimeValue(collectionName));
            xproc.getPipeline().passOption(FILE_OPTION, new RuntimeValue(file));
            xproc.getPipeline().passOption(HIDDEN_OPTION, new RuntimeValue(hidden ? "true" : "false"));
            xproc.getPipeline().passOption(MEDIA_TYPE_OPTION, new RuntimeValue(mediaType.toString()));

            try {
               xproc.getPipeline().run();
            } catch (Exception ex) {
               getLogger().log(Level.SEVERE,"Cannot run pipeline: "+createMediaEntry,ex);
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }

            String outputPort = xproc.getPipeline().getOutputs().iterator().next();
            final ReadablePipe rpipe = xproc.getPipeline().readFrom(outputPort);
            if (!rpipe.moreDocuments()) {
               getLogger().severe("No documents produced on create media entry pipeline.");
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            String id = null;
            try {
               XdmNode doc = rpipe.read();
               XdmSequenceIterator seq = doc.axisIterator(Axis.CHILD,ID_NAME);
               if (seq.hasNext()) {
                  id = seq.next().getStringValue();
               }
            } catch (Exception ex) {
               getLogger().severe("Cannot traverse results from check media pipeline.");
               response.setStatus(Status.SERVER_ERROR_INTERNAL);
               return;
            }
            if (id!=null) {
               response.setLocationRef("/collections/"+collectionName+"/_/"+id+".atom");
            }
            
            Request entryRequest = new Request(Method.GET,new Reference("riap://host/"+database+"collections/"+collectionName+"/_/"+id+".atom"));
            entryRequest.setHostRef(request.getHostRef());
            entryRequest.getAttributes().put("org.xproclet.www.identity",request.getAttributes().get("org.xproclet.www.identity"));
            Response entryResponse = getContext().getClientDispatcher().handle(entryRequest);
            getLogger().info("Entry response: "+entryResponse.getStatus());
            if (entryResponse.getStatus().isSuccess()) {
               response.setEntity(entryResponse.getEntity());
               response.getEntity().setLocationRef((Reference)null);
            }
            response.setStatus(Status.SUCCESS_CREATED);
         } finally {
            getCache().release(xproc);
         }
      } else {
         super.handle(request, response);
      }
   }
}
