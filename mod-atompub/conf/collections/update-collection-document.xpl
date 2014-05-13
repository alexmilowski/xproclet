<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="update-collection-document">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:input port="source"/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:choose>
<p:when test="/app:collection">

<p:wrap wrapper="doc" match="/"/>
<p:escape-markup/>

<p:template>
<p:input port="template">
<p:inline>
<c:query>
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare variable $name external;
let $uri := concat('app://collections/',$name,'.col'), $d := document($uri)/app:collection
   return if ($d) 
          then element http:http {{ 
             attribute status {{ "204" }}, 
             xdmp:node-replace($d,{doc/text()}), 
             let $updated := collection('app://collections/')/atom:updated
                return if ($updated)
                       then xdmp:node-replace($updated,element atom:updated {{ current-dateTime() }})
                       else xdmp:document-insert(
                                'app://collections/updated.xml',
                                element atom:updated {{ current-dateTime() }},
                                xdmp:default-permissions(),
                                ('app://collections/'))
          }}
          else element http:http {{ attribute status {{ "404" }}, element http:entity {{ "No collection found named",$name }} }}

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
</ml:adhoc-query>

</p:when>
<p:otherwise>
   <p:template>
      <p:input port="template">
      <p:inline>
      <http xmlns='http://www.xproclet.org/V/HTTP/' status='400'>
      <entity type='text/plain'>Unrecognized element {{{namespace-uri(/*)}}}{local-name(/*)}</entity>
      </http>
      </p:inline>
      </p:input>
      <p:input port="parameters"><p:inline><c:param-set/></p:inline></p:input>
   </p:template>
</p:otherwise>
</p:choose>


</p:declare-step>