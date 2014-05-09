<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                version="1.0"
                name="test">
<p:output port="result"/>

<p:identity>
<p:input port="source">
<p:inline>
<html xmlns="http://www.w3.org/1999/xhtml">
<head><title>XProc!</title></head>
<body><p>Success!</p></body>
</html>
</p:inline>
</p:input>
</p:identity>

</p:declare-step>
