/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.xproclet.xproc;

import com.xmlcalabash.runtime.XPipeline;
import java.net.URI;

/**
 *
 * @author alex
 */
public class XPipelineContext {

   long timestamp;
   XPipeline pipeline;
   URI location;
   
   XPipelineContext(URI location,XPipeline pipeline, long timestamp)
   {
      this.location = location;
      this.pipeline = pipeline;
      this.timestamp = timestamp;
   }
   
   public URI getLocation() {
      return location;
   }
   
   public XPipeline getPipeline() {
      return pipeline;
   }
   
   public long getTimestamp() {
      return timestamp;
   }
}
