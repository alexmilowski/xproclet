<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:a='http://www.xproclet.org/atompub/local'
                xmlns:atom="http://www.w3.org/2005/Atom"
                version="1.0"
                name="update-collection-feed-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='id'/>
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
<p:when test="/atom:entry">

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
   declare namespace atom='http://www.w3.org/2005/Atom';
   declare variable $name external;
   declare variable $id external;
    let $uri := concat('app://collections/',$name,'/entry/',$id,'.atom'), $d := document($uri)
       return if ($d) then 
                 if ($d/atom:entry/atom:id) 
                    then $d/atom:entry/atom:id
                    else element without-id {}
              else element missing {}
   </c:query>
   </p:inline>
   </p:input>
   </ml:adhoc-query>

   <p:choose>
   <p:when test="/missing">
      <p:template>
         <p:input port="template">
         <p:inline>
         <http xmlns='http://www.xproclet.org/V/HTTP/' status='404'>
         <entity type='text/plain'>Entry does not exist.</entity>
         </http>
         </p:inline>
         </p:input>
         <p:input port="parameters"><p:inline><c:param-set/></p:inline></p:input>
      </p:template>
   </p:when>
   <p:otherwise>
      <p:viewport name="guarantee-id" match="without-id">
         <a:generate-uuid name="uuid">
            <p:with-option name='user' select='$xdb.user'/>
            <p:with-option name='password' select='$xdb.password'/>
            <p:with-option name='host' select='$xdb.host'/>
            <p:with-option name='port' select='$xdb.port'/>
         </a:generate-uuid>
         <p:rename match="uuid" new-name="atom:id"/>
      </p:viewport>
      <ml:adhoc-query name="entry-content">
      <p:with-option name='user' select='$xdb.user'/>
      <p:with-option name='password' select='$xdb.password'/>
      <p:with-option name='host' select='$xdb.host'/>
      <p:with-option name='port' select='$xdb.port'/>
      <p:with-param name='name' select='$name'/>
      <p:with-param name='id' select='$id'/>
      <p:input port="source">
      <p:inline>
      <c:query>
      declare namespace atom='http://www.w3.org/2005/Atom';
      declare variable $name external;
      declare variable $id external;
       let $uri := concat('app://collections/',$name,'/entry/',$id,'.atom'), $d := document($uri)
          return if ($d/atom:entry/atom:content/@src)
                    then element content {
                       $d/atom:entry/atom:content/@src,
                       $d/atom:entry/atom:content/@type
                    }
                    else element without-content {}
      </c:query>
      </p:inline>
      </p:input>
      </ml:adhoc-query>

      <p:xslt>
         <p:input port="source">
            <p:pipe step="update-collection-feed-entry" port="source"/>
         </p:input>
         <p:input port="stylesheet">
            <p:inline exclude-inline-prefixes="p c ml">
<xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" >
<xsl:param name="id"/>
<xsl:param name="src"/>
<xsl:param name="type"/>
<xsl:template match="atom:entry">
<atom:entry>
   <xsl:choose>
      <xsl:when test="not(atom:id)">
      <atom:id><xsl:value-of select="$id"/></atom:id>
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
   <xsl:if test="not(atom:content) and $src">
   <atom:content src="{$src}" type="{$type}"/>
   </xsl:if>
</atom:entry>
</xsl:template>

<xsl:template match="atom:link[@rel='edit']"/>
<xsl:template match="atom:link[@rel='edit-media']"/>
<xsl:template match="app:edited|atom:id|atom:updated|atom:published"/>

<xsl:template match="atom:content">
<xsl:choose>
<xsl:when test="$src">
<atom:content src="{$src}" type="{$type}"/>
</xsl:when>
<xsl:otherwise>
<xsl:copy>
<xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:otherwise>
</xsl:choose>
</xsl:template>

<xsl:template match="*|@*">
<xsl:copy>
<xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:template>

</xsl:transform>
            </p:inline>
         </p:input>
         <p:with-param name='id' select='/atom:id'>
            <p:pipe step="guarantee-id" port="result"/>
         </p:with-param>
         <p:with-param name='src' select='/content/@src'>
            <p:pipe step="entry-content" port="result"/>
         </p:with-param>
         <p:with-param name='type' select='/content/@type'>
            <p:pipe step="entry-content" port="result"/>
         </p:with-param>
      </p:xslt>

   <ml:insert-document name="replace-entry">
      <p:with-option name='user' select='$xdb.user'/>
      <p:with-option name='password' select='$xdb.password'/>
      <p:with-option name='host' select='$xdb.host'/>
      <p:with-option name='port' select='$xdb.port'/>
      <p:with-option name="collections" select="concat('app://collections/',$name,'/entry app://collections/entry')"/>
      <p:with-option name="uri" select="concat('app://collections/',$name,'/entry/',$id,'.atom')"/>
   </ml:insert-document>

   <p:template>
      <p:input port='template'>
         <p:inline>
            <http xmlns='http://www.xproclet.org/V/HTTP/' status='204'/>
         </p:inline>
      </p:input>
      <p:with-param name='name' select='$name'/>
      <p:with-param name='id' select='$id'/>
      <p:input port="source"><p:pipe step="replace-entry" port="result"/></p:input>
   </p:template>
   
   </p:otherwise>
   </p:choose>

</p:when>
<p:otherwise>
   <p:template>
      <p:input port="template">
      <p:inline>
      <http xmlns='http://www.xproclet.org/V/HTTP/' status='400'>
      <entity type='text/plain'>Non-entry document element received: {{{namespace-uri(/*)}}}{local-name(/*)}</entity>
      </http>
      </p:inline>
      </p:input>
      <p:input port="parameters"><p:inline><c:param-set/></p:inline></p:input>
   </p:template>
</p:otherwise>
</p:choose>

</p:declare-step>
