/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import com.xmlcalabash.core.XProcConfiguration;
import com.xmlcalabash.core.XProcConstants;
import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.core.XProcRuntime;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.Input;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import net.sf.saxon.s9api.SaxonApiException;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class XProcCache {

   static Logger LOG = Logger.getLogger(XProcCache.class.getName());
   public static String ATTR = "xproc.cache";

   static class PipelineInfo {
      ConcurrentLinkedQueue<XPipeline> compiled;
      long timestamp;
      URI location;
      File checkFile;
      PipelineInfo(URI location) {
         this.location = location;
         compiled = new ConcurrentLinkedQueue<XPipeline>();
         timestamp = -1;
         checkFile = null;
         if (location.getScheme().equals("file")) {
            // check file date
            checkFile = new File(location.getSchemeSpecificPart());
         } else if (location.getScheme().equals("jar")) {
            String part = location.getSchemeSpecificPart();
            int bang = part.indexOf('!');
            if (bang>0) {
               URI uri = URI.create(part.substring(0,bang));
               if (uri.getScheme().equals("file")) {
                  checkFile = new File(uri.getSchemeSpecificPart());
               }
            }
         }
      }
   }
   
   protected Context context;
   protected Map<URI,PipelineInfo> instances;
   protected XProcConfiguration config;
   protected XProcRuntime runtime;
   
   public XProcCache(Context confContext) {
      context = confContext;
      instances = new TreeMap<URI,PipelineInfo>();
      config = new XProcConfiguration();
      runtime = new XProcRuntime(config);
      runtime.setEntityResolver(new EntityResolver() {
         public InputSource resolveEntity (String publicId, String systemId) 
            throws IOException
         {
            Reference resourceRef = new Reference(systemId);
            final boolean clientMemoryHack = resourceRef.getSchemeProtocol().equals(Protocol.HTTPS) || resourceRef.getSchemeProtocol().equals(Protocol.HTTPS);
            final Restlet client = clientMemoryHack ?  new Client(context.createChildContext(),resourceRef.getSchemeProtocol()) : context.getClientDispatcher();
            Request request = new Request(Method.GET,resourceRef);
            Response response = client.handle(request);
            if (response.getStatus().isSuccess()) {
               InputSource source = null;
               final Representation representation = response.getEntity();
               if (representation!=null) {
                  try {
                     String charset = representation.getMediaType().getParameters().getFirstValue("charset");
                     final Reader rawReader = new InputStreamReader(representation.getStream(),charset==null ? "UTF-8" : charset);
                     source = new InputSource(new BufferedReader(rawReader) {
                        public void close() 
                           throws IOException
                        {
                           rawReader.close();
                           representation.release();
                           if (clientMemoryHack) {
                              try {
                                 client.stop();
                              } catch (Exception ex) {
                                 context.getLogger().log(Level.SEVERE,"Cannot stop client.",ex);
                              }
                           }
                        }
                     });
                  } catch (IOException ex) {
                     if (clientMemoryHack) {
                        try {
                           client.stop();
                        } catch (Exception ioex) {
                           context.getLogger().log(Level.SEVERE,"Cannot stop client.",ioex);
                        }
                     }
                     throw ex;
                  }
               } else {
                  source = new InputSource(new StringReader(""));
               }
               source.setSystemId(systemId);
               source.setPublicId(publicId);
               return source;
            } else {
               throw new FileNotFoundException("Cannot open resource "+resourceRef+", status "+response.getStatus());
            }
         }
      });
      runtime.setURIResolver(new URIResolver() {
         public Source resolve(String href,String base)
            throws TransformerException
         {
            Reference resourceRef = null;
            if (base!=null) {
               try {
                  URI baseURI = new URI(base);
                  resourceRef = new Reference(baseURI.resolve(href).toString());
               } catch (URISyntaxException ex) {
                  throw new TransformerException("Cannot resolve "+href+" against "+base,ex);
               }
            } else {
               resourceRef = new Reference(href);
            }
            final boolean clientMemoryHack = resourceRef.getSchemeProtocol().equals(Protocol.HTTPS) || resourceRef.getSchemeProtocol().equals(Protocol.HTTPS);
            final Restlet client = clientMemoryHack ?  new Client(context.createChildContext(),resourceRef.getSchemeProtocol()) : context.getClientDispatcher();
            boolean sent = false;
            try {
               Request request = new Request(Method.GET,resourceRef);
               Response response = client.handle(request);
               InputSource source = null;
               if (response.getStatus().isSuccess()) {
                  final Representation representation = response.getEntity();
                  if (representation!=null) {
                     try {
                        String charset = representation.getMediaType().getParameters().getFirstValue("charset");
                        final Reader rawReader = new InputStreamReader(representation.getStream(),charset==null ? "UTF-8" : charset);
                        sent = true;
                        return new StreamSource(new BufferedReader(rawReader) {
                           public void close() 
                              throws IOException
                           {
                              rawReader.close();
                              representation.release();
                              if (clientMemoryHack) {
                                 try {
                                    client.stop();
                                 } catch (Exception ex) {
                                    context.getLogger().log(Level.SEVERE,"Cannot stop client.",ex);
                                 }
                              }
                           }
                        },resourceRef.toString());
                     } catch (IOException ex) {
                        representation.release();
                        throw new TransformerException("Cannot open resource "+resourceRef,ex);
                     }
                  }
               } else {
                  throw new XProcException(XProcConstants.dynamicError(11),"Cannot open resource "+resourceRef+", status "+response.getStatus());
               }
            } finally {
               if (!sent && clientMemoryHack) {
                  try {
                     client.stop();
                  } catch (Exception ex) {
                     context.getLogger().log(Level.SEVERE,"Cannot stop client.",ex);
                  }
               }
            }
            return null;
         }
      });
   }
   
   public XProcRuntime getRuntime() {
      return runtime;
   }
   
   public XPipelineContext get(URI location) 
      throws SaxonApiException
   {
      PipelineInfo info = null;
      synchronized (instances) {
         info = instances.get(location);
         if (info==null) {
            info = new PipelineInfo(location);
            instances.put(location,info);
         }
      }
      synchronized (info) {
         if (info.timestamp>0 && info.checkFile!=null) {
            LOG.info("Checking file: "+info.checkFile+" "+info.checkFile.lastModified()+" vs "+info.timestamp);
            if (info.checkFile.lastModified()>info.timestamp) {
               LOG.info("Pipeline modified, clearing: "+info.location);
               info.compiled.clear();
            }
         }
         if (info.compiled.size()>0) {
            XPipeline pipeline = info.compiled.remove();
            LOG.info(this+" Compiled XPipeline: "+pipeline+" , thread: "+Thread.currentThread()+", "+info.location);
            return new XPipelineContext(info.location,pipeline,info.timestamp);
         }
      }
      synchronized (runtime) {
         Input input = new Input(info.location.toString());
         XPipeline pipeline = runtime.load(input);
         LOG.info(this+" New XPipeline: "+pipeline+" , thread: "+Thread.currentThread()+", "+info.location);
         if (info.timestamp<0) {
            synchronized (info) {
               info.timestamp = System.currentTimeMillis();
            }
         }
         return new XPipelineContext(info.location,pipeline,info.timestamp);
      }
   }
   
   public void release(XPipelineContext context)
   {
      LOG.info(this+" Releasing: "+context.pipeline+" , thread: "+Thread.currentThread()+", "+context.location);
      context.getPipeline().reset();
      
      PipelineInfo info = null;
      synchronized (instances) {
         info = instances.get(context.getLocation());
         if (info==null) {
            return;
         }
      }
      synchronized (info) {
         if (info.timestamp==context.getTimestamp()) {
            info.compiled.add(context.getPipeline());
         }
      }
   }
   
   public void clear(URI location)
   {
      List<XPipeline> compiled = null;
      synchronized (instances) {
         instances.remove(location);
      }
   }
   
   public void clear()
   {
      instances.clear();
   }
}
