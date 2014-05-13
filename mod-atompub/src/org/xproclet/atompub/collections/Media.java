/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.atompub.collections;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;

/**
 *
 * @author alex
 */
public interface Media extends MediaMetadata {
   
   Representation getRepresentation();
}
