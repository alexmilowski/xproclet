<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="update-collection-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:output port="result"/>
<p:input port="source"/>
<p:serialization port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<p:identity>
<p:input port="source">
<p:inline>
<http xmlns='http://www.xproclet.org/V/HTTP/' status='501'><entity>Not implemented.</entity></http>
</p:inline>
</p:input>
</p:identity>

</p:declare-step>