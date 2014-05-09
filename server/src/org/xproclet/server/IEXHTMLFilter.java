/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.server;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class IEXHTMLFilter extends Filter {
   
   public IEXHTMLFilter(Context context) {
      super(context);
   }
   
   protected void afterHandle(Request request, Response response) {
      Representation entity = response.getEntity();
      if (entity!=null && entity.getMediaType().equals(MediaType.APPLICATION_XHTML)) {
         if (request.getClientInfo().getAgent().contains("MSIE") && !request.getClientInfo().getAgent().contains("MSIE 9.0")) {
            entity.setMediaType(MediaType.TEXT_HTML);
         }
      }
   }
}
