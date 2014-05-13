<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:a='http://www.xproclet.org/atompub/local'
                version="1.0"
                name="create-collection-feed-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='app.database'/>
<p:option name='name'/>
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

<a:generate-uuid name="uuid">
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
</a:generate-uuid>
<p:group>
   <p:variable name="uuid" select="/uuid">
      <p:pipe step="uuid" port="result"/>
   </p:variable>
   <p:xslt>
      <p:input port="source">
         <p:pipe step="create-collection-feed-entry" port="source"/>
      </p:input>
      <p:input port="stylesheet">
         <p:inline exclude-inline-prefixes="p c ml">
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="uuid"/>
<xsl:template match="atom:entry">
<atom:entry>
<xsl:choose>
   <xsl:when test="not(atom:id)">
   <atom:id>urn:uuid:<xsl:value-of select="$uuid"/></atom:id>
   </xsl:when>
   <xsl:otherwise>
   <xsl:copy-of select="atom:id"/>
   </xsl:otherwise>
</xsl:choose>
<xsl:choose>
   <xsl:when test="not(atom:published)">
   <atom:published><xsl:value-of select="current-dateTime()"/></atom:published>
   </xsl:when>
   <xsl:otherwise>
   <xsl:copy-of select="atom:published"/>
   </xsl:otherwise>
</xsl:choose>
<xsl:choose>
   <xsl:when test="not(atom:updated)">
   <atom:updated><xsl:value-of select="current-dateTime()"/></atom:updated>
   </xsl:when>
   <xsl:otherwise>
   <xsl:copy-of select="atom:updated"/>
   </xsl:otherwise>
</xsl:choose>
<app:edited><xsl:value-of select="current-dateTime()"/></app:edited>
<xsl:apply-templates/>
</atom:entry>
</xsl:template>

<xsl:template match="atom:link[@rel='edit']"/>
<xsl:template match="atom:link[@rel='edit-media']"/>
<xsl:template match="app:edited|atom:id|atom:published|atom:updated"/>

<xsl:template match="*|@*">
<xsl:copy>
<xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:template>

</xsl:transform>
         </p:inline>
      </p:input>
      <p:with-param name='uuid' select='$uuid'/>
   </p:xslt>
   <ml:insert-document name="insert-entry">
      <p:with-option name='user' select='$xdb.user'/>
      <p:with-option name='password' select='$xdb.password'/>
      <p:with-option name='host' select='$xdb.host'/>
      <p:with-option name='port' select='$xdb.port'/>
      <p:with-option name="collections" select="concat('app://collections/',$name,'/entry app://collections/entry')"/>
      <p:with-option name="uri" select="concat('app://collections/',$name,'/entry/',$uuid,'.atom')"/>
   </ml:insert-document>

   <p:template>
      <p:input port='template'>
         <p:inline>
            <http xmlns='http://www.xproclet.org/V/HTTP/' status='201'>
            <header name='Location'>/{$app.database}/collections/{$name}/_/{$uuid}.atom</header>
            <attribute name="org.xproclet.atompub.id" value="{$uuid}"/>
            </http>
         </p:inline>
      </p:input>
      <p:with-param name='app.database' select='$app.database'/>
      <p:with-param name='name' select='$name'/>
      <p:with-param name='uuid' select='$uuid'/>
      <p:input port="source"><p:pipe step="insert-entry" port="result"/></p:input>
   </p:template>
</p:group>

</p:declare-step>