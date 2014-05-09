<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:http="http://www.xproclet.org/V/HTTP/"
                version="1.0"
                name="test">
<p:output port="result"/>

<p:identity>
<p:input port="source">
<p:inline>
   <http:http status="201"/>
</p:inline>
</p:input>
</p:identity>

</p:declare-step>
