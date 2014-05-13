<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:a='http://www.xproclet.org/atompub/local'
                version="1.0"
                name="create-collection">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='slug'/>
<p:input port="source"/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:declare-step type="a:generate-uuid">
<p:output port="result" primary="true"/>
<p:option name='user'/>
<p:option name='password'/>
<p:option name='host'/>
<p:option name='port'/>
<ml:adhoc-query>
<p:with-option name='user' select='$user'/>
<p:with-option name='password' select='$password'/>
<p:with-option name='host' select='$host'/>
<p:with-option name='port' select='$port'/>
<p:input port="parameters" kind="parameter"><p:inline><c:param-set/></p:inline></p:input>
<p:input port="source">
<p:inline>
<c:query>
   declare function local:random-hex(
     $length as xs:integer
   ) as xs:string {
     string-join(
       for $n in 1 to $length
       return xdmp:integer-to-hex(xdmp:random(15)),
       ""
     )
   };

   declare function local:generate-uuid-v4() as xs:string {
     string-join(
       (
         local:random-hex(8),
         local:random-hex(4),
         local:random-hex(4),
         local:random-hex(4),
         local:random-hex(12)
       ),
       "-"
     )
   };

   (: Query :)

   element uuid { local:generate-uuid-v4() }
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>
</p:declare-step>

<p:choose>
<p:when test="/app:collection">
   <!--
   <p:choose name="generate-name">
      <p:when test="p:value-available('slug')">
         <p:output port='result'/>
         <p:template>
            <p:with-param name="name" select="$slug"/>
            <p:input port='template'>
               <p:inline>
                  <name>{$name}</name>
               </p:inline>
            </p:input>
            <p:input port="source"><p:inline><doc/></p:inline></p:input>
         </p:template>
      </p:when>
      <p:otherwise>
         <p:output port='result'/>
         <a:generate-uuid name="uuid">
            <p:with-option name='user' select='$xdb.user'/>
            <p:with-option name='password' select='$xdb.password'/>
            <p:with-option name='host' select='$xdb.host'/>
            <p:with-option name='port' select='$xdb.port'/>
         </a:generate-uuid>
         <p:template>
            <p:input port='parameters'><p:inline><c:param-set/></p:inline></p:input>
            <p:input port='template'>
               <p:inline>
                  <name>{string(/uuid)}</name>
               </p:inline>
            </p:input>
         </p:template>
      </p:otherwise>
   </p:choose>
   -->
   <p:group>
      <p:variable name="name" select="$slug"/>
<!--
      <p:variable name="name" select="/name">
         <p:pipe step="generate-name" port="result"/>
      </p:variable>
-->

      <ml:adhoc-query>
         <p:with-option name='user' select='$xdb.user'/>
         <p:with-option name='password' select='$xdb.password'/>
         <p:with-option name='host' select='$xdb.host'/>
         <p:with-option name='port' select='$xdb.port'/>
         <p:with-param name='name' select='$name'/>
         <p:input port="source">
         <p:inline>
         <c:query>
         declare namespace http="http://www.xproclet.org/V/HTTP/";
         declare variable $name external;
          let $uri := concat('app://collections/',$name,'.col'), $d := document($uri)
             return if ($d) then &lt;yes/> else &lt;no/>
         </c:query>
         </p:inline>
         </p:input>
      </ml:adhoc-query>

      <p:choose>
      <p:when test="/no">
         <ml:insert-document collections="app://collections/" name="insert">
            <p:with-option name='user' select='$xdb.user'/>
            <p:with-option name='password' select='$xdb.password'/>
            <p:with-option name='host' select='$xdb.host'/>
            <p:with-option name='port' select='$xdb.port'/>
            <p:with-option name="uri" select="concat('app://collections/',$name,'.col')"/>
            <p:input port="source">
               <p:pipe step="create-collection" port="source"/>
            </p:input>
         </ml:insert-document>
         <ml:adhoc-query>
            <p:with-option name='user' select='$xdb.user'/>
            <p:with-option name='password' select='$xdb.password'/>
            <p:with-option name='host' select='$xdb.host'/>
            <p:with-option name='port' select='$xdb.port'/>
            <p:with-param name='name' select='$name'/>
            <p:input port="source">
            <p:inline>
            <c:query>
               declare namespace atom='http://www.w3.org/2005/Atom';
               let $updated := collection('app://collections/')/atom:updated
                  return if ($updated)
                         then xdmp:node-replace($updated,element atom:updated { current-dateTime() })
                         else xdmp:document-insert(
                                  'app://collections/updated.xml',
                                  element atom:updated { current-dateTime() },
                                  xdmp:default-permissions(),
                                  ('app://collections/'))
            </c:query>
            </p:inline>
            </p:input>
         </ml:adhoc-query>
         <p:template>
            <p:input port='template'>
               <p:inline>
                  <http xmlns='http://www.xproclet.org/V/HTTP/' status='201'>
                  <header name='Location'>/collections/{$name}/</header>
                  </http>
               </p:inline>
            </p:input>
            <p:with-param name="name" select="$name"/>
            <p:input port="source"><p:inline><doc/></p:inline></p:input>
         </p:template>
      </p:when>
      <p:otherwise>
         <p:template>
            <p:input port='template'>
               <p:inline>
                  <http xmlns='http://www.xproclet.org/V/HTTP/' status='409'>
                  <entity>The collection name {$name} conflicts with an existing collection.</entity>
                  </http>
               </p:inline>
            </p:input>
            <p:input port="source"><p:inline><doc/></p:inline></p:input>
            <p:with-param name='name' select='$name'/>
         </p:template>
      </p:otherwise>
      </p:choose>
   </p:group>

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