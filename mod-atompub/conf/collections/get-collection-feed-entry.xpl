<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                version="1.0"
                name="get-collection-feed-entry">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='app.path'/>
<p:option name='request-host'/>
<p:option name='name'/>
<p:option name='id'/>
<p:output port="result"/>
<p:serialization port="result"/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='name' select='$name'/>
<p:with-param name='id' select='$id'/>
<p:with-param name='app.path' select='$app.path'/>
<p:with-param name='request-host' select='$request-host'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace http="http://www.xproclet.org/V/HTTP/";
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $name external;
declare variable $id external;
declare variable $app.path external;
declare variable $request-host external;
 let $uri := concat('app://collections/',$name,'/entry/',$id,'.atom'), $d := document($uri)
    return
    if ($d) then
       element http:http {
           attribute status { "200" },
           element http:entity {
              attribute type { "application/atom+xml; charset=utf-8" },
              element atom:entry {
                 attribute xml:base { "../" (:concat($request-host,$app.path,"/collections/",$name,"/"):) },
                 $d/atom:entry/*,
                 element atom:link {
                    attribute rel { "edit" },
                    attribute type { "application/atom+xml;type=entry" },
                    attribute href { concat("./_/",$id,".atom") (:concat($app.path,"/collections/",$name,"/_/",$id,".atom"):) }
                 },
                 if ($d/atom:entry/atom:content/@src) 
                 then
                    (element atom:link {
                       attribute rel { "edit-media" },
                       attribute href { $d/atom:entry/atom:content/@src (:concat($app.path,"/collections/",$name,"/",$d/atom:entry/atom:content/@src):) }
                     },
                     element atom:link {
                       attribute rel { "alternate" },
                       $d/atom:entry/atom:content/@type,
                       attribute href { $d/atom:entry/atom:content/@src (:concat($app.path,"/collections/",$name,"/",$d/atom:entry/atom:content/@src):) }
                     },
                     if (starts-with($d/atom:entry/atom:content/@type,'image/'))
                     then
                     element atom:link {
                       attribute rel { "icon" },
                       $d/atom:entry/atom:content/@type,
                       attribute href { concat($d/atom:entry/atom:content/@src,"?scale=100") (:concat($app.path,"/collections/",$name,"/",$d/atom:entry/atom:content/@src,"?scale=100"):) }
                     }
                     else ())
                 else ()
              }
           }
        }
   else element http:http { attribute status {"404"}, element http:entity { concat("Cannot find entry: ",$app.path,"/collections/",$name,"/_/",$id,'.atom') } }


</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>