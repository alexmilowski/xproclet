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

<ml:insert-document name="replace-media">
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
   <p:with-option name="collections" select="concat('app://collections/',$name,'/media')"/>
   <p:with-option name="uri" select="concat('app://collections/',$name,'/media/',$file)"/>
   <p:input port="source">
      <p:pipe step="update-collection-feed-media" port="source"/>
   </p:input>
</ml:insert-document>

<p:identity>
   <p:input port="source">
   <p:inline>
   <http xmlns='http://www.xproclet.org/V/HTTP/' status='204'/>
   </p:inline>
   </p:input>
</p:identity>
   


</p:declare-step>