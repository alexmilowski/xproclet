<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="query">
<p:option name="xdb.user"/>
<p:option name="xdb.password"/>
<p:option name="xdb.host"/>
<p:option name="xdb.port"/>
<p:option name="app.path"/>

<p:input port="query" primary="true"/>
<p:input port="parameters" primary="true" kind="parameter"/>
<p:output port="result" sequence="true"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='app.path' select='$app.path'/>
<p:input port="parameters"><p:pipe step="query" port="parameters"/></p:input>
</ml:adhoc-query>

</p:declare-step>