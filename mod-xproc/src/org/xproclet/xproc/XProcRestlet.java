/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import java.net.URI;
import java.net.URISyntaxException;
import net.sf.saxon.s9api.QName;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.engine.header.Header;
import org.restlet.util.Series;

/**
 *
 * @author alex
 */
public class XProcRestlet extends Restlet{
   
   XProcHelper xprocHelper;
      
   public XProcRestlet(Context context) {
      super(context);
      xprocHelper = new XProcHelper(context);
   }
   
   public void handle(Request request, Response response) {
      xprocHelper.handle(true,request, response);
   }
   
   protected XProcCache getCache() {
      return xprocHelper.getCache();
   }
   
   protected URI resolve(URI baseURI,String href) 
      throws URISyntaxException
   {
      return xprocHelper.resolve(baseURI, href);
   }
   
   protected String getHeaderValue(String headerName,Request request,Series<Header> headers) {
      return xprocHelper.getHeaderValue(headerName, request, headers);
   }
   
   protected String getParameterValue(String name)
   {
      return xprocHelper.getParameterValue(name);
   }
   
   protected String getAttributeValue(Request request,String attributeName)
   {
      return xprocHelper.getAttributeValue(request, attributeName);
   }
   
   protected String getRequestValue(Request request, String facetName)
   {
      return xprocHelper.getRequestValue(request, facetName);
   }
   
   protected String getOptionValue(Request request,QName name)
   {
      return xprocHelper.getOptionValue(request, name);
   }
}
