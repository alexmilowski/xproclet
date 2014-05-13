<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="update-collection-feed-media">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='file'/>
<p:input port="source"/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:wrap wrapper="doc" match="/"/>
<p:escape-markup/>

<p:template>
<p:input port="template">
<p:inline>
<c:query>
declare boundary-space preserve;
declare namespace app='http://www.w3.org/2007/app';
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare variable $name external;
declare variable $file external;
let $uri := concat('app://collections/',$name,'/media/',$file), $d := document($uri)
   return if ($d) 
          then element http:http {{ attribute status {{ "204" }}, xdmp:node-insert-child($d/*,{doc/text()}) }}
          else element http:http {{ attribute status {{ "404" }}, element http:entity {{ "No collection media found named",$file }} }}

</c:query>
</p:inline>
</p:input>
<p:input port="parameters"><p:inline><c:param-set/></p:inline></p:input>
</p:template>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='name' select='$name'/>
<p:with-param name='file' select='$file'/>
</ml:adhoc-query>

</p:declare-step>