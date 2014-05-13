/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.atompub.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.Uniform;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Cookie;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.representation.InputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.util.Resolver;
import org.restlet.util.Series;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class UploadApplication extends Application {

   static Logger LOG = Logger.getLogger(UploadApplication.class.getName());
   
   static DocumentBuilder docBuilder = null;
   static {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      try {
         docBuilder = factory.newDocumentBuilder();
      } catch (Exception ex) {
         LOG.log(Level.SEVERE,"Cannot configure document builder.",ex);
      }
   }
   
   static long ONE_DAY = 24*3600*1000;
   
   static class UploadProgress {
      long timestamp;
      String id;
      String submit;
      String cancel;
      String entryId;
      int status;
      boolean update;
      long length;
      UploadSource source;
      UploadActor actor;
      boolean cancelled;
      UploadProgress(String id,boolean update,String submit,String cancel) {
         this.id = id;
         this.update = update;
         this.submit = submit;
         this.cancel = cancel;
         this.length = 0;
         this.timestamp = System.currentTimeMillis();
         this.source = null;
         this.status = -1;
         this.entryId = null;
         this.actor = null;
         this.cancelled = false;
      }
      public void cancel() {
         this.cancelled = true;
         if (this.actor!=null) {
            this.actor.cancel();
         }
      }
   }
   
   interface UploadActor {
      void cancel();
   }
   

   static String toString(Reader r)
      throws IOException
   {
      StringBuilder builder = new StringBuilder();
      char [] buffer = new char[1024];
      int len;
      while ((len=r.read(buffer))>0) {
         builder.append(buffer,0,len);
      }
      return builder.toString();
   }

   static public class StartUpload extends ServerResource {
      public StartUpload()
      {
         setNegotiated(false);
      }
      public Representation post(Representation entity) {
         Form form = new Form(entity);
         String submit = form.getFirstValue("submit");
         String cancel = form.getFirstValue("cancel");
         String update = form.getFirstValue("update");
         if (submit==null) {
            submit = "Upload";
         }
         if (cancel==null) {
            cancel = "Cancel";
         }
         UploadApplication app = (UploadApplication)getContext().getAttributes().get("upload.app");
         UploadProgress progress = app.newUpload("true".equals(update),submit,cancel);
         getResponse().setStatus(Status.SUCCESS_CREATED);
         getResponse().setLocationRef(new Reference(getRequest().getResourceRef().getParentRef().toString()+progress.id+"/"));
         return new StringRepresentation("<context id='"+progress.id+"'/>",MediaType.APPLICATION_XML);
      }
   }
   static public class UploadForm extends ServerResource {
      public UploadForm()
      {
         setNegotiated(false);
      }
      
      public Representation get() {
         UploadApplication app = (UploadApplication)getContext().getAttributes().get("upload.app");
         String id = getRequest().getAttributes().get("upload.id").toString();
         final UploadProgress progress = app.getUpload(id);
         if (progress==null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return null;
         }
         try {
            InputStream is = UploadApplication.class.getResourceAsStream("upload.html");
            Reader r = new InputStreamReader(is,"UTF-8");
            Template template = new Template(UploadApplication.toString(r));
            String result = template.format(new Resolver<String>() {
               public String resolve(String name) {
                  if (name.equals("action")) {
                     return getRequest().getResourceRef().getPath();
                  } else if (name.equals("submit")) {
                     return progress.submit;
                  } else if (name.equals("cancel")) {
                     return progress.cancel;
                  } else {
                     return "";
                  }
               }
            });
            getResponse().setStatus(Status.SUCCESS_OK);
            return new StringRepresentation(result,MediaType.TEXT_HTML);
         } catch (IOException ex) {
            getLogger().log(Level.SEVERE,"Cannot get upload.html resource.",ex);
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
            return null;
         }
         
      }
      public Representation post(Representation entity) {
         UploadApplication app = (UploadApplication)getContext().getAttributes().get("upload.app");
         String id = getRequest().getAttributes().get("upload.id").toString();
         String baseURL = getRequest().getResourceRef().getBaseRef().getParentRef().getParentRef().getParentRef().getParentRef().toString();
         String appUsername = app.getAttribute(getRequest(),"app.username");
         String appPassword = app.getAttribute(getRequest(),"app.password");
         final UploadProgress progress = app.getUpload(id);
         if (progress==null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return null;
         }
         progress.length = entity.getSize();
         getLogger().info("Upload Size: "+progress.length);

         InputStream is = null;
         final AtomicBoolean cancellation = new AtomicBoolean(false);
         Reference appRef = new Reference(baseURL+getRequest().getResourceRef().getRemainingPart());
         getLogger().info("Upload to: "+appRef);
         try {
            progress.source = new UploadSource(getLogger(),entity.getStream());
            progress.actor = new UploadActor() {
               public void cancel() {
                  cancellation.set(true);
                  progress.source.cancel();
               }
            };
            Map<String,String> headers = progress.source.setup();
            Map<String,String> disposition = UploadSource.parseParameters(headers.get("Content-Disposition"));
            String filename = disposition.get("filename");
            is = progress.source.getInputStream();

            Uniform client = getContext().getClientDispatcher();
            //Client client = new Client(getContext().createChildContext(),appRef.getSchemeProtocol());
            Request appRequest = new Request(progress.update ? Method.PUT : Method.POST,appRef);
            appRequest.setRootRef(getRequest().getRootRef());
            
            if (appUsername!=null) {
               getLogger().info("Using username "+appUsername+" for authentication for APP target.");
               appRequest.setChallengeResponse(new ChallengeResponse(ChallengeScheme.HTTP_BASIC,appUsername,appPassword));
            } else if (getRequest().getChallengeResponse()!=null) {
                // pass along authentication
               getLogger().info("Passing along authentication to APP target.");
               appRequest.setChallengeResponse(getRequest().getChallengeResponse());
            }
            Cookie cookie = getRequest().getCookies().getFirst("I");
            if (cookie!=null) {
               appRequest.getCookies().add(cookie);
            }
            
            // add header for slug if new upload
            if (!progress.update && filename!=null) {
               getLogger().info("Upload filename: "+filename);
               Series<Header> appHeaders = new Series<Header>(Header.class);
               appHeaders.add("Slug",filename);
               appRequest.getAttributes().put("org.restlet.http.headers",appHeaders);
            }
            
            // setup body
            appRequest.setEntity(new InputRepresentation(is,MediaType.valueOf(headers.get("Content-Type"))));
            
            // make request
            Response appResponse = new Response(appRequest);
            client.handle(appRequest,appResponse);

            if (appResponse.getStatus().isSuccess()) {
               if (progress.update) {
                  progress.status = appResponse.getStatus().getCode();
               } else {
                  try {
                     Reader r = appResponse.getEntity().getReader();
                     Document entryDoc = docBuilder.parse(new InputSource(r));
                     NodeList ids = entryDoc.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2005/Atom", "id");
                     progress.entryId = ids.getLength()>0 ? ids.item(0).getTextContent() : null;
                     progress.status = appResponse.getStatus().getCode();
                     getLogger().info("Entry: "+progress.entryId+", status="+progress.status);
                  } catch (Exception ex) {
                     getLogger().log(Level.SEVERE,"Cannot parse response to upload to "+appRef,ex);
                     progress.status = appResponse.getStatus().getCode();
                  }
               }
            } else {
               if (cancellation.get()) {
                  progress.status = 0;
                  getLogger().severe("Upload to "+appRef+" cancelled.");
               } else {
                  progress.status = appResponse.getStatus().getCode();
                  getLogger().severe("Upload to "+appRef+" failed with status "+appResponse.getStatus().getCode());
               }
            }
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
         } catch (IOException ex) {
            if (cancellation.get()) {
               getLogger().info("Upload to "+appRef+" cancelled by user.");
               getResponse().setStatus(Status.SUCCESS_PARTIAL_CONTENT);
            } else {
               getLogger().warning("I/O error during upload, canceling: "+ex.getMessage());
            }
            // forget the error
         } finally {
            if (is!=null) {
               try {
                  is.close();
               } catch (IOException ex) {
               }
            }
         }
         return null;
      }
   }
   
   static public class UploadStatus extends ServerResource {
      public UploadStatus()
      {
         setNegotiated(false);
      }
      public Representation get() {
         UploadApplication app = (UploadApplication)getContext().getAttributes().get("upload.app");
         String id = getRequest().getAttributes().get("upload.id").toString();
         UploadProgress progress = app.getUpload(id);
         if (progress==null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return null;
         }
         Representation rep = null;
         if (progress.cancelled) {
            app.removeUpload(progress.id);
            rep = new StringRepresentation("<status id='"+progress.id+"' cancelled='true' status='0' size='"+progress.length+"' progress='"+(progress.source==null ? 0 : progress.source.getBytesRead())+"'/>",MediaType.APPLICATION_XML);
         } else if (progress.status>0) {
            app.removeUpload(progress.id);
            rep = new StringRepresentation("<status id='"+progress.id+"' status='"+progress.status+"'"+(progress.entryId==null ? "" : " entry-id='"+progress.entryId+"'")+" size='"+progress.length+"' progress='"+(progress.source==null ? 0 : progress.source.getBytesRead())+"'/>",MediaType.APPLICATION_XML);
         } else {
            rep = new StringRepresentation("<status id='"+progress.id+"' size='"+progress.length+"' progress='"+(progress.source==null ? 0 : progress.source.getBytesRead())+"'/>",MediaType.APPLICATION_XML);
         }
         rep.setTransient(true);
         rep.setExpirationDate(new Date(0));
         Series<Header> headers = new Series<Header>(Header.class);
         headers.add("Cache-Control","no-cache");
         headers.add("Pragma","no-cache");
         getResponse().getAttributes().put("org.restlet.http.headers",headers);
         return rep;
      }
   }
   
   static public class UploadCancel extends ServerResource {
      public UploadCancel()
      {
         setNegotiated(false);
      }
      public Representation get() {
         UploadApplication app = (UploadApplication)getContext().getAttributes().get("upload.app");
         String id = getRequest().getAttributes().get("upload.id").toString();
         UploadProgress progress = app.getUpload(id);
         if (progress==null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return null;
         }
         progress.cancel();
         getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
         return null;
      }
   }

   Map<String,UploadProgress> inProgress = new TreeMap<String,UploadProgress>();
   
   public UploadApplication(Context context) {
      this(context,null);
   }

   public UploadApplication(Context context, Restlet appProxy) {
      super(context.createChildContext());
      getContext().getAttributes().put("upload.app", this);
      getTunnelService().setEnabled(false);

      /*
      for (String name : context.getParameters().getNames()) {
         String value = context.getParameters().getFirstValue(name);
         getLogger().info(UploadApplication.class+" parameter "+name+" -> "+value);
         getContext().getParameters().set(name,value,false);
      }*/
   }

   public String getAttribute(Request request,String name) {
      Object requestBase = request.getAttributes().get(name);
      return requestBase==null ? getContext().getParameters().getFirstValue(name) : requestBase.toString();
   }
   
   public UploadProgress newUpload(boolean update,String submit,String cancel)
   {
      String id = UUID.randomUUID().toString();
      UploadProgress progress = new UploadProgress(id,update,submit,cancel);
      inProgress.put(id,progress);
      cleanup();
      return progress;
   }
   
   public UploadProgress getUpload(String id)
   {
      return inProgress.get(id);
   }
   
   public UploadProgress removeUpload(String id)
   {
      return inProgress.remove(id);
   }
   
   public void cleanup() {
      Set<String> keys = new TreeSet<String>();
      keys.addAll(inProgress.keySet());
      for (String id : keys) {
         UploadProgress upload = inProgress.get(id);
         if ((System.currentTimeMillis()-upload.timestamp)>=ONE_DAY) {
            inProgress.remove(id);
         }
      }
   }
   
   public Restlet createInboundRoot() {
      Router router = new Router(getContext());
      router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
      router.attach("/start/",StartUpload.class);
      router.attach("/{upload.id}/upload/",UploadForm.class);
      router.attach("/{upload.id}/status/",UploadStatus.class);
      router.attach("/{upload.id}/cancel/",UploadCancel.class);
      return router;
   }
}
