<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="collections">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:input port="parameters" kind="parameter"><p:inline><c:param-set/></p:inline></p:input>
<p:input port="source">
<p:inline>
<c:query>
element collections {
for $collection in cts:collections()
  return element collection {$collection}
}
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>