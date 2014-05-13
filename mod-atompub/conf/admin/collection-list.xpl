<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="collection-list">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='uri'/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='uri' select='$uri'/>
<p:input port="source">
<p:inline>
<c:query>
declare variable $uri external;
element collection {
   attribute id { $uri },
   for $doc in collection($uri)/*
      return element document { attribute href { base-uri($doc) }, attribute name { local-name($doc) }, attribute namespace { namespace-uri($doc) } }
}
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>