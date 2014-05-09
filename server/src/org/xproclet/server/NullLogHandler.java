/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.xproclet.server;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 *
 * @author alex
 */
public class NullLogHandler extends Handler {

   public NullLogHandler() {

   }

   public void publish(LogRecord record) {}
   public void flush() {}
   public void close() {}
}
