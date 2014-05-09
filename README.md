XProclet is a web server based on [Restlet](http://www.restlet.org/) that enables simple ways to run [XProc](http://www.w3.org/TR/XProc) pipelines (via [Calabash](http://xmlcalabash.com)) along side other web applications.  The use of the Restlet project allows XProclet to run any Restlet-based application as well as run within a standard J2EE servlet container or as a standalone server.

For example, to simply serve up files from a directory, the configuration (web.xml) is simple:

    <?xml version="1.0" encoding="UTF-8"?>
    <component xmlns="http://www.xproclet.org/V/Server/">
    
       <client protocol="CLAP"/>
       <client protocol="FILE"/>
       <client protocol="HTTP"/>
    
       <server protocol="HTTP" address="*">
          <host name="*">
             <content match="/" href="web" index="index.xhtml"/>
          </host>
       </server>
    </component>
    
then you can run the server by simply:

    server web.xml

More importantly, running XProc as a web service is very easy:

    <?xml version="1.0" encoding="UTF-8"?>
    <component xmlns="http://www.xproclet.org/V/Server/">
    
       <client protocol="CLAP"/>
       <client protocol="FILE"/>
       <client protocol="HTTP"/>
    
       <include href="../dist/mod-xproc/mod-xproc.xml"/>
       
       <server protocol="HTTP" address="*" port="8080">
          <host name="*">
             <attribute name="xproc.cache" class="org.xproclet.xproc.XProcCache" ref="xproc"/>
             <route match="/test/" ref="xproc">
                <parameter name="xproc.href" href="test.xpl"/>
             </route>
          </host>
       </server>
    </component>
    
The server configuration will load the necessary libraries and compile the pipeline using Calabash.  The configuration allows associating pipelines with different HTTP methods, mapping headers and parameters to options, and binding outputs to responses.  In this simple example, the pipeline responds to a GET request on `/test/`.

There is a full compliment of Restlet-oriented features that allows configuration and use of libraries, built-in or custom, of resources, applications, and other constructs.  As necessary, a developer can "escape to Java" and build specialized components.  The routing, filtering, and handling of requests can then all be configured in the markup.
