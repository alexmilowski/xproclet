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
<p:option name='path'/>
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
<p:with-param name='path' select='$path'/>
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
declare variable $path external;
declare variable $limit external;
declare variable $start external;
declare variable $app.path external;
declare variable $request-host external;
element atom:feed {
   "
",
   element atom:title { $path },
   "
",
   element atom:id {
      concat($request-host,$app.path,"/topic/",$path)
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
   let $s := tokenize(if (ends-with($path,"/")) then substring($path,0,string-length($path)) else $path,"/"),
       $cpath := (
      let $index := (1 to count($s)-1)
      for $i in $index
         let $c := collection('app://collections/')/app:collection[string(atom:category[@scheme='http://www.atomojo.org/O/' and @term='parent'])=$s[$i]][string(atom:category[@scheme='http://www.atomojo.org/O/' and @term='topic'])=$s[$i+1]]
            return
                if ($c)
                then $c
                else element no { attribute pair { subsequence($s,$i,2)} }
   )
      return 
         if ($cpath/no) 
         then () 
         else 
            let $len := count($s)
            for $w in collection('app://collections/')/app:collection[string(atom:category[@scheme='http://www.atomojo.org/O/' and @term='parent'])=$s[$len - 1]][string(atom:category[@scheme='http://www.atomojo.org/O/' and @term='topic'])=$s[$len]]
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