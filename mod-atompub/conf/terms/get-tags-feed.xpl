<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="get-term-uri-feed">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='limit'/>
<p:option name='start'/>
<p:output port="result"/>
<p:option name='app.path'/>
<p:option name='request-host'/>
<p:option name='forwarded-host'/>
<p:option name='forwarded-path'/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>


<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='limit' select="$limit"/>
<p:with-param name='start' select="$start"/>
<p:with-param name='request-host' select='if (string-length($forwarded-host)>0) then $forwarded-host else $request-host'/>
<p:with-param name='app.path' select='if (string-length($forwarded-path)>0) then $forwarded-path else $app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare default collation "http://marklogic.com/collation/codepoint";
declare variable $limit external;
declare variable $start external;
declare variable $app.path external;
declare variable $request-host external;
element atom:feed {
   "
",
   element atom:id {
      concat($request-host,$app.path,"/term/tags/")
   },
   "
",
   element atom:link {
      attribute rel { "self" },
      attribute href { "" }
   },
          "
",
for $tag in cts:element-attribute-values(xs:QName("atom:category"),xs:QName("term"),"","item-frequency")
   return if (exists(collection('app://collections/entry')/atom:entry/atom:category[not(@scheme) and @term=$tag]))
          then element atom:category {
                  attribute term { $tag },
                  attribute content { cts:frequency($tag) }
               }
          else (),
          element foo {}
}
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>
</p:declare-step>