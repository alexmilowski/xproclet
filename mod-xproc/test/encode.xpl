<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:http="http://www.xproclet.org/V/HTTP/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                name="test">
<p:input port="source"/>
<p:output port="result"/>

<p:template>
   <p:input port="template">
      <p:inline>
         <c:request method="GET" href="{resolve-uri(/c:file/@name,base-uri(/c:file))}" override-content-type="{/c:file/@content-type}"/>
      </p:inline>
   </p:input>
   <p:input port="parameters">
      <p:empty/>
   </p:input>
</p:template>
<p:http-request/>

</p:declare-step>