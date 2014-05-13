<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="head-collection-feed">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='app.path'/>
<p:output port="result"/>
<p:serialization port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='name' select='$name'/>
<p:with-param name='app.path' select='$app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $name external;
declare variable $app.path external;
let $uri := concat('app://collections/',$name,'.col'), $d := document($uri)
   return if ($d)
          then element http:http { 
                  attribute status {"200"}, 
                  element http:header {
                     attribute name { "Cache-Control" },
                     "no-cache"
                  },
                  element http:entity {
                     attribute type { "application/atom+xml;type=feed" },
                     attribute last-modified { 
                        string(let $curi := concat('app://collections/',$name,'/entry')
                           return (for $e in collection($curi)/atom:entry
                                   order by $e/app:edited descending return $e)[1]/app:edited)
                     }
                  }
               }
          else element http:http { attribute status {"404"}, element http:entity { concat("There is no collection named ",$name,".") } }
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>