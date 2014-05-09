/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.routing.Filter;

/**
 *
 * @author alex
 */
public class XProcResponseFilter extends Filter{
   
   XProcHelper xprocHelper;
   public XProcResponseFilter(Context context) {
      super(context);
      xprocHelper = new XProcHelper(context);
   }

   protected void afterHandle(Request request, Response response) {
      xprocHelper.handle(false, request, response);
   }
}
