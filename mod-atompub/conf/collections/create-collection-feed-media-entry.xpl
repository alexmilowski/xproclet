<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:a='http://www.xproclet.org/atompub/local'
                version="1.0"
                name="create-collection-feed-media-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='file'/>
<p:option name='media-type'/>
<p:option name='hidden'/>
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

<a:generate-uuid name="entry-uuid">
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
</a:generate-uuid>
<p:group>
   <p:variable name="id" select="/uuid"/>
   <p:xslt>
      <p:with-param name="id" select="$id"/>
      <p:with-param name="file" select="$file"/>
      <p:with-param name="media-type" select="$media-type"/>
      <p:with-param name="hidden" select="$hidden"/>
      <p:input port="stylesheet">
         <p:inline exclude-inline-prefixes="p c ml a">
            <xsl:transform version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
               <xsl:param name="id"/>
               <xsl:param name="file"/>
               <xsl:param name="media-type"/>
               <xsl:param name="hidden"/>
               <xsl:template match="/">
               <atom:entry>
                  <atom:id>urn:uuid:<xsl:value-of select="$id"/></atom:id>
                  <atom:published><xsl:value-of select="current-dateTime()"/></atom:published>
                  <atom:updated><xsl:value-of select="current-dateTime()"/></atom:updated>
                  <app:edited><xsl:value-of select="current-dateTime()"/></app:edited>
                  <atom:title><xsl:value-of select="$file"/></atom:title>
                  <xsl:if test="$hidden='true'">
                     <atom:category scheme="http://www.atomojo.org/O/" term="hidden"/>
                  </xsl:if>
                  <atom:content src="{$file}" type="{$media-type}"/>
                  </atom:entry>
               </xsl:template>
            </xsl:transform>
         </p:inline>
      </p:input>
   </p:xslt>
   <ml:insert-document name="insert-media-entry">
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
            <id>{$id}</id>
         </p:inline>
      </p:input>
      <p:with-param name='name' select='$name'/>
      <p:with-param name='id' select='$id'/>
      <p:input port="source"><p:pipe step="insert-media-entry" port="result"/></p:input>
   </p:template>
</p:group>

</p:declare-step>