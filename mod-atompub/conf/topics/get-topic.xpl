<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="get-term-feed">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='leaf'/>
<p:option name='limit'/>
<p:option name='start'/>
<p:option name='app.path'/>
<p:option name='request-host'/>
<p:option name='forwarded-host'/>
<p:option name='forwarded-path'/>
<p:output port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='leaf' select='$leaf'/>
<p:with-param name='limit' select="$limit"/>
<p:with-param name='start' select="$start"/>
<p:with-param name='request-host' select='if (string-length($forwarded-host)>0) then $forwarded-host else $request-host'/>
<p:with-param name='app.path' select='if (string-length($forwarded-path)>0) then $forwarded-path else $app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/2011/1/0";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $leaf external;
declare variable $limit external;
declare variable $start external;
declare variable $app.path external;
declare variable $request-host external;
element atom:feed {
   "
",
   element atom:title { $leaf },
   "
",
   element atom:id {
      concat($request-host,$app.path,"/topic/",$leaf,"/")
   },
   "
",
   element atom:link {
      attribute rel { "self" },
      attribute href { "" }
   },
   "
",
   collection('app://collections/')/atom:updated,"
",
   for $w in collection('app://collections/')/app:collection[string(atom:category[@scheme='http://www.atomojo.org/O/' and @term='topic'])=$leaf]
      let $name := substring-before(substring-after(base-uri($w),'app://collections/'),'.col')
      return element atom:entry { 
          $w/atom:title,
          element atom:link {
             attribute rel { "edit" },
             attribute href { concat($app.path,"/collections/",$name,".atom") }
          },
          element atom:link {
             attribute rel { "edit-media" },
             attribute type { "application/atomsvc+xml;type=collection" },
             attribute href { concat($app.path,"/collections/",$name,".col") }
          },
          element atom:link {
             attribute rel { "alternate" },
             attribute type { "application/atom+xml;type=feed" },
             attribute href { concat($app.path,"/collections/",$name,"/") }
          },
          element atom:content {
             attribute type { "application/atomsvc+xml;type=collection" },
             attribute src { concat($app.path,"/collections/",$name,".col") }
          },
          $w/atom:category
   }
}
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>