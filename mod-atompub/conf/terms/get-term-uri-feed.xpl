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
<p:option name='uri'/>
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
<p:with-param name='uri' select='$uri'/>
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
declare variable $uri external;
declare variable $limit external;
declare variable $start external;
declare variable $app.path external;
declare variable $request-host external;
element atom:feed {
   "
",
   element atom:id {
      concat($request-host,$app.path,"/term/",$uri)
   },
   "
",
   element atom:link {
      attribute rel { "self" },
      attribute href { "" }
   },
   "
",
   (for $e in collection('app://collections/entry')/atom:entry[atom:category[concat(@scheme,@term)=$uri]]
      order by $e/atom:updated descending 
      return $e)[1]/atom:updated,
   "
",
   let $istart := xs:integer($start), $ilimit := $istart+xs:integer($limit)
   for $e in (for $e in collection('app://collections/entry')/atom:entry[atom:category[concat(@scheme,@term)=$uri]] order by $e/atom:updated return $e)[position()&gt;=$istart and position()&lt;$ilimit]
      return (element atom:entry {
        "
",
        for $c in $e/*
           return (
              $c,"
"),
              if ($e/atom:content/@src) 
              then
                 (element atom:link {
                    attribute rel { "alternate" },
                    $e/atom:content/@type,
                    attribute href { $e/atom:content/@src (:concat($app.path,"/collections/",$name,"/",$e/atom:content/@src):) }
                 },"
",
                  if (starts-with($e/atom:content/@type,'image/'))
                  then
                  element atom:link {
                    attribute rel { "icon" },
                    $e/atom:content/@type,
                    attribute href { concat($e/atom:content/@src,"?scale=100") (:concat($app.path,"/collections/",$name,"/",$e/atom:content/@src,"?scale=100"):) }
                  }
                  else ()
                 )
              else ()
      },"
")
}
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>
</p:declare-step>