/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.server;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.engine.header.Header;
import org.restlet.routing.Filter;
import org.restlet.util.Series;

/**
 *
 * @author alex
 */
public class CORSFilter extends Filter {

   protected String origin;
   protected String allowHeaders;
   
   public CORSFilter(Context context) {
      super(context);
   }
   public void start() 
      throws Exception
   {
      super.start();
      
      origin = getContext().getParameters().getFirstValue("Allow-Origin");
      if (origin==null) {
         origin = "*";
      }
      
      allowHeaders = getContext().getParameters().getFirstValue("Allow-Headers");
      if (allowHeaders==null) {
         allowHeaders = "accept-encoding,cache-control";
      }
   }
   protected void addAccessControl(Response response) {
      Series<Header> responseHeaders = (Series<Header>)response.getAttributes().get("org.restlet.http.headers");
      if (responseHeaders==null) {
         responseHeaders = new Series<Header>(Header.class);
         response.getAttributes().put("org.restlet.http.headers",responseHeaders);
      }
      responseHeaders.add("Access-Control-Allow-Origin", origin);
      responseHeaders.add("Access-Control-Allow-Headers",allowHeaders);
   }
   protected int beforeHandle(Request request,Response response) {
      if (request.getMethod().equals(Method.OPTIONS)) {
         response.setStatus(Status.SUCCESS_NO_CONTENT);
         addAccessControl(response);
         return Filter.STOP;
      }
      return Filter.CONTINUE;
   }
   
   protected void afterHandle(Request request, Response response) {
      addAccessControl(response);
   }
}
