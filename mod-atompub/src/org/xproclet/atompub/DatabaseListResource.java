/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub;

import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ServerResource;

/**
 *
 * @author alex
 */
public class DatabaseListResource extends ServerResource {
   
   String [] databases;
   public DatabaseListResource() {
      
   }
   
   protected void doInit() {
      String list = getContext().getParameters().getFirstValue("app.databases");
      if (list!=null) {
         databases = list.split(",");
      } else {
         databases = null;
      }
   }
   
   protected Representation get() {
      StringBuilder xml = new StringBuilder();
      xml.append("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>Databases</title></head><body>\n");
      if (databases!=null) {
         for (int i=0; i<databases.length; i++) {
            xml.append("<div about=\"#"+databases[i]+"\"><h2 property=\"dc:title\">"+databases[i]+"</h2>");
            xml.append("<a rel=\"related\" href=\"./"+databases[i]+"/\">Service</a>");
            xml.append(" ");
            xml.append("<a rel=\"related\" href=\"./"+databases[i]+"/collections/\">Collections</a>");
            xml.append(" ");
            xml.append("<a rel=\"edit\" href=\"./"+databases[i]+"/edit/\">Edit</a>");
            xml.append("</div>\n");
         }
      }
      xml.append("</body></html>");
      return new StringRepresentation(xml.toString(),MediaType.APPLICATION_XHTML);
   }
   
}
