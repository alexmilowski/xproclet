/*
 * Configuration.java
 *
 * Created on June 18, 2007, 3:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.resource.Directory;
import org.restlet.resource.Finder;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Extractor;
import org.restlet.routing.Filter;
import org.restlet.routing.Redirector;
import org.restlet.routing.Router;
import org.restlet.routing.Template;
import org.restlet.routing.TemplateRoute;
import org.restlet.routing.Variable;
import org.restlet.service.MetadataService;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 *
 * @author alex
 */
public class Configuration
{
   static Logger LOG = Logger.getLogger(Configuration.class.getName());

   static void logSevere(Node n,String message) {
      LOG.severe(DocumentLoader.getLocation(n)+": "+message);
   }
   
   static final String NAMESPACE = "http://www.xproclet.org/V/Server/";
   static final DocumentLoader.Name ALIAS = new DocumentLoader.Name(NAMESPACE,"alias");
   static final DocumentLoader.Name COMPONENT = new DocumentLoader.Name(NAMESPACE,"component");
   static final DocumentLoader.Name CLIENT = new DocumentLoader.Name(NAMESPACE,"client");
   static final DocumentLoader.Name SERVER = new DocumentLoader.Name(NAMESPACE,"server");
   static final DocumentLoader.Name LIBRARY = new DocumentLoader.Name(NAMESPACE,"library");
   static final DocumentLoader.Name APPLICATION = new DocumentLoader.Name(NAMESPACE,"application");
   static final DocumentLoader.Name HOST = new DocumentLoader.Name(NAMESPACE,"host");
   static final DocumentLoader.Name PARAMETER = new DocumentLoader.Name(NAMESPACE,"parameter");
   static final DocumentLoader.Name CONTEXT = new DocumentLoader.Name(NAMESPACE,"context");
   static final DocumentLoader.Name LOG_E = new DocumentLoader.Name(NAMESPACE,"log");
   static final DocumentLoader.Name DEFINE = new DocumentLoader.Name(NAMESPACE,"define");
   static final DocumentLoader.Name ROUTE = new DocumentLoader.Name(NAMESPACE,"route");
   static final DocumentLoader.Name FILTER = new DocumentLoader.Name(NAMESPACE,"filter");
   static final DocumentLoader.Name ROUTER = new DocumentLoader.Name(NAMESPACE,"router");
   static final DocumentLoader.Name MATCH = new DocumentLoader.Name(NAMESPACE,"match");
   static final DocumentLoader.Name ATTRIBUTE = new DocumentLoader.Name(NAMESPACE,"attribute");
   static final DocumentLoader.Name NEXT = new DocumentLoader.Name(NAMESPACE,"next");
   static final DocumentLoader.Name DEFAULT = new DocumentLoader.Name(NAMESPACE,"default");
   static final DocumentLoader.Name CONTENT = new DocumentLoader.Name(NAMESPACE,"content");
   static final DocumentLoader.Name REDIRECT = new DocumentLoader.Name(NAMESPACE,"redirect");
   static final DocumentLoader.Name INCLUDE = new DocumentLoader.Name(NAMESPACE,"include");
   static final DocumentLoader.Name VARIABLE = new DocumentLoader.Name(NAMESPACE,"variable");
   static final DocumentLoader.Name MAP = new DocumentLoader.Name(NAMESPACE,"map");

   static final String [] variableTypeNames = {
      "all", "alpha", "alpha-digit", 
      "comment", "comment-attribute", "digit", 
      "token", "uri", "uri-fragment", 
      "uri-path", "uri-query", "uri-query-param", 
      "uri-scheme", "uri-segment", "uri-unreserved", 
      "word"
   };
   
   static int [] variableTypes = {
      Variable.TYPE_ALL, Variable.TYPE_ALPHA, Variable.TYPE_ALPHA_DIGIT, 
      Variable.TYPE_COMMENT, Variable.TYPE_COMMENT_ATTRIBUTE, Variable.TYPE_DIGIT,
      Variable.TYPE_TOKEN, Variable.TYPE_URI_ALL, Variable.TYPE_URI_FRAGMENT,
      Variable.TYPE_URI_PATH, Variable.TYPE_URI_QUERY, Variable.TYPE_URI_QUERY_PARAM,
      Variable.TYPE_URI_SCHEME, Variable.TYPE_URI_SEGMENT, Variable.TYPE_URI_UNRESERVED,
      Variable.TYPE_WORD
   };
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

   
   static Restlet getRestletInstance(Context context, Class targetClass) 
      throws InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
   {
      try {
         Constructor<Restlet> makeit = targetClass.getConstructor(Context.class);
         return makeit.newInstance(context);
      } catch (NoSuchMethodException ex) {
         Constructor<Restlet> makeit = targetClass.getConstructor();
         Restlet r = makeit.newInstance();
         r.setContext(context);
         return r;
      }
   }
   
   static boolean isServerResource(Class targetClass) {
      return ServerResource.class.isAssignableFrom(targetClass);
   }
   
   public class Server {
      String addr;
      int port;
      Protocol protocol;
      Map<String,Host> hosts;
      Context serverContext;
      String agent;
      
      public Server(String addr,int port,Protocol protocol)
      {
         this.addr = addr;
         this.port = port;
         this.protocol = protocol;
         this.hosts = new TreeMap<String,Host>();
         this.serverContext = new Context();
         this.agent = "XProclet";
      }
      
      public String getKey() {
         return this.addr+":"+this.port;
      }
      
      public String getAddress() {
         return addr;
      }
      
      public int getPort() {
         return port;
      }
      
      public Protocol getProtocol() {
         return protocol;
      }
      
      public Map<String,Host> getHosts() {
         return hosts;
      }
      
      public Context getContext() {
         return serverContext;
      }
      
      public String getAgent() {
         return agent;
      }
      
      public void setAgent(String agent) {
         this.agent = agent;
      }
      
   }
   
   public class Host extends App {
      String name;
      Set<String> aliases;
      public Host(String name, Element conf)
      {
         this(name,null,conf);
      }

      public Host(String name,String internalName,Element conf)
      {
         super(internalName,conf);
         this.name = name;
         this.aliases = new TreeSet<String>();
         for (Element aliasElement : DocumentLoader.getElementsByName(conf,ALIAS)) {
            String alias = DocumentLoader.getAttributeValue(aliasElement,"name");
            LOG.info("Alias: "+alias);
            if (alias!=null) {
               aliases.add(alias.trim());
            }
         }
      }
         
      public String getName() {
         return name;
      }
      
      public Set<String> getAliases() {
         return aliases;
      }

      
   }

   public class App {
      String internalName;
      Map<String,String> logConf;
      Element conf;
      
      public App(String internalName,Element conf)
      {
         this.internalName = internalName;
         this.conf = conf;
         this.logConf = new HashMap<String,String>();
         Iterator<Element> logs = DocumentLoader.getElementsByName(conf,LOG_E).iterator();
         if (logs.hasNext()) {
            Element logE = logs.next();
            NamedNodeMap attrs = logE.getAttributes();
            for (int i=0; i<attrs.getLength(); i++) {
               Attr att = (Attr)attrs.item(i);
               logConf.put(att.getLocalName(),att.getValue());
            }
         }
      }
      
      public String getInternalName() {
         return internalName;
      }

      public Map<String,String> getLogConfiguration() {
         return logConf;
      }
      
      public void attach(Router router) {
         LOG.fine("attach() context: "+router.getContext());
         loadContext(router.getContext(),conf);
         for (Element child : DocumentLoader.getElementChildren(conf)) {
            attach(router,child,false);
         }
      }
      
      public void attachNext(Filter filter,Element child) {
         if (filter==null) {
            throw new RuntimeException("Filter is null!");
         }
         attach(null,filter,child,false);
      }
      public void attach(Router router,Element child,boolean defaultRoute) {
         attach(router,null,child,defaultRoute);
      }
      protected void attach(Router router,Filter filter,Element child,boolean defaultRoute) {
         Context parentContext = router==null ? filter.getContext() : router.getContext();
         if (ROUTER.equals(child)) {
            String match = DocumentLoader.getAttributeValue(child,"match");
            if (match==null && filter==null && !defaultRoute) {
               LOG.severe("The router element does not have the required match attribute.");
               return;
            }
            if (defaultRoute) {
               router.attachDefault(createRouter(parentContext,child));
            } else if (router!=null) {
               router.attach(match,createRouter(parentContext,child));
            } else {
               filter.setNext(createRouter(parentContext,child));
            }
         } else if (ROUTE.equals(child)) {
            String match = DocumentLoader.getAttributeValue(child,"match");
            if (match==null && filter==null && !defaultRoute) {
               logSevere(child,"The route element does not have the required match attribute.");
               return;
            }
            Class def = getTargetClass(child);
            if (def==null) {
               logSevere(child,"The route element does not have any resource or restlet assocaited with it.");
               return;
            }
            TemplateRoute route = null;
            String [] extraction = null;
            if (match.length()>0 && match.charAt(match.length()-1)=='}') {
               // seek to start of bracket expression
               int pos = match.length()-2;
               for (; pos>0 && match.charAt(pos)!='{'; pos--);
               if (pos>=0 && match.charAt(pos+1)=='?') {
                  String expr = match.substring(pos+2,match.length()-1);
                  extraction = expr.split(",");
                  match = match.substring(0,pos);
                  LOG.fine("redirect extraction, match="+match+", expr="+expr);
               }
            }
            if (isServerResource(def)) {
               Restlet finder = Finder.createFinder(def, Finder.class, hasParametersOrAttributes(child) ? createContext(parentContext,child) : parentContext,parentContext.getLogger());
               if (extraction!=null) {
                  Extractor extractor = new Extractor(parentContext,finder);
                  for (int i=0; i<extraction.length; i++) {
                     extractor.extractFromQuery(extraction[i], extraction[i], true);
                  }
                  finder = extractor;
               }
               if (defaultRoute) {
                  LOG.fine("Mapping default -> "+def.getName());
                  route = router.attachDefault(finder);
               } else if (router!=null) {
                  LOG.fine("Mapping "+match+" -> "+def.getName());
                  route = router.attach(match,finder);
               } else {
                  filter.setNext(finder);
               }
            } else {
               try {
                  Restlet restlet = createRestlet(parentContext,def,child);
                  if (extraction!=null) {
                     Extractor extractor = new Extractor(parentContext,restlet);
                     for (int i=0; i<extraction.length; i++) {
                        extractor.extractFromQuery(extraction[i], extraction[i], true);
                     }
                     restlet = extractor;
                  }
                  if (defaultRoute) {
                     LOG.fine("Mapping default -> "+def.getName());
                     route = router.attachDefault(restlet);
                  } else if (router!=null) {
                     LOG.fine("Mapping "+match+" -> "+def.getName());
                     route = router.attach(match,restlet);
                  } else {
                     filter.setNext(restlet);
                  }
               } catch (Exception ex) {
                  LOG.log(Level.SEVERE,"Cannot instantiate class: "+def.getName(),ex);
               }
            }
            String mode = DocumentLoader.getAttributeValue(child,"mode");
            if (route!=null && mode!=null) {
               if ("starts-with".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
               } else if ("equals".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
               }
            }
            
            for (Element variableConf : DocumentLoader.getElementsByName(child,VARIABLE)) {
               String varName = DocumentLoader.getAttributeValue(variableConf,"name");
               String type = DocumentLoader.getAttributeValue(variableConf,"type");
               if (varName!=null) {
                  Variable var = route.getTemplate().getVariables().get(varName.trim());
                  if (var!=null && type!=null) {
                     type = type.trim();
                     for (int i=0; i<variableTypeNames.length; i++) {
                        if (variableTypeNames[i].equals(type)) {
                           var.setType(variableTypes[i]);
                           break;
                        }
                     }
                  }
               }
            }
         } else if (FILTER.equals(child)) {
            String match = DocumentLoader.getAttributeValue(child,"match");
            if (match==null && filter==null && !defaultRoute) {
               LOG.severe("The filter element does not have the required match attribute.");
               return;
            }
            Class def = getTargetClass(child);
            if (def==null) {
               return;
            }
            Filter childFilter = null;
            try {
               childFilter = createFilter(parentContext,def,child);
            } catch (Exception ex) {
               LOG.log(Level.SEVERE,"Cannot instantiate filter.",ex);
               return;
            }
            if (defaultRoute) {
               router.attachDefault(childFilter);
            } else if (router!=null) {
               TemplateRoute route = router.attach(match,childFilter);
               String mode = DocumentLoader.getAttributeValue(child,"mode");
               if (route!=null && mode!=null) {
                  if ("starts-with".equals(mode)) {
                     route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
                  } else if ("equals".equals(mode)) {
                     route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
                  }
               }
            } else {
               filter.setNext(childFilter);
            }
            for (Element nextChild : DocumentLoader.getElementChildren(child)) {
               if (NEXT.equals(nextChild)) {
                  for (Element e : DocumentLoader.getElementChildren(nextChild)) {
                     attachNext(childFilter,e);
                  }
               }
            }
         } else if (CONTENT.equals(child)) {
            String match = DocumentLoader.getAttributeValue(child,"match");
            if (match==null && filter==null && !defaultRoute) {
               LOG.severe("The content element does not have the required match attribute.");
               return;
            }
            Restlet restlet = createContent(parentContext,child);
            TemplateRoute route = null;            
            if (defaultRoute) {
               router.attachDefault(restlet);
            } else if (router!=null) {
               route = router.attach(match,restlet);
            } else {
               filter.setNext(restlet);
            }
            String mode = DocumentLoader.getAttributeValue(child,"mode");
            if (route!=null && mode!=null) {
               if ("starts-with".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
               } else if ("equals".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
               }
            }
         } else if (REDIRECT.equals(child)) {
            String match = DocumentLoader.getAttributeValue(child,"match");
            if (match==null && filter==null && !defaultRoute) {
               LOG.severe("The content element does not have the required match attribute.");
               return;
            }
            String to = DocumentLoader.getAttributeValue(child,"to");
            if (to==null) {
               LOG.severe("The redirect element is missing the required 'to' attribute.");
               return;
            }
            
            Reference targetRef = new Reference(to);
            Restlet restlet = null;
            if (targetRef.isAbsolute() && targetRef.getScheme().equals("riap")) {
               restlet = new Redirector(parentContext,to,Redirector.MODE_SERVER_INBOUND);
            } else {
               restlet = new Redirector(parentContext,to,Redirector.MODE_CLIENT_SEE_OTHER);
            }
            TemplateRoute route = null;            
            if (defaultRoute) {
               router.attachDefault(restlet);
            } else if (router!=null) {
               String [] extraction = null;
               if (match.charAt(match.length()-1)=='}') {
                  // seek to start of bracket expression
                  int pos = match.length()-2;
                  for (; pos>0 && match.charAt(pos)!='{'; pos--);
                  if (pos>=0 && match.charAt(pos+1)=='?') {
                     String expr = match.substring(pos+2,match.length()-1);
                     extraction = expr.split(",");
                     match = match.substring(0,pos);
                     LOG.fine("redirect extraction, match="+match+", expr="+expr);
                  }
               }
               if (extraction!=null) {
                  Extractor extractor = new Extractor(parentContext,restlet);
                  for (int i=0; i<extraction.length; i++) {
                     extractor.extractFromQuery(extraction[i], extraction[i], true);
                  }
                  route = router.attach(match,extractor);
               } else {
                  route = router.attach(match,restlet);
               }
            } else {
               filter.setNext(restlet);
            }
            String mode = DocumentLoader.getAttributeValue(child,"mode");
            if (route!=null && mode!=null) {
               if ("starts-with".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_STARTS_WITH);
               } else if ("equals".equals(mode)) {
                  route.getTemplate().setMatchingMode(Template.MODE_EQUALS);
               }
            }
         } else if (DEFAULT.equals(child)) {
            for (Element e : DocumentLoader.getElementChildren(child)) {
               attach(router,e,true);
            }
            
         } else if (INCLUDE.equals(child)) {
            String href = DocumentLoader.getAttributeValue(child,"href");
            if (href==null) {
               return;
            }
            URI location = DocumentLoader.resolve(child.getBaseURI(),href); 
            try {
               Document included = docLoader.load(location);
               Element top = included.getDocumentElement();
               if (!REDIRECT.equals(top) &&
                   !CONTENT.equals(top) &&
                   !FILTER.equals(top) &&
                   !ROUTER.equals(top) &&
                   !ROUTE.equals(top)) {
                  return;
               }
               attach(router,filter,top,defaultRoute);
            } catch (Exception ex) {
               LOG.log(Level.SEVERE,"Cannot load included document: "+location,ex);
            }
         }

      }
      
      public Router createRouter(Context parentContext,Element routerConf) {
         Context routerContext = hasParametersOrAttributes(routerConf) ? createContext(parentContext,routerConf) : parentContext;
         Router router = new Router(routerContext);
         router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
         String method = DocumentLoader.getAttributeValue(routerConf,"method");
         if ("best".equals(method)) {
            router.setRoutingMode(Router.MODE_BEST_MATCH);
         } else if ("first".equals(method)) {
            router.setRoutingMode(Router.MODE_FIRST_MATCH);
         } else if ("last".equals(method)) {
            router.setRoutingMode(Router.MODE_LAST_MATCH);
         } else if ("next".equals(method)) {
            router.setRoutingMode(Router.MODE_NEXT_MATCH);
         } else if ("random".equals(method)) {
            router.setRoutingMode(Router.MODE_RANDOM_MATCH);
         }
         String matching = DocumentLoader.getAttributeValue(routerConf,"mode");
         if ("starts-with".equals(matching)) {
            router.setDefaultMatchingMode(Template.MODE_STARTS_WITH);
         } else if ("equals".equals(matching)) {
            router.setDefaultMatchingMode(Template.MODE_EQUALS);
         }
         for (Element e : DocumentLoader.getElementChildren(routerConf)) {
            attach(router,e,false);
         }
         return router;
      }
      
      protected boolean hasParametersOrAttributes(Element useConf) {
         Iterator<Element> children = DocumentLoader.getElementChildren(useConf).iterator();
         if (children.hasNext()) {
            Element child = children.next();
            return PARAMETER.equals(child) || ATTRIBUTE.equals(child);
         }
         return false;
      }
      
      public Restlet createContent(Context parentContext,Element contentConf) {
         Context contentContext = hasParametersOrAttributes(contentConf) ? createContext(parentContext,contentConf) : parentContext;
         String packageName = DocumentLoader.getAttributeValue(contentConf,"package");
         String href = DocumentLoader.getAttributeValue(contentConf,"href");
         String ref = DocumentLoader.getAttributeValue(contentConf,"ref");
         
         // If there is a package, create the class resource finder and return
         if (packageName!=null) {
            ClassLoader classLoader = ref!=null ? loaders.get(ref.trim()) : this.getClass().getClassLoader();
            String indexName = DocumentLoader.getAttributeValue(contentConf,"index"); 
            ClassResourceFinder finder = new ClassResourceFinder(contentContext,classLoader,packageName,indexName==null ? "index.xhtml" : indexName);
            return finder;
         }
         
         // All other configurations need an href
         if (href==null) {
            LOG.warning("Missing 'href' on "+CONTENT);
            return null;
         }
         boolean templateMode = "template".equals(DocumentLoader.getAttributeValue(contentConf,"resolve"));
         boolean isDirectory = false;
         String uri = null;
         if (!templateMode) {
            URI source = DocumentLoader.resolve(contentConf.getBaseURI(),href);
            String scheme = source.getScheme();
            isDirectory = scheme.equals("file");
            uri = source.toString();
         } else {
            uri = href;
         }
         if (!isDirectory) {
            LOG.fine("  Content proxy to: "+uri);
            ProxyApplication proxy = new ProxyApplication(contentContext,uri);
            proxy.getTunnelService().setEnabled(false);
            String username = DocumentLoader.getAttributeValue(contentConf,"username");
            String password = DocumentLoader.getAttributeValue(contentConf,"password");
            String token = DocumentLoader.getAttributeValue(contentConf,"token");
            if (username!=null) {
               proxy.setIdentity(username, password);
            }
            if (token!=null) {
               proxy.setAccessToken(token);
            }
            proxy.setTemplateMode(templateMode);
            return proxy;
         } else {
            final String dirRef = uri;
            final String indexName = DocumentLoader.getAttributeValue(contentConf,"index"); 
            // hope the directory resource can handle it
            LOG.fine("  Content directory: "+uri);
            final Iterable<Element> extensionMappings = DocumentLoader.getElementsByName(contentConf,MAP);
            Application app = new Application(contentContext) {
               public void start()
                  throws Exception
               {
                  super.start();
                  MetadataService metadata = getMetadataService();
                  //getLogger().info(this+"App metadata: "+metadata);
                  for (Element mapping: extensionMappings) {
                     String ext  = DocumentLoader.getAttributeValue(mapping,"extension");
                     String mediaType  = DocumentLoader.getAttributeValue(mapping,"type");
                     if (ext!=null && mediaType!=null) {
                        getLogger().info("Adding extension mapping: "+ext+" -> "+mediaType);
                        metadata.addExtension(ext,MediaType.valueOf(mediaType),true);
                     }
                  }
               }
               public Restlet createInboundRoot() {
                  Directory directory = new Directory(getContext(),dirRef);
                  directory.setIndexName(indexName==null ? "index.xhtml" : indexName);
                  return directory;
               }
            };
            return app;
         }
         
      }

      public Restlet createRestlet(Context parentContext,Element useConf)
         throws ClassNotFoundException,InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
      {
         Class def = getTargetClass(useConf);
         if (def==null) {
            return null;
         }
         String className = DocumentLoader.getAttributeValue(useConf,"class");
         Class targetClass = this.getClass().getClassLoader().loadClass(className);
         return createRestlet(parentContext,targetClass,useConf);
      }
      
      protected Context createContext(Context parentContext, Element useConf)
      {
         Context appContext = parentContext.createChildContext();
         
         String copy = DocumentLoader.getAttributeValue(useConf,"copy");
         if (copy==null || "parameters".equals(copy) || "all".equals(copy)) {
            for (Parameter param : parentContext.getParameters()) {
               appContext.getParameters().add(param);
            }
         }
         
         if (copy==null || "attributes".equals(copy) || "all".equals(copy)) {
            for (String key : parentContext.getAttributes().keySet()) {
               appContext.getAttributes().put(key,parentContext.getAttributes().get(key));
            }
         }
         
         loadContext(appContext,useConf);
         
         return appContext;
      }
      
      public Restlet createRestlet(Context parentContext,Class targetClass,Element useConf)
         throws InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
      {
         LOG.fine("Creating restlet: "+targetClass);
         Context appContext = createContext(parentContext,useConf);
         return getRestletInstance(appContext,targetClass);
      }
      
      public Filter createFilter(Context parentContext, Element child) 
         throws ClassNotFoundException,InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
      {
         LOG.fine("Creating restlet as filter...");
         return (Filter)createRestlet(parentContext,child);
      }

      public Filter createFilter(Context parentContext, Class targetClass, Element child) 
         throws ClassNotFoundException,InstantiationException,IllegalAccessException,NoSuchMethodException,InvocationTargetException
      {
         LOG.fine("Creating restlet as filter...");
         return (Filter)createRestlet(parentContext,targetClass,child);
      }

   }
   

   Map<String,ClassLoader> loaders;
   Map<String,Class> definitions;
   List<Server> servers;
   List<Protocol> clients;
   List<App> applications;
   DocumentLoader docLoader;
   
   /** Creates a new instance of Configuration */
   public Configuration()
   {
      this.loaders = new HashMap<String,ClassLoader>();
      this.definitions = new HashMap<String,Class>();
      this.servers = new ArrayList<Server>();
      this.clients = new ArrayList<Protocol>();
      this.applications = new ArrayList<App>();
      this.docLoader = new DocumentLoader();
   }
   
   
   public Host createHost(Element hostE)
   {
      String name = DocumentLoader.getAttributeValue(hostE,"name");
      String internalName = DocumentLoader.getAttributeValue(hostE,"internal");
      Host host = new Host(name,internalName,hostE);

      return host;
      
   }
   
   public App createApplication(Element appE)
   {
      String internalName = DocumentLoader.getAttributeValue(appE,"name");
      App app = new App(internalName,appE);

      return app;
      
   }
   public void load(URI location)
      throws Exception
   {
      String shippedAgent = null;
      InputStream is = getClass().getResourceAsStream("/xproclet.properties");
      if (is!=null) {
         Properties releaseProps = new Properties();
         releaseProps.load(is);
         String name = releaseProps.getProperty("xproclet.name");
         String version = releaseProps.getProperty("xproclet.version");
         if (name!=null && version!=null) {
            shippedAgent = name+" V"+version;
         }
         is.close();
      }
      Document doc = docLoader.load(location);
      Element top = doc.getDocumentElement();
      if (!COMPONENT.equals(top)) {
         throw new Exception("Expecting "+COMPONENT+" but found "+DocumentLoader.getName(top));
      }
      
      loadInternal(shippedAgent,top);
      
   }
   
   protected void loadDefinition(Element appdefE) 
      throws Exception
   {
      String name = DocumentLoader.getAttributeValue(appdefE,"name");
      String className = DocumentLoader.getAttributeValue(appdefE,"class");
      String ref = DocumentLoader.getAttributeValue(appdefE,"ref");

      ClassLoader baseLoader = this.getClass().getClassLoader();
      if (ref!=null) {
         LOG.fine("Using base loader "+ref);
         baseLoader = loaders.get(ref.trim());
      }
      List<URL> libraries = new ArrayList<URL>();
      for (Element libE : DocumentLoader.getElementsByName(appdefE,LIBRARY)) {
         String href = DocumentLoader.getAttributeValue(libE,"href");
         if (href!=null) {
            URI u = DocumentLoader.resolve(libE.getBaseURI(),href);
            libraries.add(u.toURL());
         }
      }
      ClassLoader theLoader = null;
      if (!libraries.isEmpty()) {
         URL [] list = new URL[libraries.size()];
         list = libraries.toArray(list);
         theLoader = new URLClassLoader(list,baseLoader);
         LOG.fine("ClassLoader: "+name+" -> "+theLoader);
         LOG.fine("  Libraries: "+libraries);
         if (name!=null) {
            loaders.put(name, theLoader);
         }
      } else {
         theLoader = baseLoader;
      }
      if (theLoader==null) {
         throw new Exception(DocumentLoader.getLocation(appdefE)+"Cannot find definition: "+ref);
      }
      if (className!=null && name!=null) {
         Class cdef = theLoader.loadClass(className);
         definitions.put(name,cdef);
      }
   }

   protected void loadInternal(String shippedAgent,Element top)
      throws Exception
   {
      for (Element child : DocumentLoader.getElementChildren(top)) {
         if (DEFINE.equals(child)) {
            loadDefinition(child);
         } else if (CLIENT.equals(child)) {
            String protocol = DocumentLoader.getAttributeValue(child,"protocol");
            if (protocol!=null) {
               Protocol p = Protocol.valueOf(protocol.trim());
               clients.add(p);
            } 
         } else if (APPLICATION.equals(child)) {
            App app = createApplication(child);
            applications.add(app);
         } else if (SERVER.equals(child)) {
            String addr = DocumentLoader.getAttributeValue(child,"address");
            String portS = DocumentLoader.getAttributeValue(child,"port");
            String protocol = DocumentLoader.getAttributeValue(child,"protocol");
            String agent = DocumentLoader.getAttributeValue(child,"agent");
            Protocol p = Protocol.HTTP;
            if (protocol!=null) {
               p = Protocol.valueOf(protocol.trim());
            } 
            int port = portS==null ? p.getDefaultPort() : Integer.parseInt(portS);
            Server server = new Server(addr,port,p);
            servers.add(server);
            if (agent!=null) {
               server.setAgent(agent);
            }
            if (shippedAgent!=null) {
               server.setAgent(shippedAgent);
            }
            loadContext(server.getContext(),child);

            for (Element hostE : DocumentLoader.getElementsByName(child,HOST)) {
               Host host = createHost(hostE);
               server.getHosts().put(host.getName(),host);
            }
            
         } else if (INCLUDE.equals(child)) {
            String href = DocumentLoader.getAttributeValue(child,"href");
            if (href==null) {
               continue;
            }
            URI location = DocumentLoader.resolve(child.getBaseURI(),href); 
            try {
               Document included = docLoader.load(location);
               Element includedTop = included.getDocumentElement();
               if (COMPONENT.equals(includedTop)) {
                  loadInternal(shippedAgent,includedTop);
               } else if (DEFINE.equals(includedTop)) {
                  loadDefinition(includedTop);
               } else {
                  logSevere(child,"Included document starts with unrecognized element "+DocumentLoader.getName(includedTop));
                  continue;
               }
            } catch (Exception ex) {
               LOG.log(Level.SEVERE,"Cannot load included document: "+location,ex);
            }
            
         }
      }
      
   }
   
   public List<Protocol> getClients() {
      return clients;
   }

   public List<Server> getServers() {
      return servers;
   }
   
   public List<App> getApplications() {
      return applications;
   }

   protected void loadContext(Context appContext,Element useConf)
   {
      LOG.fine("Loading context: "+appContext);

      for (Element child : DocumentLoader.getElementChildren(useConf)) {
         if (ATTRIBUTE.equals(child)) {
            Element attrE = child;
            String name = DocumentLoader.getAttributeValue(attrE,"name");
            String value = DocumentLoader.getAttributeValue(attrE,"value");
            String href = DocumentLoader.getAttributeValue(attrE,"href");
            if (href!=null) {
               value = DocumentLoader.resolve(attrE.getBaseURI(),href).toString();
            }
            Class def = getTargetClass(attrE);
            if (value!=null) {
               LOG.fine("Attribute: "+name+"="+value);
               appContext.getAttributes().put(name,value);
            } else if (def!=null) {
               try {
                  Object obj = null;
                  try {
                     Constructor<Object> makeit = def.getConstructor(Context.class);
                     obj = makeit.newInstance(appContext);
                  } catch (NoSuchMethodException ex) {
                     Constructor<Restlet> makeit = def.getConstructor();
                     obj = makeit.newInstance();
                  }
                  LOG.fine("Attribute: "+name+"="+obj);
                  appContext.getAttributes().put(name,obj);
               } catch (Exception ex) {
                  LOG.log(Level.SEVERE,"Cannot instantiate "+def.getName()+" for attribute "+name,ex);
               }
            } else {
               List<Object> values = new ArrayList<Object>();
               for (Element attChild : DocumentLoader.getElementChildren(attrE)) {
                  if (PARAMETER.equals(attChild)) {
                     String pname = DocumentLoader.getAttributeValue(attChild,"name");
                     if (pname!=null) {
                        String pvalue = DocumentLoader.getAttributeValue(attChild,"value");
                        String phref = DocumentLoader.getAttributeValue(attChild,"href");
                        String use = DocumentLoader.getAttributeValue(attChild,"use");
                        if (phref!=null) {
                           URI uri = DocumentLoader.resolve(attChild.getBaseURI(),phref);
                           pvalue = "path".equals(use) ? uri.getPath() : uri.toString();
                        }
                        if (pvalue==null) {
                           pvalue = "";
                        }
                        values.add(new Parameter(pname,pvalue));
                     }
                  } else {
                     // Switch to DOM
                     try {
                        Document doc = docBuilder.newDocument();
                        doc.appendChild(doc.importNode(attChild, true));
                        values.add(doc);
                     } catch (Exception ex) {
                        LOG.log(Level.SEVERE,"Cannot serialize attribute XML.",ex);
                     }

                  }
               }
               LOG.fine("Attribute: "+name+" is list of values.");
               appContext.getAttributes().put(name,values);
            }
         } else if (PARAMETER.equals(child)) {
            Element paramE = child;
            String pname = DocumentLoader.getAttributeValue(paramE,"name");
            if (pname!=null) {
               String value = DocumentLoader.getAttributeValue(paramE,"value");
               String href = DocumentLoader.getAttributeValue(paramE,"href");
               String use = DocumentLoader.getAttributeValue(paramE,"use");
               if (href!=null) {
                  URI uri = DocumentLoader.resolve(paramE.getBaseURI(),href);
                  value = "path".equals(use) ? uri.getPath() : uri.toString();
               }
               String resource = DocumentLoader.getAttributeValue(paramE,"resource");
               String ref = DocumentLoader.getAttributeValue(paramE,"ref");
               LOG.fine("resource="+resource+", ref="+ref);
               if (resource!=null && ref!=null) {
                  ClassLoader loader = loaders.get(ref);
                  if (loader==null) {
                     Class targetClass = definitions.get(ref);
                     if (targetClass!=null) {
                        targetClass.getClassLoader();
                     }
                  }
                  if (loader!=null) {
                     URL uvalue = loader.getResource(resource);
                     if (uvalue!=null) {
                        value = uvalue.toString();
                     }
                  } else {
                     logSevere(child,"Cannot find definition of "+ref);
                  }
               }
               if (value==null) {
                  value = "";
               }
               LOG.fine("Setting parameter "+pname+"="+value);
               if ("true".equals(DocumentLoader.getAttributeValue(paramE,"replace"))) {
                  Parameter toRemove;
                  while ((toRemove = appContext.getParameters().getFirst(pname))!=null) {
                     appContext.getParameters().remove(toRemove);
                  }
               }
               appContext.getParameters().add(pname,value);
            }

         } else if (INCLUDE.equals(child)) {
            String href = DocumentLoader.getAttributeValue(child,"href");
            if (href==null) {
               continue;
            }
            URI location = DocumentLoader.resolve(child.getBaseURI(),href); 
            try {
               Document included = docLoader.load(location);
               Element top = included.getDocumentElement();
               if (!CONTEXT.equals(top)) {
                  continue;
               }
               loadContext(appContext,top);
            } catch (Exception ex) {
               LOG.log(Level.SEVERE,"Cannot load included document: "+location,ex);
            }
         }
      }

   }

   Class getTargetClass(Element child) {
      String ref = DocumentLoader.getAttributeValue(child,"ref");
      String className = DocumentLoader.getAttributeValue(child,"class");
      if (ref!=null && className!=null) {
         ClassLoader loader = loaders.get(ref.trim());
         if (loader==null) {
            logSevere(child,"Cannot find a class loader definition for '"+ref+"'");
            return null;
         }
         LOG.fine("ClassLoader: "+loader);
         try {
            return loader.loadClass(className);
         } catch (Exception ex) {
            LOG.log(Level.SEVERE,"Cannot load class '"+className+"' via "+ref,ex);
         }
      } else if (ref!=null) {
         Class def = definitions.get(ref.trim());
         if (def==null) {
            logSevere(child,"Cannot find a class definition for '"+ref+"'");
         }
         return def;
      } else if (className!=null) {
         try {
            return this.getClass().getClassLoader().loadClass(className.trim());
         } catch (Exception ex) {
            LOG.log(Level.SEVERE,"Cannot load class '"+className+"'",ex);
         }
      }
      return null;
   }

}
