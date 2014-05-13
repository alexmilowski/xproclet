<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="head-collection-feed-media">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='id'/>
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
<p:with-param name='id' select='$id'/>
<p:with-param name='app.path' select='$app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare namespace prop="http://marklogic.com/xdmp/property";
declare variable $name external;
declare variable $id external;
declare variable $app.path external;
 let $uri := concat('app://collections/',$name,'/entry/',$id,'.atom'), $d := document($uri)
    return
    if ($d) then
       let $last-modified := xdmp:document-properties($uri)/prop:properties/prop:last-modified
          return
          element http:http {
             attribute status { "200" },
             element http:header {
                attribute name { "Cache-Control" },
                "no-cache"
             },
             element http:entity {
                attribute type { "application/atom+xml;type=entry"  },
                attribute last-modified { string($last-modified) }
             }
          }
   else element http:http { attribute status {"404"}, element http:entity { concat("Cannot find entry: ",$app.path,"/collections/",$name,"/_/",$id,'.atom') } }


</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>