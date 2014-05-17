<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="get-collection-feed">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='name'/>
<p:option name='limit'/>
<p:option name='start'/>
<p:option name='scheme'/>
<p:option name='term'/>
<p:option name='published'/>
<p:option name='app.path'/>
<p:option name='request-host'/>
<p:option name='forwarded-host'/>
<p:option name='forwarded-path'/>
<p:output port="result"/>
<p:serialization port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='name' select='$name'/>
<p:with-param name='limit' select="$limit"/>
<p:with-param name='start' select="$start"/>
<p:with-param name='term' select="$term"/>
<p:with-param name='scheme' select="$scheme"/>
<p:with-param name='order-by' select="$order-by"/>
<p:with-param name='published' select="$published"/>
<p:with-param name='request-host' select='if (string-length($forwarded-host)>0) then $forwarded-host else $request-host'/>
<p:with-param name='app.path' select='if (string-length($forwarded-path)>0) then $forwarded-path else $app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $name external;
declare variable $limit external;
declare variable $start external;
declare variable $scheme external;
declare variable $term external;
declare variable $published external;
declare variable $order-by external;
declare variable $app.path external;
declare variable $request-host external;
let $uri := concat('app://collections/',$name,'.col'), $d := document($uri), $curi := concat('app://collections/',$name,'/entry'),
    $istart := xs:integer($start), $ilimit := $istart+xs:integer($limit)
   return if ($d)
          then   
          element atom:feed {
            "
",
            for $e in $d/app:collection/*[namespace-uri()!='http://www.w3.org/2007/app'][not(namespace-uri()='http://www.w3.org/2005/Atom' and local-name()='updated')]
               return ($e,
            "
"),
            if ($d/app:collection/atom:id)
            then ()
            else (
               element atom:id {
                  concat($request-host,$app.path,"/collections/",$name,"/")
               },
               "
"),
            element atom:link {
               attribute rel { "self" },
               attribute href { "" (:concat($app.path,"/collections/",$name,"/"):) }
            },
            "
",
            element atom:link {
               attribute rel { "edit" },
               attribute href { "" (:concat($app.path,"/collections/",$name,"/"):) }
            },
            if ((collection($curi)/atom:entry)[position()&gt;=$ilimit])
            then
               ("
",

               element atom:link {
                  attribute rel { "next" },
                  attribute href { concat("?start=",$ilimit,"&amp;amp;limit=",$limit)  }
               }
               )
            else (),
            "
",
            (for $e in collection($curi)/atom:entry
               order by $e/atom:updated descending return $e)[1]/atom:updated,
            for $e in 
               (
                if ($order-by = "published")
                then 
                  if ($published)
                  then for $e in collection($curi)/atom:entry[atom:published=$published] order by $e/atom:published descending return $e
                  else
                  if ($scheme and $term)
                  then for $e in collection($curi)/atom:entry[atom:category[@term=$term and @scheme=$scheme ]] order by $e/atom:published descending return $e
                  else
                  if ($term) 
                  then for $e in collection($curi)/atom:entry[atom:category[@term=$term and not(@scheme)]] order by $e/atom:published descending return $e
                  else for $e in collection($curi)/atom:entry order by $e/atom:published descending return $e
                else 
                  if ($published)
                  then for $e in collection($curi)/atom:entry[atom:published=$published] order by $e/atom:updated descending return $e
                  else
                  if ($scheme and $term) 
                  then for $e in collection($curi)/atom:entry[atom:category[@term=$term and @scheme=$scheme]] order by $e/atom:updated descending return $e
                  else
                  if ($term) 
                  then for $e in collection($curi)/atom:entry[atom:category[@term=$term and not(@scheme)]] order by $e/atom:updated descending return $e
                  else for $e in collection($curi)/atom:entry order by $e/atom:updated descending return $e
               )[position()&gt;=$istart and position()&lt;$ilimit]
               
               return (element atom:entry {
                 "
",
                 for $c in $e/*
                    return ($c,"
"),
                 element atom:link {
                    attribute rel { "edit" },
                    attribute type { "application/atom+xml;type=entry" },
                    attribute href { concat("./_",substring-after(base-uri($e),$curi)) (:concat($app.path,"/collections/",$name,"/_",substring-after(base-uri($e),$curi)):) }
                 },
                 "
",
                 if ($e/atom:content/@src) 
                 then
                    (element atom:link {
                       attribute rel { "edit-media" },
                       attribute href { $e/atom:content/@src (:concat($app.path,"/collections/",$name,"/",$e/atom:content/@src):) }
                    },"
",                   element atom:link {
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
          else element http:http { attribute status {"404"}, element http:entity { concat("There is no collection named ",$name,".") } }
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>