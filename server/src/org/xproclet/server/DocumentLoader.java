/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.io.Reader;
import java.net.URI;
import java.util.Iterator;
import java.util.Stack;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.events.MutationEvent;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.LocatorImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 *
 * @author alex
 */
public class DocumentLoader {

   public static class Name {
      String localName;
      String namespaceURI;
      public Name(String namespaceURI,String localName) {
         this.namespaceURI = namespaceURI;
         this.localName = localName;
      }
      public Name(String localName) {
         this.namespaceURI = null;
         this.localName = localName;
      }
      public String getLocalName() {
         return localName;
      }
      public String getNamespaceURI() {
         return namespaceURI;
      }
      public String toString() {
         return "{"+namespaceURI+"}"+localName;
      }
      public boolean equals(Object o) {
         if (o instanceof Element) {
            Element e = (Element)o;
            return e.getLocalName().equals(localName) && 
                   ((e.getNamespaceURI()==null && namespaceURI==null) || (namespaceURI!=null && namespaceURI.equals(e.getNamespaceURI())));
         } else if (o instanceof Name) {
            Name n = (Name)o;
            return n.localName.equals(localName) && 
                  ((namespaceURI==null && n.namespaceURI==null) || (namespaceURI!=null && namespaceURI.equals(n.namespaceURI)));
         } else {
            return super.equals(o);
         }
      }
   }
   
   public static Name getName(Element e) {
      return new Name(e.getNamespaceURI(),e.getLocalName());
   }
   
   public static Iterable<Element> getElementChildren(final Element parent)
   {
      return new Iterable<Element>() {
         public Iterator<Element> iterator() {
            return new Iterator<Element>() {
               Element next = null;
               Node child = parent.getFirstChild();
               public boolean hasNext() {
                  while (child!=null && next==null) {
                     do {
                        child = child.getNextSibling();
                     } while (child!=null && child.getNodeType()!=Node.ELEMENT_NODE);
                     next = (Element)child;
                  }
                  return next!=null;
               }
               public Element next() {
                  if (next!=null) {
                     Element value = next;
                     next = null;
                     return value;
                  }
                  return hasNext() ? next() : null;
               }
               public void remove() {
                  throw new UnsupportedOperationException("remove() is not supported");
               }
            };
         }
      };
   }
   
   public static Iterable<Element> getElementsByName(final Element parent,final Name name)
   {
       return new Iterable<Element>() {
         public Iterator<Element> iterator() {
            return new Iterator<Element>() {
                Element next = null;
                Node child = parent.getFirstChild();
                public boolean hasNext() {
                   while (child!=null && next==null) {
                      do {
                         child = child.getNextSibling();
                      } while (child!=null && child.getNodeType()!=Node.ELEMENT_NODE);
                      if (child!=null) {
                         next = (Element)child;
                         if (!name.equals(next)) {
                            next = null;
                            child = child.getNextSibling();
                         }
                      }
                   }
                   return next!=null;
                }
                public Element next() {
                   if (next!=null) {
                      Element value = next;
                      next = null;
                      return value;
                   }
                   return hasNext() ? next() : null;
                }
                public void remove() {
                   throw new UnsupportedOperationException("remove() is not supported");
                }
             };
          }
       };
   }
   
   public static String getAttributeValue(Element parent,String name)
   {
      Attr node = parent.getAttributeNode(name);
      return node!=null ? node.getValue() : null;
   }
   
   public static URI resolve(String baseURI,String href) {
      URI location = URI.create(baseURI);
      return location.resolve(href);
   }
   
   public static String getLocation(Node n)
   {
      DocumentLoader.LocationData location = (DocumentLoader.LocationData)n.getUserData(DocumentLoader.LocationData.LOCATION_DATA_KEY);
      return location!=null ? n.getBaseURI()+":"+location.getStartLine()+":"+location.getStartColumn() : n.getBaseURI();
   }   
   // location tracking from http://javacoalface.blogspot.com/2011/04/line-and-column-numbers-in-xml-dom.html
   public static class LocationData {
      public static final String LOCATION_DATA_KEY = "location";

       private final String systemId;
       private final int startLine;
       private final int startColumn;
       private final int endLine;
       private final int endColumn;

       public LocationData(String systemId, int startLine,
               int startColumn, int endLine, int endColumn) {
           super();
           this.systemId = systemId;
           this.startLine = startLine;
           this.startColumn = startColumn;
           this.endLine = endLine;
           this.endColumn = endColumn;
       }

       public String getSystemId() {
           return systemId;
       }

       public int getStartLine() {
           return startLine;
       }

       public int getStartColumn() {
           return startColumn;
       }

       public int getEndLine() {
           return endLine;
       }

       public int getEndColumn() {
           return endColumn;
       }

       @Override
       public String toString() {
           return getSystemId() + "[line " + startLine + ":"
                   + startColumn + " to line " + endLine + ":"
                   + endColumn + "]";
       }
   }

   class LocationAnnotator extends XMLFilterImpl {

       private Locator locator;
       private Element lastAddedElement;
       private Stack<Locator> locatorStack = new Stack<Locator>();
       private UserDataHandler dataHandler = new LocationDataHandler();

       LocationAnnotator(XMLReader xmlReader, Document dom) {
           super(xmlReader);

           // Add listener to DOM, so we know which node was added.
           EventListener modListener = new EventListener() {
               @Override
               public void handleEvent(Event e) {
                   EventTarget target = ((MutationEvent) e).getTarget();
                   lastAddedElement = (Element) target;
               }
           };
           ((EventTarget) dom).addEventListener("DOMNodeInserted",
                   modListener, true);
       }

       @Override
       public void setDocumentLocator(Locator locator) {
           super.setDocumentLocator(locator);
           this.locator = locator;
       }

       @Override
       public void startElement(String uri, String localName,
               String qName, Attributes atts) throws SAXException {
           super.startElement(uri, localName, qName, atts);

           // Keep snapshot of start location,
           // for later when end of element is found.
           locatorStack.push(new LocatorImpl(locator));
       }

       @Override
       public void endElement(String uri, String localName, String qName)
               throws SAXException {

           // Mutation event fired by the adding of element end,
           // and so lastAddedElement will be set.
           super.endElement(uri, localName, qName);

           if (locatorStack.size() > 0) {
               Locator startLocator = locatorStack.pop();

               LocationData location = new LocationData(
                       startLocator.getSystemId(),
                       startLocator.getLineNumber(),
                       startLocator.getColumnNumber(),
                       locator.getLineNumber(),
                       locator.getColumnNumber());

               lastAddedElement.setUserData(
                       LocationData.LOCATION_DATA_KEY, location,
                       dataHandler);
           }
       }

       // Ensure location data copied to any new DOM node.
       private class LocationDataHandler implements UserDataHandler {

           @Override
           public void handle(short operation, String key, Object data,
                   Node src, Node dst) {

               if (src != null && dst != null) {
                   LocationData locatonData = (LocationData)
                           src.getUserData(LocationData.LOCATION_DATA_KEY);

                   if (locatonData != null) {
                       dst.setUserData(LocationData.LOCATION_DATA_KEY,
                               locatonData, dataHandler);
                   }
               }
           }
       }
   }
   
   DocumentBuilderFactory documentBuilderFactory;
   TransformerFactory transformerFactory;
   Transformer nullTransformer;
   
   public DocumentLoader() 
   {
      documentBuilderFactory = DocumentBuilderFactory.newInstance();
      transformerFactory = TransformerFactory.newInstance();
      try {
         nullTransformer = transformerFactory.newTransformer();      
      } catch (TransformerConfigurationException ex) {
         throw new RuntimeException(ex.getMessage(),ex);
      }
   }
   
   public Document load(InputSource inputSource)
      throws Exception
   {
      DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();
      DOMResult domResult = new DOMResult(doc);      
      
      SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setNamespaceAware(true);
      SAXParser saxParser = saxParserFactory.newSAXParser();
      XMLReader xmlReader = saxParser.getXMLReader();
      
      LocationAnnotator locationAnnotator = new LocationAnnotator(xmlReader, doc);

      SAXSource saxSource = new SAXSource(locationAnnotator, inputSource);
      
      nullTransformer.transform(saxSource, domResult);
      
      return doc;
   }
           
   public Document load(URI location) 
      throws Exception
   {
      InputSource inputSource = new InputSource(location.toString());
      Document doc = load(inputSource);
      doc.setDocumentURI(location.toString());
      
      return doc;
   }
   
   public Document load(Reader reader)
      throws Exception
   {
      InputSource inputSource = new InputSource(reader);
      return load(inputSource);
   }
}
