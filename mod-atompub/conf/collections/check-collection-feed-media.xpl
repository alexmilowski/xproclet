<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                xmlns:atom="http://www.w3.org/2005/Atom"
                version="1.0"
                name="check-collection-feed-media">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='file'/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
   <p:with-option name='user' select='$xdb.user'/>
   <p:with-option name='password' select='$xdb.password'/>
   <p:with-option name='host' select='$xdb.host'/>
   <p:with-option name='port' select='$xdb.port'/>
   <p:with-param name="name" select="$name"/>
   <p:with-param name="file" select="$file"/>
   <p:input port="source">
      <p:inline>
      <c:query>
      declare namespace http="http://www.xproclet.org/V/HTTP/";
      declare variable $name external;
      declare variable $file external;
       let $uri := concat('app://collections/',$name,'/media/',$file), $d := document($uri)
          return if ($d) then &lt;yes/> else &lt;no/>
      </c:query>
      </p:inline>
   </p:input>
</ml:adhoc-query>

</p:declare-step>