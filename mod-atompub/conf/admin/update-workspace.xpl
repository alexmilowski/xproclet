<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="update-workspace">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='uuid'/>
<p:input port='source'/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:choose>
<p:when test="/app:workspace">

   <ml:insert-document collections="app://admin/" name="insert">
      <p:with-option name='user' select='$xdb.user'/>
      <p:with-option name='password' select='$xdb.password'/>
      <p:with-option name='host' select='$xdb.host'/>
      <p:with-option name='port' select='$xdb.port'/>
      <p:with-option name="uri" select="concat('app://admin/workspace/',$uuid)"/>
   </ml:insert-document>

   <p:identity>
      <p:input port="source">
      <p:inline>
      <http xmlns='http://www.xproclet.org/V/HTTP/' status='204'/>
      </p:inline>
      </p:input>
   </p:identity>
   
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