<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="delete-collection-feed-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='id'/>
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
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $name external;
declare variable $id external;
 let $uri := concat('app://collections/',$name,'/entry/',$id,'.atom'), $d := document($uri)
    return if ($d) then element http:http {
              attribute status { "204" },
              if ($d/atom:entry/atom:content/@src)
              then let $muri := concat('app://collections/',$name,'/media/',$d/atom:entry/atom:content/@src), $media := document($muri)
                      return if ($media) 
                             then xdmp:document-delete($muri) 
                             else element http:attribute {
                                attribute name { "org.xproclet.atompub.media.src" },
                                attribute value { $d/atom:entry/atom:content/@src }
                             }
              else (),
              xdmp:document-delete($uri)
           }
           else element http:http { attribute status {"404"}, element http:entity { concat("There is no collection named ",$name,".") } }


</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>