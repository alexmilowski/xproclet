/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import org.xproclet.xproc.XProcRestlet;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;

/**
 *
 * @author alex
 */
public class XQueryRestlet extends XProcRestlet {
   final String XQUERY_BASE_PARAMETER = "xquery.base.href";
   final String XQUERY_CONTEXT_ATTR = "xquery.context";
   MediaType xqueryMediaType;
   URI baseRef;
   Class contextClass;
   public XQueryRestlet(Context context) {
      super(context);
      String href = context.getParameters().getFirstValue(XQUERY_BASE_PARAMETER);
      if (href!=null) {
         try {
            baseRef = href==null ? null : new URI(href);
         } catch (URISyntaxException ex) {
            getLogger().warning("Bad URI value for "+XQUERY_BASE_PARAMETER+" "+href+", "+ex.getMessage());
         }
      }
      xqueryMediaType = new MediaType("application/xquery");
      Object o = context.getAttributes().get(XQUERY_CONTEXT_ATTR);
      contextClass = o==null ? this.getClass() : o.getClass();
   }
   
   public void handle(Request request, Response response) {
      if (request.isEntityAvailable()) {
         if (xqueryMediaType.equals(request.getEntity().getMediaType(), true)) {
            try {
               StringBuilder query = new StringBuilder();
               query.append("<c:query xmlns:c='http://www.w3.org/ns/xproc-step'>");
               escape(query,request.getEntity().getReader());
               query.append("</c:query>");
               if (getLogger().isLoggable(Level.FINE)) {
                  getLogger().fine("Query: "+query.toString());
               }
               request.setEntity(new StringRepresentation(query.toString(),MediaType.APPLICATION_XML));
            } catch (IOException ex) {
               getLogger().warning("Unable to read entity while processing query.");
               response.setStatus(Status.CLIENT_ERROR_UNPROCESSABLE_ENTITY);
               return;
            }
         } else {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
            response.setEntity(new StringRepresentation("Invalid media type: "+request.getEntity().getMediaType(),MediaType.TEXT_PLAIN));
            return;
         }
      } else {
         String xquery = request.getResourceRef().getQueryAsForm().getFirstValue("xquery");
         if (xquery!=null) {
            try {
               URI xqueryRef = baseRef==null ? new URI(xquery) : baseRef.resolve(xquery);
               if (!xqueryRef.isAbsolute()) {
                  URL resource = contextClass.getResource(xquery);
                  if (resource==null) {
                     getLogger().warning("Cannot find xquery resource "+xquery);
                     response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
                     return;
                  }
                  xqueryRef = resource.toURI();
               }
               // TODO: cache
               // TODO: handle other encodings
               Reader reader = new InputStreamReader(xqueryRef.toURL().openStream(),"UTF-8");
               StringBuilder query = new StringBuilder();
               query.append("<c:query xmlns:c='http://www.w3.org/ns/xproc-step'>");
               escape(query,reader);
               query.append("</c:query>");
               if (getLogger().isLoggable(Level.FINE)) {
                  getLogger().fine("Query: "+query.toString());
               }
               request.setEntity(new StringRepresentation(query.toString(),MediaType.APPLICATION_XML));
            } catch (URISyntaxException ex) {
               getLogger().warning("Bad URI xquery parameter: "+xquery);
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               return;
            } catch (IOException ex) {
               getLogger().warning("Error reading xquery "+xquery);
               response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
               return;
            }
         }
      }
      super.handle(request, response);
   }
   
   protected void escape(StringBuilder out,Reader query) 
      throws IOException
   {
      char [] buffer = new char[4096];
      int len;
      while ((len=query.read(buffer))>0) {
         int start = 0;
         for (int i=0; i<len; i++) {
            if (buffer[i]=='<') {
               if (start!=i) {
                  out.append(buffer,start,i-start);
               }
               out.append("&lt;");
               start = i+1;
            } else if (buffer[i]=='&') {
               if (start!=i) {
                  out.append(buffer,start,i-start);
               }
               out.append("&amp;");
               start = i+1;
            }
         }
         if (start!=len) {
            out.append(buffer,start,len-start);
         }
      }
   }
}
