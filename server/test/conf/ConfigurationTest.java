/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conf;

import java.net.URI;
import java.util.logging.Logger;
import org.xproclet.server.Configuration;
import org.xproclet.server.Configuration.Host;
import org.xproclet.server.Configuration.Server;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;

/**
 *
 * @author alex
 */
public class ConfigurationTest {
   
   static Logger LOG = Logger.getLogger(ConfigurationTest.class.getName());
   static String [] tests = { "attributes.xml", "basic.xml", "basic-secure.xml", "basic-8080.xml", "basic-secure-8081.xml", "parameters.xml", "redirect-internal.xml", "redirect.xml", "top-level-routing.xml" };
      
   public ConfigurationTest() {
   }

   @BeforeClass
   public static void setUpClass() throws Exception {
   }

   @AfterClass
   public static void tearDownClass() throws Exception {
   }
   
   @Before
   public void setUp() {
   }
   
   @After
   public void tearDown() {
   }
   
   @Test
   public void testLoad() {
      for (int i=0; i<tests.length; i++) {
         Configuration conf = new Configuration();
         try {
            URI uri = getClass().getResource(tests[i]).toURI();
            LOG.info("Loading: "+uri);
            conf.load(uri);
            for (Protocol client : conf.getClients()) {
               LOG.info("Client: "+client);
            }
            for (Server server : conf.getServers()) {
               LOG.info("Server: "+server.getProtocol()+" at "+server.getAddress()+":"+server.getPort());
               for (Host host : server.getHosts().values()) {
                  LOG.info("Host: "+host.getName());
                  Context context = new Context();
                  Router router = new Router(context);
                  host.attach(router);
               }
            }
         } catch (Exception ex) {
            ex.printStackTrace();
            org.junit.Assert.fail();
         }
      }
   }
}
