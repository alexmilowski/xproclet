<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="create-workspace">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:input port="source"/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:choose>
<p:when test="/app:workspace">
   <ml:adhoc-query name='generate-uuid'>
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
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

   <ml:insert-document collections="app://admin/" name="insert">
      <p:with-option name='user' select='$xdb.user'/>
      <p:with-option name='password' select='$xdb.password'/>
      <p:with-option name='host' select='$xdb.host'/>
      <p:with-option name='port' select='$xdb.port'/>
      <p:with-option name="uri" select="concat('app://admin/workspace/',/uuid)">
         <p:pipe step="generate-uuid" port="result"/>
      </p:with-option>
      <p:input port="source">
         <p:pipe step="create-workspace" port="source"/>
      </p:input>
   </ml:insert-document>
   <p:template>
   <p:input port='template'>
      <p:inline>
         <http xmlns='http://www.xproclet.org/V/HTTP/' status='201'>
         <header name='Location'>/admin/workspaces/{concat(substring-after(/*/text(),'app://admin/workspace/'),'/')}</header>
         </http>
      </p:inline>
   </p:input>
   <p:input port="parameters"><p:inline><c:param-set/></p:inline></p:input>
   <p:input port="source"><p:pipe step="insert" port="result"/></p:input>
   </p:template>
</p:when>
<p:otherwise>
   <p:identity>
      <p:input port="source">
      <p:inline>
      <http xmlns='http://www.xproclet.org/V/HTTP/' status='400'/>
      </p:inline>
      </p:input>
   </p:identity>
</p:otherwise>
</p:choose>

</p:declare-step>