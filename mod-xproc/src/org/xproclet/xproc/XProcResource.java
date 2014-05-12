/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XPipeline;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class XProcResource extends ServerResource {
   
   public static final String CONTEXT_ATTR = "xproc.load.context";
   public static final String HREF_PARAM = "xproc.href";
   public static final String TMPDIR_PARAM = "xproc.tmpdir";
   public static final String LOAD_TYPE_PARAM = "xproc.load.type";
   public static final String OPTION_NAMES_PARAM = "xproc.option.names";
   public static final String OPTION_HEADER_NAMES_PARAM = "xproc.option.header.names";
   XProcCache cache;
   URI pipelineRef;
   List<QName> options;
   List<QName> headersToBind;
   public XProcResource() {
      setNegotiated(false);
      cache = null;
      pipelineRef = null;
      options = null;
   }
   
   protected String getOptionValue(QName name)
   {
      String key = name.getLocalName();
      if (name.getNamespaceURI()!=null) {
         key = name.getClarkName();
      }
      Object o = getRequest().getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      o = getContext().getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      return getContext().getParameters().getFirstValue(key);
   }
   
   protected void doInit() {
      try {
         cache = (XProcCache)getContext().getAttributes().get(XProcCache.ATTR);
         if (cache==null) {
            getLogger().warning("No cache "+XProcCache.ATTR+" attribute was found for caching xproc pipeline instances.");
         }
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot retrieve cache from context.",ex);
      }
      
      String href = getContext().getParameters().getFirstValue(HREF_PARAM);
      if (href!=null) {
         try {
            if ("context".equals(getContext().getParameters().getFirstValue(LOAD_TYPE_PARAM))) {
               Object contextObj = getContext().getAttributes().get(CONTEXT_ATTR);
               if (contextObj!=null) {
                  URL pipelineURL = contextObj.getClass().getResource(href);
                  if (pipelineURL!=null) {
                     pipelineRef = pipelineURL.toURI();
                  } else {
                     getLogger().info("Cannot find resource via context: "+href);
                  }
               } else {
                  getLogger().info("No context object specified for attribute "+CONTEXT_ATTR);
               }
            } else {
               pipelineRef = new URI(href);
            }
            getLogger().info("Pipeline: "+pipelineRef);
         } catch (URISyntaxException ex) {
            getLogger().log(Level.SEVERE,"Cannot instantiate pipeline URI reference: "+href,ex);
         }
      } else {
         getLogger().warning("No xproc.href parameter was specified.");
      }
      String optionNames = getContext().getParameters().getFirstValue(OPTION_NAMES_PARAM);
      if (optionNames!=null) {
         String [] names = optionNames.split(",");
         for (int i=0; i<names.length; i++) {
            QName name = QName.fromClarkName(names[i].trim());
            if (options==null) {
               options = new ArrayList<QName>();
            }
            options.add(name);
         }
      }
      headersToBind = null;
      optionNames = getContext().getParameters().getFirstValue(XProcResource.OPTION_HEADER_NAMES_PARAM);
      if (optionNames!=null) {
         String [] names = optionNames.split(",");
         for (int i=0; i<names.length; i++) {
            QName name = QName.fromClarkName(names[i].trim());
            if (headersToBind==null) {
               headersToBind = new ArrayList<QName>();
            }
            headersToBind.add(name);
         }
      }
   }
   
   protected Representation doHandle() {
      if (cache==null) {
         getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         return null;
      }
      if (pipelineRef==null) {
         getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         return null;
      }
      XPipelineContext cachedXProc = null;
      try {
         cachedXProc = cache.get(pipelineRef);
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot load pipeline.",ex);
         getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
         return null;
      }
      final XPipelineContext xproc = cachedXProc;
      XPipeline pipeline = xproc.getPipeline();
         
      if (getRequest().isEntityAvailable() && pipeline.getInputs().size()==1) {
         Representation entity = getRequest().getEntity();
         MediaType mediaType = entity.getMediaType();
         String xml = null;
         if (MediaType.APPLICATION_ALL_XML.includes(mediaType) || MediaType.TEXT_XML.equals(mediaType)) {
            try {
               xml = entity.getText();
            } catch (IOException ex) {
               getLogger().info("I/O exception while reading from client.");
               getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               cache.release(xproc);
               return null;
            }
         } else if (mediaType.equals(MediaType.APPLICATION_WWW_FORM)) {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append("<form>\n");
            Form form = new Form(entity);
            for (String name : form.getNames()) {
               String value = form.getValues(name);
               xmlBuilder.append("<input name=\"");
               xmlBuilder.append(name.replace("\"", "&quot;"));
               xmlBuilder.append("\" value=\"");
               xmlBuilder.append(value.replace("\"", "&quot;"));
               xmlBuilder.append("\"/>\n");
            }
            xmlBuilder.append("</form>");
            xml = xmlBuilder.toString();
         }
         SAXSource source = new SAXSource(new InputSource(new StringReader(xml)));
         DocumentBuilder builder = cache.getRuntime().getProcessor().newDocumentBuilder();
         try {
            XdmNode doc = builder.build(source);
            String port = pipeline.getInputs().iterator().next();
            pipeline.clearInputs(port);
            pipeline.writeTo(port,doc);
         } catch (SaxonApiException ex) {
            getLogger().info("Syntax error in XML from client: "+ex.getMessage());
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return null;
         }


      } else if (getRequest().isEntityAvailable() || pipeline.getInputs().size()!=0) {
         // Note: excluded case of no entity and no input ports
         if (pipeline.getInputs().size()==1) {
            getLogger().severe("Required input not provided to pipeline: "+xproc.location+", port="+xproc.getPipeline().getInputs());
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return null;
         } else if (pipeline.getInputs().size()==0) {
            getLogger().severe("Input not allowed on pipeline: "+xproc.location);
            getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return null;
         }
         // unsupported
         getLogger().severe("Input port mismatch: "+xproc.location+", ports="+xproc.getPipeline().getInputs());
         getResponse().setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         cache.release(xproc);
         return null;
      }
      //for (QName optionName : pipeline.getOptions()) {
      if (options!=null) {
         for (QName optionName : options) {
            getLogger().fine("Setting option: "+optionName);
            String value = getOptionValue(optionName);
            if (value!=null) {
               getLogger().fine("Option: "+optionName+"="+value);
               pipeline.passOption(optionName, new RuntimeValue(value));
            }
         }
      }
      Series<Header> headers = (Series<Header>)getRequest().getAttributes().get("org.restlet.http.headers");
      if (headersToBind!=null) {
         for (QName optionName : headersToBind) {
            getLogger().fine("Getting header "+optionName+" value.");
            String value = headers.getFirstValue(optionName.getLocalName());
            if (value!=null) {
               getLogger().fine("Option: "+optionName+"="+value);
               pipeline.passOption(optionName, new RuntimeValue(value));
            }
         }
      }
      
      try {
         pipeline.run();
         if (xproc.getPipeline().getOutputs().size()==1) {
            final String outputPort = xproc.getPipeline().getOutputs().iterator().next();
            getResponse().setStatus(Status.SUCCESS_OK);
            return new OutputRepresentation(MediaType.APPLICATION_XML) {
               public void write(OutputStream out) 
                  throws IOException
               {
                  Serialization serial = xproc.getPipeline().getSerialization(outputPort);               
                  WritableDocument doc = new WritableDocument(cache.getRuntime(),null,serial,out);
                  ReadablePipe rpipe = xproc.getPipeline().readFrom(outputPort);
                  while (rpipe.moreDocuments()) {
                     try {
                        doc.write(rpipe.read());
                     } catch (SaxonApiException ex) {
                        throw new IOException("Exception while writing output port "+outputPort,ex);
                     }
                  }
                  out.flush();
               }
               public void release() {
                  cache.release(xproc);
               }
            };
         } else {
            cache.release(xproc);
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            return null;
         }
      } catch (SaxonApiException ex) {
         getLogger().info("Error running pipeline: "+ex.getMessage());
         getResponse().setStatus(Status.SERVER_ERROR_INTERNAL);
         return null;
      }
   }
}
