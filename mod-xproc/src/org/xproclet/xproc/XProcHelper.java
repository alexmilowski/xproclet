/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import com.xmlcalabash.core.XProcException;
import com.xmlcalabash.io.ReadablePipe;
import com.xmlcalabash.io.WritableDocument;
import com.xmlcalabash.model.RuntimeValue;
import com.xmlcalabash.model.Serialization;
import com.xmlcalabash.runtime.XInput;
import com.xmlcalabash.runtime.XPipeline;
import com.xmlcalabash.util.S9apiUtils;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.sax.SAXSource;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
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
import org.restlet.representation.OutputRepresentation;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class XProcHelper {

   final static String NS = "http://www.xproclet.org/V/XProc/";
   final static String HTTP_NS = "http://www.xproclet.org/V/HTTP/";
   final static QName HTTP_NAME = QName.fromClarkName("{"+HTTP_NS+"}http");
   final static QName HEADER_NAME = QName.fromClarkName("{"+HTTP_NS+"}header");
   final static QName ENTITY_NAME = QName.fromClarkName("{"+HTTP_NS+"}entity");
   final static QName ATTRIBUTE_NAME = QName.fromClarkName("{"+HTTP_NS+"}attribute");
   final static QName STATUS_NAME = QName.fromClarkName("status");
   final static QName NAME_NAME = QName.fromClarkName("name");
   final static QName TYPE_NAME = QName.fromClarkName("type");
   final static QName VALUE_NAME = QName.fromClarkName("value");
   final static QName LAST_MODIFIED_NAME = QName.fromClarkName("last-modified");
   public final static String XPROC_CONFIG_ATTR = "xproc.configuration";
   final static SimpleDateFormat xsdDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
   final static SimpleDateFormat xsdDateFormatWithMilliseconds = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
   
   static class OptionBinding {
      enum Source {
         ATTRIBUTES,
         HEADERS,
         PARAMETERS,
         QUERY,
         REQUEST
      }
      QName optionName;
      Source source;
      String name;
      String defaultValue;
      OptionBinding(QName optionName,Source source,String name,String defaultValue)
      {
         this.optionName = optionName;
         this.name = name;
         this.source = source;
         this.defaultValue = defaultValue;
      }
   }
   static class PipeInfo {
      enum QueryBind {
         NONE,
         PARAMETERS,
         OPTIONS
      }
      URI location;
      boolean bindResult;
      QueryBind bindQuery;
      List<OptionBinding> optionBindings;
      /*
      Map<String,QName> parametersToBind;
      Map<String,QName> headersToBind;
      Map<String,QName> requestsToBind;
      Map<String,QName> attrsToBind;
       */
      Map<String,String> optionValues;
      List<MediaType> requiredTypes;
      MediaType outputType;
      PipeInfo(URI location) {
         this.location = location;
         /*
         this.parametersToBind = null;
         this.optionValues = null;
         this.headersToBind = null;
         this.requestsToBind = null;
         this.attrsToBind = null;
          */
         this.optionBindings = null;
         this.bindResult = false;
         this.bindQuery = QueryBind.NONE;
         this.requiredTypes = new ArrayList<MediaType>();
         this.outputType = MediaType.valueOf(MediaType.APPLICATION_XML+";charset=UTF-8");
      }
      void bindOptionToParameter(String parameterName,QName name,String defaultValue) {
         if (optionBindings==null) {
            optionBindings = new ArrayList<OptionBinding>();
         }
         optionBindings.add(new OptionBinding(name,OptionBinding.Source.PARAMETERS,parameterName,defaultValue));
         /*
         if (parametersToBind==null) {
            parametersToBind = new TreeMap<String,QName>();
         }
         parametersToBind.put(parameterName,name);
          * 
          */
      }
      void bindOptionToHeader(String header,QName name,String defaultValue) {
         if (optionBindings==null) {
            optionBindings = new ArrayList<OptionBinding>();
         }
         optionBindings.add(new OptionBinding(name,OptionBinding.Source.HEADERS,header,defaultValue));
         /*
         if (headersToBind==null) {
            headersToBind = new TreeMap<String,QName>();
         }
         headersToBind.put(header,name);
          * 
          */
      }
      void bindOption(String name,String value) {
         if (optionValues==null) {
            optionValues = new TreeMap<String,String>();
         }
         optionValues.put(name, value);
      }
      void bindOptionToRequest(String part,QName name,String defaultValue) {
         if (optionBindings==null) {
            optionBindings = new ArrayList<OptionBinding>();
         }
         optionBindings.add(new OptionBinding(name,OptionBinding.Source.REQUEST,part,defaultValue));
         /*
         if (requestsToBind==null) {
            requestsToBind = new TreeMap<String,QName>();
         }
         requestsToBind.put(part,name);
          * 
          */
      }
      void bindOptionToAttribute(String attrName,QName name,String defaultValue) {
         if (optionBindings==null) {
            optionBindings = new ArrayList<OptionBinding>();
         }
         optionBindings.add(new OptionBinding(name,OptionBinding.Source.ATTRIBUTES,attrName,defaultValue));
         /*
         if (attrsToBind==null) {
            attrsToBind = new TreeMap<String,QName>();
         }
         attrsToBind.put(attrName,name);
          * 
          */
      }
      void bindOptionToQuery(String parameterName,QName name,String defaultValue)
      {
         if (optionBindings==null) {
            optionBindings = new ArrayList<OptionBinding>();
         }
         optionBindings.add(new OptionBinding(name,OptionBinding.Source.QUERY,parameterName,defaultValue));
      }
   }
   Context context;
   Map<Method,PipeInfo> methodPipelines;
   protected XProcCache cache;
   List<QName> optionsToBind;
   Map<String,QName> headersToBind;
   Object contextObject;
   
   static boolean equalsName(Element e,String name)
   {
      return NS.equals(e.getNamespaceURI()) && name.equals(e.getLocalName());
   }
      
   public XProcHelper(Context context) {
      this.context = context;
      try {
         cache = (XProcCache)getContext().getAttributes().get(XProcCache.ATTR);
         if (cache==null) {
            getLogger().warning("No cache "+XProcCache.ATTR+" attribute was found for caching xproc pipeline instances.");
         }
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot retrieve cache from context.",ex);
      }
      this.methodPipelines = new TreeMap<Method,PipeInfo>();
      contextObject = null;
      if ("context".equals(getContext().getParameters().getFirstValue(XProcResource.LOAD_TYPE_PARAM))) {
         contextObject = getContext().getAttributes().get(XProcResource.CONTEXT_ATTR);
      }
      Object o = context.getAttributes().get(XPROC_CONFIG_ATTR);
      if (o instanceof List) {
         List<Object> configuration = (List<Object>)o;
         for (Object item : configuration) {
            if (item instanceof Document) {
               Document methodDoc = (Document)item;
               Element top = methodDoc.getDocumentElement();
               if (equalsName(top,"method")) {
                  String methodName = top.getAttribute("name");
                  String href = top.getAttribute("href");
                  if (methodName!=null && href!=null) {
                     Method method = Method.valueOf(methodName.trim().toUpperCase());
                     try {
                        URI pipeline = resolve(top.getBaseURI()==null ? null : new URI(top.getBaseURI()),href);
                        if (pipeline==null) {
                           continue;
                        }
                        PipeInfo info = new PipeInfo(pipeline);
                        if ("true".equals(top.getAttribute("bind-output"))) {
                           getLogger().fine("Binding result for "+method+" on "+pipeline);
                           info.bindResult = true;
                        }
                        String bindQuery = top.getAttribute("bind-query");
                        if ("options".equals(bindQuery)) {
                           info.bindQuery = PipeInfo.QueryBind.OPTIONS;
                        } else if ("parameters".equals(bindQuery)) {
                           info.bindQuery = PipeInfo.QueryBind.PARAMETERS;
                        }
                        methodPipelines.put(method,info);
                        String outputType = top.getAttribute("output-type");
                        if (outputType!=null) {
                           info.outputType = MediaType.valueOf(outputType);
                        }
                        NodeList nl = top.getElementsByTagNameNS(NS, "option");
                        for (int i=0; i<nl.getLength(); i++) {
                           Element option = (Element)nl.item(i);
                           String name = option.getAttribute("name");
                           if (name==null) {
                              continue;
                           }
                           QName optionName = QName.fromClarkName(name);
                           String source = option.getAttribute("source");
                           String defaultValue = option.getAttribute("default");
                           if (source!=null) {
                              source = source.trim();
                           }
                           if (option.hasAttribute("value")) {
                              String value = option.getAttribute("value");
                              getLogger().fine("Binding "+optionName+" to value: "+value);
                              info.bindOption(optionName.getClarkName(), value);
                           } else if ("parameters".equals(source)) {
                              String from = option.getAttribute("from");
                              if (from==null || from.length()==0) {
                                 from = optionName.getLocalName();
                              }
                              getLogger().fine("Binding "+optionName+" to parameter "+from);
                              info.bindOptionToParameter(from,optionName,defaultValue);
                           } else if ("header".equals(source)) {
                              String from = option.getAttribute("from");
                              if (from==null || from.length()==0) {
                                 from = optionName.getLocalName();
                              }
                              getLogger().fine("Binding "+optionName+" to header "+from);
                              info.bindOptionToHeader(from,optionName,defaultValue);
                           } else if ("request".equals(source)) {
                              String from = option.getAttribute("from");
                              if (from==null || from.length()==0) {
                                 from = optionName.getLocalName();
                              }
                              getLogger().fine("Binding "+optionName+" to request "+from);
                              info.bindOptionToRequest(from,optionName,defaultValue);
                           } else if ("attributes".equals(source)) {
                              String from = option.getAttribute("from");
                              if (from==null || from.length()==0) {
                                 from = optionName.getLocalName();
                              }
                              getLogger().fine("Binding "+optionName+" to attribute "+from);
                              info.bindOptionToAttribute(from,optionName,defaultValue);
                           } else if ("query".equals(source)) {
                              String from = option.getAttribute("from");
                              if (from==null || from.length()==0) {
                                 from = optionName.getLocalName();
                              }
                              getLogger().fine("Binding "+optionName+" to query "+from);
                              info.bindOptionToQuery(from,optionName,defaultValue);
                           }
                        }
                        nl = top.getElementsByTagNameNS(NS, "require");
                        for (int i=0; i<nl.getLength(); i++) {
                           Element require = (Element)nl.item(i);
                           String typeName = require.getAttribute("content-type");
                           if (typeName==null) {
                              continue;
                           }
                           MediaType contentType = MediaType.valueOf(typeName);
                           info.requiredTypes.add(contentType);
                        }
                     } catch (URISyntaxException ex) {
                        getLogger().severe("Bad URI: "+ex.getMessage());
                     }
                  }
               }
            }
         }
      }
      optionsToBind = null;
      String [] optionNames = context.getParameters().getValuesArray(XProcResource.OPTION_NAMES_PARAM);
      if (optionNames!=null) {
         for (int v=0; v<optionNames.length; v++) {
            String [] names = optionNames[v].split(",");
            for (int i=0; i<names.length; i++) {
               QName name = QName.fromClarkName(names[i].trim());
               if (optionsToBind==null) {
                  optionsToBind = new ArrayList<QName>();
               }
               optionsToBind.add(name);
               getLogger().fine("Binding option "+name+" to parameter/attribute.");
            }
         }
      }
      headersToBind = null;
      optionNames = context.getParameters().getValuesArray(XProcResource.OPTION_HEADER_NAMES_PARAM);
      if (optionNames!=null) {
         for (int v=0; v<optionNames.length; v++) {
            String [] names = optionNames[v].split(",");
            for (int i=0; i<names.length; i++) {
               String [] parts = names[i].trim().split("=");
               QName name = QName.fromClarkName(parts[parts.length==1 ? 0 : 1]);
               String header = parts.length==1 ? name.getLocalName() : parts[0];
               if (headersToBind==null) {
                  headersToBind = new TreeMap<String,QName>();
               }
               headersToBind.put(header,name);
               getLogger().fine("Binding option "+name+" to request header.");
            }
         }
      }
   }
   
   public XProcCache getCache() {
      return cache;
   }
   
   public Context getContext() {
      return context;
   }
   
   public Logger getLogger() {
      return context.getLogger();
   }
   
   protected URI resolve(URI baseURI,String href) 
      throws URISyntaxException
   {
      if (contextObject!=null) {
         URL resourceURL = contextObject.getClass().getResource(href);
         if (resourceURL==null) {
            getLogger().severe("Cannot load reference "+href+" against class "+contextObject.getClass().getName());
            return null;
         } else {
            return resourceURL.toURI();
         }
      } else {
         return baseURI==null ? new URI(href) : baseURI.resolve(href);
      }
      
   }
   
   protected String getHeaderValue(String headerName,Request request,Series<Header> headers) {
      if (headerName.equals("Host")) {
         Reference hostRef = request.getHostRef();
         return hostRef==null ? null : hostRef.toString();
      } else {
         return headers==null ? null : headers.getFirstValue(headerName);
      }
   }
   
   protected String getParameterValue(String name)
   {
      return getContext().getParameters().getFirstValue(name);
   }
   
   protected String getAttributeValue(Request request,String attributeName)
   {
      Object o = request.getAttributes().get(attributeName);
      if (o!=null) {
         return o.toString();
      }
      o = getContext().getAttributes().get(attributeName);
      if (o!=null) {
         return o.toString();
      }
      return null;
   }
   
   protected String getRequestValue(Request request, String facetName)
   {
      if ("path".equals(facetName)) {
         return request.getResourceRef().getPath();
      } else if ("uri".equals(facetName)) {
         return request.getResourceRef().toString();
      } else if ("remaining".equals(facetName)) {
         String remaining = request.getResourceRef().getRemainingPart();
         int q = remaining.indexOf('?');
         return q<0 ? remaining : remaining.substring(0,q);
      } else if ("query".equals(facetName)) {
         return request.getResourceRef().getQuery();
      } else if ("base".equals(facetName)) {
         Reference baseRef = request.getResourceRef().getBaseRef();
         if (baseRef!=null) {
            return baseRef.toString();
         }
      }
      return null;
   }
   
   protected String getOptionValue(Request request,QName name)
   {
      String key = name.getLocalName();
      if (name.getNamespaceURI()!=null) {
         key = name.getClarkName();
      }
      Object o = request.getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      o = getContext().getAttributes().get(key);
      if (o!=null) {
         return o.toString();
      }
      return getContext().getParameters().getFirstValue(key);
   }
   
   protected void bindOptions(PipeInfo pipeInfo,XPipeline pipeline,Request request) {
      boolean isFineLog = getLogger().isLoggable(Level.FINE);
      Form query = request.getResourceRef().getQueryAsForm();
      Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
      if (pipeInfo.optionBindings!=null) {
         for (OptionBinding binding : pipeInfo.optionBindings) {
            String value = null;
            switch (binding.source) {
               case ATTRIBUTES:
                  value = getAttributeValue(request,binding.name);
                  break;
               case HEADERS:
                  value = getHeaderValue(binding.name,request,headers);
                  break;
               case PARAMETERS:
                  value = getParameterValue(binding.name);
                  break;
               case QUERY:
                  value = query.getValues(binding.name);
                  break;
               case REQUEST:
                  value = getRequestValue(request,binding.name);
                  break;
            }
            if (value==null) {
               value = binding.defaultValue;
            }
            if (value!=null) {
               if (isFineLog) {
                  getLogger().fine("Option "+binding.optionName+"="+value+" from "+binding.source);
               }
               pipeline.passOption(binding.optionName, new RuntimeValue(value));
            } else if (isFineLog) {
               getLogger().fine("Option "+binding.optionName+" has no value.");
            }
         }
      }
      if (optionsToBind!=null) {
         for (QName optionName : optionsToBind) {
            String value = getOptionValue(request,optionName);
            if (value!=null) {
               getLogger().fine("Option: "+optionName+"="+value);
               pipeline.passOption(optionName, new RuntimeValue(value));
            } else {
               getLogger().fine("Option: "+optionName+" has no value.");
            }
         }
      }
      if (headersToBind!=null) {
         for (String headerName : headersToBind.keySet()) {
            String value = getHeaderValue(headerName,request,headers);
            QName optionName = headersToBind.get(headerName);
            if (value!=null) {
               if (isFineLog) {
                  getLogger().fine("Option: "+optionName+"="+value);
               }
               pipeline.passOption(optionName, new RuntimeValue(value));
            } else if (isFineLog) {
               getLogger().fine("Option: "+optionName+" has no value.");
            }
         }
      }
      if (pipeInfo.optionValues!=null) {
         for (String name : pipeInfo.optionValues.keySet()) {
            String value = pipeInfo.optionValues.get(name);
            QName optionName = QName.fromClarkName(name);
            if (isFineLog) {
               getLogger().fine("Option: "+optionName+"="+value);
            }
            pipeline.passOption(optionName,new RuntimeValue(value));
         }
      }
      switch (pipeInfo.bindQuery) {
         case OPTIONS:
            for (String name : query.getNames()) {
               String value = query.getValues(name);
               if (isFineLog) {
                  getLogger().fine("Option: "+name+"="+value);
               }
               pipeline.passOption(QName.fromClarkName(name),new RuntimeValue(value));
            }
         break;
         case PARAMETERS:
            for (String name : query.getNames()) {
               String value = query.getValues(name);
               if (isFineLog) {
                  getLogger().fine("Parameter: "+name+"="+value);
               }
               pipeline.setParameter(QName.fromClarkName(name),new RuntimeValue(value));
            }
      }
   }
   
   public void handle(boolean inputFromRequest,Request request, Response response) {
      boolean isFineLog = getLogger().isLoggable(Level.FINE);
      if (cache==null) {
         response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         return;
      }
      Method requestMethod = request.getMethod();
      PipeInfo pipeInfo = methodPipelines.get(requestMethod);
      if (pipeInfo==null) {
         response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
         return;
      }
      getLogger().info(getLogger().isLoggable(Level.FINE) ? "Using pipeline: "+pipeInfo.location : "");

      if (pipeInfo.requiredTypes.size()>0 && request.isEntityAvailable()) {
         MediaType contentType = request.getEntity().getMediaType();
         boolean matches = false;
         for (MediaType type : pipeInfo.requiredTypes) {
            if (type.equals(contentType) || type.includes(contentType)) {
               matches = true;
               break;
            }
         }
         if (!matches) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new StringRepresentation("Content type "+contentType+" is not allowed.",MediaType.TEXT_PLAIN));
            return;
         }
      }
      XPipelineContext cachedXProc = null;
      try {
         cachedXProc = cache.get(pipeInfo.location);
      } catch (Exception ex) {
         getLogger().log(Level.SEVERE,"Cannot load pipeline.",ex);
         response.setStatus(Status.SERVER_ERROR_INTERNAL);
         return;
      }
      final XPipelineContext xproc = cachedXProc;
      XPipeline pipeline = xproc.getPipeline();
      if (pipeline.getOutputs().size()>1) {
         // unsupported
         getLogger().severe("Multiple outputs are not supported.");
         response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         cache.release(xproc);
         return;
      }
      String inputPipeName = null;
      String parametersPipeName = null;
      boolean unknownPipes = false;
      for (String pipeName : pipeline.getInputs()) {
         XInput inputPipe = pipeline.getInput(pipeName);
         if (inputPipeName==null && !inputPipe.getParameters()) {
            inputPipeName = pipeName;
         } else if (parametersPipeName==null && inputPipe.getParameters()) {
            parametersPipeName = pipeName;
         } else {
            unknownPipes = true;
         }
      }
      if (inputFromRequest) {
         if (request.isEntityAvailable() && !unknownPipes && inputPipeName!=null) {
            Representation entity = request.getEntity();
            MediaType mediaType = entity.getMediaType();
            InputSource isource = null;
            String xml = null;
            if (MediaType.APPLICATION_ALL_XML.includes(mediaType) || MediaType.TEXT_XML.equals(mediaType)) {
               try {
                  isource = new InputSource(entity.getReader());
               } catch (IOException ex) {
                  getLogger().warning("I/O error getting XML input reader: "+ex.getMessage());
                  response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                  cache.release(xproc);
                  return;
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
               isource = new InputSource(new StringReader(xml));
            } else {
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               response.setEntity(new StringRepresentation("Unprocessable input media type "+mediaType));
               cache.release(xproc);
               return;
            }
            SAXSource source = new SAXSource(isource);
            DocumentBuilder builder = cache.getRuntime().getProcessor().newDocumentBuilder();
            try {
               XdmNode doc = builder.build(source);
               pipeline.clearInputs(inputPipeName);
               pipeline.writeTo(inputPipeName,doc);
            } catch (SaxonApiException ex) {
               getLogger().warning("Syntax error in XML from client: "+ex.getMessage());
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               cache.release(xproc);
               return;
            }

            try {
               isource.getCharacterStream().close();
            } catch (IOException ex) {
               getLogger().log(Level.WARNING,"I/O exception on close of client stream.",ex);
            }


         } else if (request.isEntityAvailable() || unknownPipes) {
            // Note: excluded case of no entity and no input ports
            if (pipeline.getInputs().size()==1) {
               getLogger().severe("Required input not provided to pipeline: "+xproc.location+", port="+xproc.getPipeline().getInputs());
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               cache.release(xproc);
               return;
            } else if (pipeline.getInputs().isEmpty()) {
               getLogger().severe("Input not allowed on pipeline: "+xproc.location);
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               cache.release(xproc);
               return;
            }
            // unsupported
            getLogger().severe("Input port mismatch: "+xproc.location+", ports="+xproc.getPipeline().getInputs());
            response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
            cache.release(xproc);
            return;
         }
      } else if (response.isEntityAvailable() && !unknownPipes && inputPipeName!=null) {
         Representation entity = response.getEntity();
         MediaType mediaType = entity.getMediaType();
         InputSource isource = null;
         
         if (!MediaType.APPLICATION_ALL_XML.includes(mediaType) && !MediaType.TEXT_XML.equals(mediaType)) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new StringRepresentation("Unprocessable input media type "+mediaType));
            cache.release(xproc);
            return;
         }
         
         try {
            isource = new InputSource(entity.getReader());
         } catch (IOException ex) {
            getLogger().warning("I/O error getting XML input reader: "+ex.getMessage());
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return;
         }
         
         SAXSource source = new SAXSource(isource);
         DocumentBuilder builder = cache.getRuntime().getProcessor().newDocumentBuilder();
         try {
            XdmNode doc = builder.build(source);
            pipeline.clearInputs(inputPipeName);
            pipeline.writeTo(inputPipeName,doc);
         } catch (SaxonApiException ex) {
            getLogger().warning("Syntax error in XML from client: "+ex.getMessage());
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return;
         }

         try {
            isource.getCharacterStream().close();
         } catch (IOException ex) {
            getLogger().log(Level.WARNING,"I/O exception on close of client stream.",ex);
         }
         
      } else if (request.isEntityAvailable() || unknownPipes) {
         // Note: excluded case of no entity and no input ports
         if (pipeline.getInputs().size()==1) {
            getLogger().severe("Required input not provided to pipeline: "+xproc.location+", port="+xproc.getPipeline().getInputs());
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return;
         } else if (pipeline.getInputs().isEmpty()) {
            getLogger().severe("Input not allowed on pipeline: "+xproc.location);
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            cache.release(xproc);
            return;
         }
         // unsupported
         getLogger().severe("Input port mismatch: "+xproc.location+", ports="+xproc.getPipeline().getInputs());
         response.setStatus(Status.SERVER_ERROR_SERVICE_UNAVAILABLE);
         cache.release(xproc);
         return;
      }
      
      bindOptions(pipeInfo,pipeline,request);
      
      try {
         try {
            pipeline.run();
         } catch (XProcException ex) {
            cache.release(xproc);
            getLogger().info("Error running pipeline: ("+ex.getErrorCode()+") "+ex.getMessage());
            response.setStatus(Status.SERVER_ERROR_INTERNAL);
            return;
         }
         if (xproc.getPipeline().getOutputs().size()==1) {
            final String outputPort = xproc.getPipeline().getOutputs().iterator().next();
            if (pipeInfo.bindResult) {
               getLogger().fine("Binding result of pipeline run...");
               response.setStatus(Status.SUCCESS_NO_CONTENT);
               Series<Header> responseHeaders = (Series<Header>)response.getAttributes().get("org.restlet.http.headers");
               if (responseHeaders==null) {
                  responseHeaders = new Series<Header>(Header.class);
                  response.getAttributes().put("org.restlet.http.headers",responseHeaders);
               }
               final ReadablePipe rpipe = xproc.getPipeline().readFrom(outputPort);
               if (rpipe.moreDocuments()) {
                  XdmNode doc = rpipe.read();
                  XdmSequenceIterator seq = doc.axisIterator(Axis.CHILD,HTTP_NAME);
                  if (seq.hasNext()) {
                     getLogger().fine("Found http to bind...");
                     XdmNode http = (XdmNode)seq.next();
                     String status = http.getAttributeValue(STATUS_NAME);
                     if (status!=null && status.length()>0) {
                        getLogger().fine(isFineLog ? "Status: "+status : "");
                        response.setStatus(Status.valueOf(Integer.parseInt(status)));
                     }
                     Series<Header> headers = (Series<Header>)request.getAttributes().get("org.restlet.http.headers");
                     XdmSequenceIterator headerElements = http.axisIterator(Axis.CHILD,HEADER_NAME);
                     while (headerElements.hasNext()) {
                        XdmNode headerElement = (XdmNode)headerElements.next();
                        String name = headerElement.getAttributeValue(NAME_NAME);
                        if (name!=null && name.length()>0) {
                           String value = headerElement.getStringValue();
                           getLogger().fine(isFineLog ? name+": "+value : "");
                           if ("location".equalsIgnoreCase(name)) {
                              response.setLocationRef(value);
                           } else if ("Cache-Control".equalsIgnoreCase(name)) {
                              List<CacheDirective> directives = response.getCacheDirectives();
                              if (directives==null) {
                                 directives = new ArrayList<CacheDirective>();
                                 response.setCacheDirectives(directives);
                              }
                              String [] parts = value.trim().split("=");
                              if (parts.length>1) {
                                 directives.add(new CacheDirective(parts[0],parts[1]));
                              } else {
                                 directives.add(new CacheDirective(parts[0]));
                              }
                           } else {
                              headers.add(name, value);
                           }
                        }
                     }
                     XdmSequenceIterator attrElements = http.axisIterator(Axis.CHILD,ATTRIBUTE_NAME);
                     while (attrElements.hasNext()) {
                        XdmNode attrElement = (XdmNode)attrElements.next();
                        String name = attrElement.getAttributeValue(NAME_NAME);
                        String value = attrElement.getAttributeValue(VALUE_NAME);
                        if (name!=null && value!=null) {
                           response.getAttributes().put(name,value);
                        }
                     }
                     XdmSequenceIterator entityElements = http.axisIterator(Axis.CHILD,ENTITY_NAME);
                     boolean doRelease = true;
                     while (entityElements.hasNext()) {
                        XdmNode entityElement = (XdmNode)entityElements.next();
                        String mediaTypeName = entityElement.getAttributeValue(TYPE_NAME);
                        String lastModifiedValue = entityElement.getAttributeValue(LAST_MODIFIED_NAME);
                        Date lastModified = null;
                        if (lastModifiedValue!=null && lastModifiedValue.length()>22) {
                           //getLogger().info("Parsing date: "+lastModifiedValue);
                           try {
                              String dtValue = lastModifiedValue.substring(0,22)+lastModifiedValue.substring(23);
                              lastModified = xsdDateFormat.parse(dtValue);
                           } catch (ParseException ex) {
                              int lastColon = lastModifiedValue.lastIndexOf(':');
                              String dtValue = lastModifiedValue.substring(0,lastColon)+lastModifiedValue.substring(lastColon+1);
                              try {
                                 lastModified = xsdDateFormatWithMilliseconds.parse(dtValue);
                              } catch (ParseException pex) {
                                 Date date = new Date();
                                 getLogger().warning("Cannot parse date '"+lastModifiedValue+"' (sample "+xsdDateFormatWithMilliseconds.format(date)+" or "+xsdDateFormat.format(date)+"): "+pex.getMessage());
                              }
                           }
                        }
                        MediaType mediaType = mediaTypeName==null || mediaTypeName.length()==0 ? MediaType.TEXT_PLAIN : MediaType.valueOf(mediaTypeName);
                        boolean entitySet = false;
                        if (MediaType.APPLICATION_ALL_XML.includes(mediaType) || MediaType.TEXT_XML.equals(mediaType)) {
                           XdmSequenceIterator children = entityElement.axisIterator(Axis.CHILD);
                           HashSet<String> exlcudedNamespaces = new HashSet<String>();
                           exlcudedNamespaces.add(HTTP_NS);
                           while (children.hasNext()) {
                              XdmItem item = children.next();
                              if (!item.isAtomicValue()) {
                                 XdmNode node = (XdmNode)item;
                                 if (node.getNodeKind()==XdmNodeKind.ELEMENT) {
                                    entitySet = true;
                                    final XdmNode documentElement = S9apiUtils.removeNamespaces(cache.getRuntime(), node, exlcudedNamespaces,true);
                                    final Serialization serial = xproc.getPipeline().getSerialization(outputPort); 
                                    response.setEntity(new OutputRepresentation(mediaType) {
                                       public void write(OutputStream out) 
                                          throws IOException
                                       {
                                          Serialization serial = xproc.getPipeline().getSerialization(outputPort);               
                                          WritableDocument outputDoc = new WritableDocument(cache.getRuntime(),null,serial,out);
                                          outputDoc.write(documentElement);
                                          out.flush();
                                       }
                                       public void release() {
                                          cache.release(xproc);
                                       }
                                    });
                                    doRelease = false;
                                    
                                    break;
                                 }
                              }
                           }
                        } 
                        if (!entitySet) {
                           response.setEntity(new StringRepresentation(entityElement.getStringValue(),mediaType));
                        }
                        if (lastModified!=null) {
                           response.getEntity().setModificationDate(lastModified);
                        }
                     }
                     if (doRelease) {
                        cache.release(xproc);
                     }
                  } else {
                     getLogger().fine("No HTTP binding from pipeline, serializing result...");
                     // XML to serialize
                     final XdmNode startDoc = doc;
                     final String charset = pipeInfo.outputType.getParameters().getFirstValue("charset");
                     response.setStatus(Status.SUCCESS_OK);
                     response.setEntity(new OutputRepresentation(pipeInfo.outputType) {
                        public void write(OutputStream out) 
                           throws IOException
                        {
                           Serialization serial = xproc.getPipeline().getSerialization(outputPort);
                           if (serial==null && charset!=null) {
                              serial = new Serialization(cache.getRuntime(),null);
                           }
                           if (charset!=null) {
                              serial.setEncoding(charset);
                           }
                           WritableDocument outputDoc = new WritableDocument(cache.getRuntime(),null,serial,out);
                           outputDoc.write(startDoc);
                           while (rpipe.moreDocuments()) {
                              try {
                                 XdmNode nextDoc = rpipe.read();
                                 if (nextDoc!=null) {
                                    outputDoc.write(nextDoc);
                                 }
                              } catch (SaxonApiException ex) {
                                 throw new IOException("Exception while writing output port "+outputPort,ex);
                              }
                           }
                           out.flush();
                        }
                        public void release() {
                           cache.release(xproc);
                        }
                     });
                  }
               }
            } else {
               final String charset = pipeInfo.outputType.getParameters().getFirstValue("charset");
               response.setEntity(new OutputRepresentation(pipeInfo.outputType) {
                  public void write(OutputStream out) 
                     throws IOException
                  {
                     Serialization serial = xproc.getPipeline().getSerialization(outputPort);               
                     if (serial==null && charset!=null) {
                        serial = new Serialization(cache.getRuntime(),null);
                     }
                     if (charset!=null) {
                        serial.setEncoding(charset);
                     }
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
               });
               response.setStatus(Status.SUCCESS_OK);
            }
         } else {
            cache.release(xproc);
            response.setStatus(Status.SUCCESS_NO_CONTENT);
         }
      } catch (SaxonApiException ex) {
         cache.release(xproc);
         getLogger().info("Error running pipeline: "+ex.getMessage());
         response.setStatus(Status.SERVER_ERROR_INTERNAL);
         return;
      }
   }
}
