<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:ml="http://xmlcalabash.com/ns/extensions/marklogic"
                xmlns:app='http://www.w3.org/2007/app'
                version="1.0"
                name="service">
<p:option name='xdb.user'/>
<p:option name='xdb.password'/>
<p:option name='xdb.host'/>
<p:option name='xdb.port'/>
<p:option name='app.path'/>
<p:output port="result"/>
<p:serialization port="result" indent='true'/>

<p:import href="http://xmlcalabash.com/extension/steps/library-1.0.xpl"/>

<ml:adhoc-query>
<p:with-option name='user' select='$xdb.user'/>
<p:with-option name='password' select='$xdb.password'/>
<p:with-option name='host' select='$xdb.host'/>
<p:with-option name='port' select='$xdb.port'/>
<p:with-param name='app.path' select='$app.path'/>
<p:input port="source">
<p:inline>
<c:query>
declare namespace app='http://www.w3.org/2007/app';
declare namespace atom='http://www.w3.org/2005/Atom';
declare variable $app.path external;
&lt;app:service xmlns:app="http://www.w3.org/2007/app" xmlns:atom="http://www.w3.org/2005/Atom">
{
   for $w in collection('app://admin/')/app:workspace
      return element app:workspace {
         attribute href { concat($app.path,"/collections/") },          
         $w/atom:*,
         for $c in $w/app:collection
            return
            if ($c/@href = "*")
            then 
               for $d in collection('app://collections/')/app:collection
                  return element app:collection {
                            attribute href { concat($app.path,substring-before(substring-after(base-uri($d),'app:/'),'.col'),'/') },
                            $d/*
                         } 
            else 
               let $uri := concat('app:/',substring($c/@href,1,string-length($c/@href)-1),'.col'), $d := document($uri)
                  return if ($d) 
                         then element app:collection {
                            attribute href { concat($app.path,$c/@href) },
                            $d/app:collection/*
                         } 
                         else ()
            
      }
}
&lt;/app:service>
</c:query>
</p:inline>
</p:input>
</ml:adhoc-query>

</p:declare-step>