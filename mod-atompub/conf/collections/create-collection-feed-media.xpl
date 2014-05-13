<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:atom="http://www.w3.org/2005/Atom"
                version="1.0"
                name="create-collection-feed-media">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='file'/>
<p:option name='media-type'/>
<p:input port="source"/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:insert-document name="insert-media">
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
   <p:with-option name="collections" select="concat('app://collections/',$name,'/media')"/>
   <p:with-option name="uri" select="concat('app://collections/',$name,'/media/',$file)"/>
   <p:input port="source">
      <p:pipe step="create-collection-feed-media" port="source"/>
   </p:input>
</ml:insert-document>
<ml:adhoc-query>
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
   <p:with-param name='name' select='$name'/>
   <p:with-param name='file' select='$file'/>
   <p:with-param name='media-type' select='$media-type'/>
   <p:input port="source">
      <p:inline>
      <c:query>
      declare variable $name external;
      declare variable $file external;
      declare variable $media-type external;
      element ok {
         xdmp:document-set-property(concat('app://collections/',$name,'/media/',$file),&lt;media-type xmlns='http://www.atomojo.org/property'>{$media-type}&lt;/media-type>)
      }
      </c:query>
      </p:inline>
   </p:input>
</ml:adhoc-query>

</p:declare-step>