/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.util.Date;
import org.restlet.data.MediaType;

/**
 *
 * @author alex
 */
public interface MediaMetadata {
   
   String getName();
   
   Date getLastModified();
   
   MediaType getContentType();
   
   long getSize();
}
