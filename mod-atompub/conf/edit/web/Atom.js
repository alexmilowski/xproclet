var ATOM_NS = "http://www.w3.org/2005/Atom";
var ATOM_PUB_INTROSPECT_NS = "http://www.w3.org/2007/app";
var ATOM_PUB_INTROSPECT_MIME_TYPE = "application/atomserv+xml";
var _Atom_debug_ = false;
var _Atom_debug_prefix_ = "";

if (!document.ELEMENT_NODE) {
  document.ELEMENT_NODE = 1;
  document.ATTRIBUTE_NODE = 2;
  document.TEXT_NODE = 3;
  document.CDATA_SECTION_NODE = 4;
  document.ENTITY_REFERENCE_NODE = 5;
  document.ENTITY_NODE = 6;
  document.PROCESSING_INSTRUCTION_NODE = 7;
  document.COMMENT_NODE = 8;
  document.DOCUMENT_NODE = 9;
  document.DOCUMENT_TYPE_NODE = 10;
  document.DOCUMENT_FRAGMENT_NODE = 11;
  document.NOTATION_NODE = 12;
}

var AtomPub = {
   isIE: typeof ActiveXObject != "undefined",
   toLower: document.documentElement.tagName=="HTML",

createElementNS: function(owner,namespace,localName) {
   if (typeof owner.createElementNS != 'undefined') {
      return owner.createElementNS(namespace,localName);
   } else if (typeof owner.createNode != 'undefined') {
      return owner.createNode(1,localName,namespace);
   } else if (!namespace || namespace=="http://www.w3.org/1999/xhtml") {
      return owner.createElement(localName);
   } else {
      throw "Unsupported namespace "+namespace;
   }
},
importNode: function(owner,node, allChildren,forceNamespace,convertToLower) {
  switch (node.nodeType) {
    case document.ELEMENT_NODE:
       var newNode = null;
       var name = convertToLower ? (node.localName ? node.localName.toLowerCase() : node.nodeName.toLowerCase()) :
                                   (node.localName ? node.localName : node.nodeName);
       var defaultNS = node.getAttribute("xmlns");
       if (defaultNS) {
          forceNamespace = defaultNS;
       }
       if (name.indexOf(":")>0) {
          forceNamespace = null;
       }
       if (forceNamespace) {
          newNode = this.createElementNS(owner,forceNamespace,name);
          if (node.localName) {
          } else {
             newNode = this.createElementNS(owner,forceNamespace,name);
          }
       } else {
          newNode = this.createElementNS(owner,node.namespaceURI,name);
       }
                     //alert("newNode={"+newNode.namespaceURI+"}"+newNode.localName+" "+newNode.nodeName);
      /* does the node have any attributes to add? */
      if (node.attributes && node.attributes.length > 0) {
         for (var i = 0; i < node.attributes.length; i++) {
            var attName = convertToLower ? node.attributes[i].nodeName.toLowerCase() : node.attributes[i].nodeName;
            // IE does strange things with the xmlns so we check for the attribute
            if (this.isIE && attName=="xmlns") {
               continue;
            }
            if (this.isIE) {
               var attr = node.attributes[attName];
               if (attr && attr.nodeValue) {
                  newNode.setAttribute(attName,attr.nodeValue);
               }
            } else {
               var value =  node.getAttribute(node.attributes[i].nodeName);
               if (value) {
                  newNode.setAttribute(attName,value);
               }
            }
         }
      }
      /* are we going after children too, and does the node have any? */
      if (allChildren && node.childNodes && node.childNodes.length > 0) {
         for (var i = 0; i < node.childNodes.length; i++) {
            newNode.appendChild(this.importNode(owner,node.childNodes[i], allChildren,forceNamespace,convertToLower));
         }
      }
      return newNode;
      break;
    case document.TEXT_NODE:
    case document.CDATA_SECTION_NODE:
      return owner.createTextNode(node.nodeValue);
      break;
    case document.COMMENT_NODE:
      return owner.createComment(node.nodeValue);
      break;
  }
}
};

function _newXMLDocument() {
   if (document.implementation && document.implementation.createDocument) {
      return document.implementation.createDocument("", "", null);
   } else if (window.ActiveXObject) {
      return new ActiveXObject("Microsoft.XMLDOM");
   } else {
      throw "Cannot create new XML document.";
   }
}

if (typeof DOMParser == "undefined") {
   DOMParser = function () {}

   DOMParser.prototype.parseFromString = function (str, contentType) {
      if (typeof ActiveXObject != "undefined") {
         var d = new ActiveXObject("MSXML.DomDocument");
         d.loadXML(str);
         return d;
      } else if (typeof XMLHttpRequest != "undefined") {
         var req = new XMLHttpRequest;
         req.open("GET", "data:" + (contentType || "application/xml") +
                      ";charset=utf-8," + encodeURIComponent(str), false);
         if (req.overrideMimeType) {
            req.overrideMimeType(contentType);
         }
         req.send(null);
         return req.responseXML;
      }
   }
}
function _newDOMParser() {
   return new DOMParser();
}

if (typeof XMLSerializer == "undefined") {
   XMLSerializer = function() {}
   XMLSerializer.prototype.serializeToString = function(doc) {
      return doc.xml;
   }
}

function _newXMLSerializer() {
   return new XMLSerializer();
}


function _Atom_forChild(parent,name,namespace,handler) {
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      if (_Atom_debug_) {
        alert(_Atom_debug_prefix_+DOMUtil.localName(current)+"=?"+name+" "+current.namespaceURI+"=?"+namespace);
      }
      if (DOMUtil.localName(current)==name && DOMUtil.namespaceURI(current)==namespace) {
         handler(current);
      }
      current = current.nextSibling;
   }
}

function _Atom_removeChildren(parent) {
   while (parent.childNodes.length>0) {
      parent.removeChild(parent.childNodes.item(0));
   }
}

function AtomService(uri,source) {
   this.type = 0;
   this.uri = AtomPub.newURI(uri);
   this.authorization = null;
   this.source = source;
   this.username = null;
   this.author = null;
   this.password = null;
   this.xml = null;
   this.loaded = false;
   this.inprocess = null;
   this.attempted = 0;
   this.workspaces = {};
   this.bufferSize = 8192;
}

AtomService.prototype.introspect = function(options) {
   var currentService = this;
   this.attempted += 1;
   this.inprogress = HTTP("GET",this.uri.spec,{
      timeout: options.timeout,
      overrideMimeType: "text/xml",
      username: this.username,
      password: this.password,
      onSuccess: function(status,doc,text) {
         currentService.inprogress = null;
         if (!doc || !doc.documentElement) {
            try {
              var parser = new DOMParser();
              doc = parser.parseFromString(text,"text/xml");
            } catch (ex) {
               if (options.onFailure) {
                 options.onFailure(status,doc,text);
               }
            }
         }
         if (doc) {
            DOMUtil.setAttribute(doc.documentElement,"xml:base","http://www.w3.org/XML/1998/namespace",currentService.uri.spec);
            currentService.loadIntrospection(doc.documentElement);   
            if (options.onSuccess) {
              options.onSuccess(status,doc,text);
            }
         } else if (options.onFailure) {
            options.onFailure(status,doc,text);
         }
      },
      onFailure: function(status,doc,text) {
         currentService.inprogress = null;
         options.onFailure(status,doc,text);
      }
   });
}

AtomService.prototype.loadIntrospection = function(element) {
   this.xml = element;
   this.workspaces = {};
   if (DOMUtil.namespaceURI(element)!=ATOM_PUB_INTROSPECT_NS) {
      this.loaded = false;
      throw "Introspection document from "+this.uri.spec+" is not in the correct namespace: expecting "+ATOM_PUB_INTROSPECT_NS+" and found "+DOMUtil.namespaceURI(element);
   }
   if (DOMUtil.localName(element)!="service") {
      this.loaded = false;
      throw "Introspection document element from "+this.uri.spec+" is not {"+ATOM_PUB_INTROSPECT_NS+"}service : {"+DOMUtil.namespaceURI(element)+"}"+element.localName;
   }
   this.loaded = true;
   var current = element.firstChild;
   var count = 0;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      if (DOMUtil.localName(current)=="workspace" && DOMUtil.namespaceURI(current)==ATOM_PUB_INTROSPECT_NS) {
         count++;
         var id = this.uri.spec+"/"+count;
         current.setAttribute("client-id",id);
         this.workspaces[id] = new AtomWorkspace(this,id,current);
      }
      current = current.nextSibling;
   }
}

function AtomWorkspace(service,id,element) {
   this.type = 1;
   this.service = service;
   this.id = id;
   this.xml = element;
   if (!this.xml) {
      var doc = _newXMLDocument();
      this.xml = DOMUtil.createElementNS(doc,ATOM_PUB_INTROSPECT_NS,"workspace");
      var titleE = DOMUtil.createElementNS(doc,ATOM_NS,"title");
      this.xml.appendChild(titleE);
      titleE.appendChild(doc.createTextNode("workspace"));
   }
   var title = null;
   _Atom_forChild(
      this.xml,
      "title",
      ATOM_NS,
      function(target) { title = target; } 
   );
   if (title) {
      this.title = DOMUtil.text(title);
   } else {
      this.title = this.xml.getAttribute("title");
   }
   this.collections = {};
   this.children = [];

   var href = this.xml.getAttribute("href");
   this.uri = href ? service.uri.resolve(href) : service.uri;
   var current = this.xml.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      if (DOMUtil.localName(current)=="collection" && DOMUtil.namespaceURI(current)==ATOM_PUB_INTROSPECT_NS) {
         var href = current.getAttribute("href");
         if (href) {
            // Make sure the href is absolute
            var uri = this.uri.resolve(href);
            current.setAttribute("href",href);
            var href = uri.relativeTo(this.uri);
            
            var collection = new AtomCollection(this,href,uri.spec,current);
            this.collections[href] = collection;
         }
      }
      current = current.nextSibling;
   }
   this.treeSort();
}

AtomWorkspace.prototype.treeSort = function() {
   var maxLevel = 0;
   for (var href in this.collections) {
      var collection = this.collections[href];
      this._calculateCollectionLevel(collection);
      if (collection.level>maxLevel) {
         maxLevel = collection.level;
      }
   }
   this.parent = null;
   this.children = [];
   for (var href in this.collections) {
      var collection = this.collections[href];
      if (collection.level==0) {
         this.children.push(collection);
         collection.parent = this;
      }
      collection.children = [];
      var next = collection.level + 1;
      for (var otherHref in this.collections) {
         var other = this.collections[otherHref];
         if (other.level==next && otherHref.substring(0,href.length)==href) {
            collection.children.push(other);
            other.parent = collection;
         }
      }
      collection.children.sort(function(x,y) {return x.href < y.href });
   }
   this.children.sort(function(x,y) {return x.href < y.href });
}

AtomWorkspace.prototype._calculateCollectionLevel = function(collection) {
   var count = 0;
   var href = collection.href;
   for (var otherHref in this.collections) {
      if (href!=otherHref && otherHref.length<=href.length && href.substring(0,otherHref.length)==otherHref) {
         count++;
      }
   }
   collection.level = count;
}

AtomWorkspace.prototype.newCollection = function(href,title) {
   var element = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_PUB_INTROSPECT_NS,"collection");
   // Make sure the href is absolute
   var uri = this.uri.resolve(href);
   element.setAttribute("href",href);
   this.xml.appendChild(element);
   var titleE = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"title");
   element.appendChild(titleE);
   titleE.appendChild(element.ownerDocument.createTextNode(title));
            
   var collection = new AtomCollection(this,href,uri.spec,element);
   this.collections[href] = collection;
   this.treeSort();
   return collection;
}

AtomWorkspace.prototype.checkCollectionHierarchy = function(collection) {
   var collections = [];
   var path = collection.uri.relativeTo(this.uri);
   var segments = path.split(/\//);
   var collectionPath = path.charAt(0)=="/" ? "/" : "";
   var end = path.charAt(path.length-1)=="/" ? segments.length-1 : segments.length;
   for (var i=0; i<end; i++) {
      collectionPath += segments[i]+"/";
      if (!this.collections[collectionPath]) {
         var ancestor = this.newCollection(collectionPath,segments[i]);
         this.collections[collectionPath] = ancestor;
         collections.push(ancestor);
      }
   }
   return collections;
}

AtomWorkspace.prototype.createByFeedPost = function(collection,options) {
   var content = _newXMLDocument();
   var feed = DOMUtil.createElementNS(content,ATOM_NS,"feed");
   content.appendChild(feed);
   
   var titleE = DOMUtil.createElementNS(content,ATOM_NS,"title");
   titleE.appendChild(content.createTextNode(collection.title));
   content.documentElement.appendChild(titleE);
   var authorE = DOMUtil.createElementNS(content,ATOM_NS,"author");
   var authorNameE = DOMUtil.createElementNS(content,ATOM_NS,"name");
   authorE.appendChild(authorNameE);
   authorNameE.appendChild(content.createTextNode(this.service.author));
   content.documentElement.appendChild(authorE);
   collection.feed.xml = null;
   collection.feed.entries = {};
   var headers = {};
   if (this.service.authorization) {
      headers["authorization"] = this.service.authorization;
   }

   HTTP("POST",collection.uri.spec,{
      timeout: options.timeout,
      username: this.service.username,
      password: this.service.password,
      headers: headers,
      overrideMimeType: "text/xml",
      body: content,
      contentType: "application/atom+xml",
      returnHeaders: true,
      onSuccess: function(status,feedDoc,entryText,headers) {
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}

AtomWorkspace.prototype.createByPost = function(collection,options) {
   var content = _newXMLDocument();
   var appCollection = DOMUtil.createElementNS(content,ATOM_PUB_INTROSPECT_NS,"collection");
   content.appendChild(appCollection);
   
   var titleE = DOMUtil.createElementNS(content,ATOM_NS,"title");
   titleE.appendChild(content.createTextNode(collection.title));
   content.documentElement.appendChild(titleE);
   if (this.service.author) {
      var authorE = DOMUtil.createElementNS(content,ATOM_NS,"author");
      var authorNameE = DOMUtil.createElementNS(content,ATOM_NS,"name");
      authorE.appendChild(authorNameE);
      authorNameE.appendChild(content.createTextNode(this.service.author));
      content.documentElement.appendChild(authorE);
   }
   collection.feed.xml = null;
   collection.feed.entries = {};
   var headers = {};
   if (this.service.authorization) {
      headers["authorization"] = this.service.authorization;
   }
   if (options && options.slug) {
      headers["Slug"] = options.slug;
   } else {
      var slug = collection.uri.relativeTo(this.uri);
      if (slug.charAt(slug.length-1) === "/") { 
         slug = slug.substring(0,slug.length-1);
      }
      headers["Slug"] = slug;
   }

   HTTP("POST",this.uri.spec,{
      timeout: options.timeout,
      username: this.service.username,
      password: this.service.password,
      headers: headers,
      overrideMimeType: "text/xml",
      body: content,
      contentType: "application/atomsvc+xml",
      returnHeaders: true,
      onSuccess: function(status,feedDoc,entryText,headers) {
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}


AtomWorkspace.prototype.removeByDelete = function(collection,options) {
   var theWorkspace = this;
   var headers = {};
   if (this.service.authorization) {
      headers["authorization"] = this.service.authorization;
   }
   HTTP("DELETE",options && options.uri ? options.uri : collection.uri.spec,{
      timeout: options.timeout,
      username: this.service.username,
      password: this.service.password,
      headers: headers,
      overrideMimeType: "text/xml",
      onSuccess: function(status,feedDoc,entryText,headers) {
         var queue = [];
         queue.push(collection);
         for (var i=0; i<collection.parent.children.length; i++) {
            if (collection.parent.children[i]==collection) {
               collection.parent.children.splice(i,1);
            }
         }
         do {
            var currentCollection = queue.splice(0,1)[0];
            if (currentCollection) {
               for (var i=0; i<currentCollection.children.length; i++) {
                  queue.push(currentCollection.children[i]);
               }
               delete theWorkspace.collections[currentCollection.href];
               theWorkspace.xml.removeChild(currentCollection.xml);
               if (options.onDelete) {
                  options.onDelete(currentCollection);
               }
            }
         } while (queue.length>0);
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}

function AtomCollection(workspace,href,uri,element,titleSpec) {
   this.type = 2;
   this.workspace = workspace;
   this.href = href;
   this.uri = AtomPub.newURI(uri);
   this.xml = element;
   var title = null;
   if (element) {
      _Atom_forChild(
         this.xml,
         "title",
         ATOM_NS,
         function(target) { title = target; } 
      );
      
      if (title) {
         this.title = DOMUtil.text(title);
      } else {
         this.title = element.getAttribute("title");
      }
   } else {
      this.title = titleSpec;
   }
   this.feed = new AtomFeed(this,uri);
   this.parent = null;
   this.children = [];
}

AtomCollection.prototype.newFeed = function() {
   var content = _newXMLDocument();
   var feed = DOMUtil.createElementNS(content,ATOM_NS,"feed");
   content.appendChild(feed);
   
   var titleE = DOMUtil.createElementNS(content,ATOM_NS,"title");
   titleE.appendChild(content.createTextNode(this.title));
   content.documentElement.appendChild(titleE);
   var authorE = DOMUtil.createElementNS(content,ATOM_NS,"author");
   var authorNameE = DOMUtil.createElementNS(content,ATOM_NS,"name");
   authorE.appendChild(authorNameE);
   authorNameE.appendChild(content.createTextNode(this.workspace.service.author));
   content.documentElement.appendChild(authorE);
   this.feed.xml = feed;
}

function AtomFeed(collection,uri,xml) {
   this.type = 3;
   this.collection = collection;
   this.uri = AtomPub.newURI(uri);
   this.loaded = false;
   this.id = null;
   this.xml = xml;
   this.entries = {};
   if (this.xml) {
      this.loaded = true;
      this._loadEntries();
   }
}

AtomFeed.prototype.load = function(options) {
   this.xml = null;
   this.loaded = false;
   this.entries = {};
   var current = this;
   var headers = {};
   if (this.collection && this.collection.workspace.service.authorization) {
      headers["authorization"] = this.collection.workspace.service.authorization;
   }
   HTTP("GET",this.uri.spec,{
      timeout: options.timeout,
      username: this.collection ? this.collection.workspace.service.username : this.username,
      password: this.collection ? this.collection.workspace.service.password : this.password,
      headers: headers,
      overrideMimeType: "text/xml",
      onSuccess: function(status,doc,text) {
         if (!doc || !doc.documentElement) {
            try {
              var parser = new DOMParser();
              doc = parser.parseFromString(text,"text/xml");
            } catch (ex) {
               if (options.onFailure) {
                  options.onFailure(status,doc,text);
               }
            }
         }
         current.xml = doc.documentElement;
         current.loaded = true;
         current._loadEntries();
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}

AtomFeed.prototype._loadEntries = function() {
   this.entries = {};
   if (!this.xml) {
      return;
   }
   this.firstEntry = null;
   var lastEntry = null;
   var current = this.xml.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      var localName = DOMUtil.localName(current);
      if (localName=="id" && DOMUtil.namespaceURI(current)==ATOM_NS) {
         this.id = DOMUtil.text(current);
      } else if (localName=="link" && DOMUtil.namespaceURI(current)==ATOM_NS) {
         var rel = current.getAttribute("rel");
         if (rel=="edit") {
            this.editURL = this.uri.resolve(current.getAttribute("href"));
         } else if (rel=="self") {
            this.selfURL = this.uri.resolve(current.getAttribute("href"));
         }
      } else if (localName=="entry" && DOMUtil.namespaceURI(current)==ATOM_NS) {
         var entry = new AtomEntry(this,current);
         entry.prev = lastEntry;
         if (lastEntry) {
            lastEntry.next = entry;
         }
         if (!this.firstEntry) {
            this.firstEntry = entry;
         }
         this.entries[entry.id] = entry;
         lastEntry = entry;
      }
      current = current.nextSibling;
   }
}

AtomFeed.prototype.create = function(options) {

   var headers = {};
   if (this.collection.workspace.service.authorization) {
      headers["authorization"] = this.collection.workspace.service.authorization;
   }
   var current = this;
   HTTP("POST",this.uri.spec,{
      timeout: options.timeout,
      username: this.collection.workspace.service.username,
      password: this.collection.workspace.service.password,
      headers: headers,
      overrideMimeType: "text/xml",
      body: this.xml.ownerDocument,
      contentType: "application/atom+xml",
      onSuccess: function(status,doc,entryText,headers) {
         if (!doc || !doc.documentElement) {
            try {
              var parser = new DOMParser();
              doc = parser.parseFromString(entryText,"text/xml");
            } catch (ex) {
               if (options.onFailure) {
                  options.onFailure(status,doc,entryText);
               }
            }
         }
         current.xml = doc.documentElement;
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}

AtomFeed.prototype.updateByPut = function(options) {
   var content = _newXMLDocument();
   var feed = DOMUtil.createElementNS(content,ATOM_NS,"feed");
   content.appendChild(feed);
   //var content = this.xml.ownerDocument.implementation.createDocument(ATOM_NS,"feed","");
   //var feed = content.documentElement;
   var current = this.xml.firstChild;
   while (current) {
      if (DOMUtil.localName(current)=="entry" && DOMUtil.namespaceURI(current)==ATOM_NS) {
         break;
      }
      feed.appendChild(AtomPub.importNode(content,current,true));
      current = current.nextSibling;
   }
   var headers = {};
   if (this.collection.workspace.service.authorization) {
      headers["authorization"] = this.collection.workspace.service.authorization;
   }
   
   HTTP("PUT",this.uri.spec,{
      timeout: options.timeout,
      username: this.collection.workspace.service.username,
      password: this.collection.workspace.service.password,
      headers: headers,
      overrideMimeType: "text/xml",
      body: content,
      contentType: "application/atom+xml",
      onSuccess: function(status,feedDoc,entryText,headers) {
         options.onSuccess();
      },
      onFailure: options.onFailure
   });
}

AtomFeed.prototype.getElement = function(name,ns) {
   var e = null;
   _Atom_forChild(this.xml,name,ns ? ns : ATOM_NS,
      function(target) { 
         e = target; 
      }
   );
   return e;
}

AtomFeed.prototype.getElements = function(name,ns) {
   var list = [];
   _Atom_forChild(this.xml,name,ns ? ns : ATOM_NS,
      function(target) { 
         list.push(target); 
      }
   );
   return list;
}

AtomFeed.prototype.getTitle = function(create) {
   var title = null;
   _Atom_forChild(this.xml,"title",ATOM_NS,function(target) { title = target; } );
   if (create && !title) {
      title = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"title");
      title.setAttribute("type","text");
      var firstEntry = null;
      _Atom_forChild(this.xml,"entry",ATOM_NS,function(target) { if (!firstEntry) { firstEntry = target} } );
      if (firstEntry) {
         this.xml.insertBefore(title,firstEntry);
      } else {
         this.xml.appendChild(title);
      }
   }
   return title ? new AtomText(title) : null;
}

AtomFeed.prototype.getSubtitle = function(create) {
   var subtitle = null;
   _Atom_forChild(this.xml,"subtitle",ATOM_NS,function(target) { subtitle = target; } );
   if (create && !subtitle) {
      subtitle = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"subtitle");
      subtitle.setAttribute("type","text");
      var firstEntry = null;
      _Atom_forChild(this.xml,"entry",ATOM_NS,function(target) { if (!firstEntry) { firstEntry = target} } );
      if (firstEntry) {
         this.xml.insertBefore(subtitle,firstEntry);
      } else {
         this.xml.appendChild(subtitle);
      }
   }
   return subtitle ? new AtomText(subtitle) : null;
}
AtomFeed.prototype.addLink = function(rel,href,type,title) {
   var link = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"link");
   link.setAttribute("rel",rel);
   link.setAttribute("href",href);
   if (type) {
      link.setAttribute("type",type);
   }
   if (title) {
      link.setAttribute("title",type);
   }
   var firstEntry = null;
   _Atom_forChild(this.xml,"entry",ATOM_NS,function(target) { if (!firstEntry) { firstEntry = target} } );
   if (firstEntry) {
      this.xml.insertBefore(link,firstEntry);
   } else {
      this.xml.appendChild(link);
   }
   return link;
}


AtomFeed.prototype.getCategories = function() {
   var values = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { values[values.length] = new AtomCategory(e); } );
   return values;
}

AtomFeed.prototype.getCategory = function(scheme,term) {
   var values = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { 
      var cat = new AtomCategory(e);
      if (scheme && cat.scheme!=scheme) {
         return;
      }
      if (term && cat.term!=term) {
         return;
      }
      values.push(cat);
   });
   return values;
}

AtomFeed.prototype.getLinks = function(rel) {
   var values = [];
   _Atom_forChild(this.xml,"link",ATOM_NS,function(e) {
      if (!rel || rel && e.getAttribute("rel")==rel) {
         values[values.length] = new AtomLink(e); 
      }
   });
   return values;
}
AtomFeed.prototype.removeAllLinks = function(matchRel) {
   var toRemove = [];
   _Atom_forChild(this.xml,"link",ATOM_NS,
      function(e) { 
         var rel = e.getAttribute("rel");
         if (matchRel && rel==matchRel) {
            toRemove[toRemove.length] = e;
         } else if (!matchRel && rel!="edit" && rel!="self") {
            toRemove[toRemove.length] = e;
         }
      }
   );
   for (var i=0; i<toRemove.length; i++) {
      this.xml.removeChild(toRemove[i]);
   }
}
AtomFeed.prototype.removeLink = function(link) {
   this.xml.removeChild(link.xml);
}


AtomFeed.prototype.addCategory = function(scheme,term,value) {
   var category = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"category");
   if (scheme && scheme.length>0) {
      category.setAttribute("scheme",scheme);
   }
   category.setAttribute("term",term);
   if (value) {
      category.appendChild(this.xml.ownerDocument.createTextNode(value));
   }     
   var firstEntry = null;
   _Atom_forChild(this.xml,"entry",ATOM_NS,function(target) { if (!firstEntry) { firstEntry = target} } );
   if (firstEntry) {
      this.xml.insertBefore(category,firstEntry);
   } else {
      this.xml.appendChild(category);
   }
   return new AtomCategory(category);
}

AtomFeed.prototype.removeCategory = function(category) {
   this.xml.removeChild(category.xml);
}

AtomFeed.prototype.removeAllCategories = function() {
   var toRemove = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { toRemove[toRemove.length] = e; });
   for (var i=0; i<toRemove.length; i++) {
      this.xml.removeChild(toRemove[i]);
   }
}

AtomFeed.prototype.getId = function() {
   var id = null;
   _Atom_forChild(this.xml,"id",ATOM_NS,function(target) { id = DOMUtil.text(target); } );
   return id;
}

AtomFeed.prototype.addResource = function(content,options) {

   var currentFeed = this;
   var headers = {};
   if (options.slug) {
      headers["Slug"] = options.slug;
   }
   if (this.collection.workspace.service.authorization) {
      headers["authorization"] = this.collection.workspace.service.authorization;
   }
   //consoleService.logStringMessage("Sending entry to "+this.collection.uri+" ...");
   var req = HTTP("POST",this.collection.uri.spec,
      {
         username: this.collection.workspace.service.username,
         password: this.collection.workspace.service.password,
         headers: headers,
         contentType: options.contentType,
         body: content,
         returnHeaders: true,
         onSuccess: function(status,entryDoc,entryText,headers) {
           if ((!entryDoc || !entryDoc.documentElement) && entryText) {
              try {
                var parser = new DOMParser();
                entryDoc = parser.parseFromString(entryText,"text/xml");
              } catch (ex) {
                 if (options.onFailure) {
                    options.onFailure(status,entryDoc,entryText);
                 }
              }
           }
            if (entryDoc) {
               var newXML = AtomPub.importNode(currentFeed.xml.ownerDocument,entryDoc.documentElement,true);
               currentFeed.xml.appendChild(newXML);
               var newEntry = new AtomEntry(currentFeed,newXML);
               options.onSuccess(newEntry);
            } else {
               var location = headers["Location"];
               if (!location) {
                  if (consoleService) {
                     consoleService.logStringMessage("There was no entry or location header returned.");
                  }
                  throw "There was no entry or location header returned.";
               }
               HTTP("GET",headers["location"], {
                  timeout: options.timeout,
                  overrideMimeType: "text/xml",
                  onSuccess: function(status,entryDoc,entryText) {
                     if (!entryDoc || !entryDoc.documentElement) {
                        try {
                          var parser = new DOMParser();
                          entryDoc = parser.parseFromString(entryText,"text/xml");
                        } catch (ex) {
                           if (options.onFailure) {
                              options.onFailure(status,entryDoc,entryText);
                           }
                        }
                     }
                     var newXML = AtomPub.importNode(currentFeed.xml.ownerDocument,entryDoc.documentElement,true);
                     currentFeed.xml.appendChild(newXML);
                     var newEntry = new AtomEntry(currentFeed,newXML);
                     options.onSuccess(newEntry);
                  },
                  onFailure: options.onFailure
               });
            }
         },
         onFailure: function(status,xml,text,headers) {
            if (consoleService) {
               consoleService.logStringMessage("Entry failed: "+status+" "+text);
            }
            options.onFailure(status,xml,text,headers);
         }
      }
   );
}

AtomFeed.prototype._bootstrap = function() {
   var parser = _newDOMParser();
   var doc = parser.parseFromString("<feed xmlns='http://www.w3.org/2005/Atom'/>","text/xml");
   this.xml = doc.documentElement;
}
AtomFeed.prototype.createEntry = function(title) {
   if (!this.xml) {
      this._bootstrap();
   }
   var doc = this.xml.ownerDocument;
   var entry = DOMUtil.createElementNS(doc,ATOM_NS,"entry");
   /*
   var idE = DOMUtil.createElementNS(entry.ownerDocument,ATOM_NS,"id");
   var id = "temp:"+(new Date()).getTime();
   idE.appendChild(doc.createTextNode(id));
   entry.appendChild(idE);
   */
   var titleE = DOMUtil.createElementNS(doc,ATOM_NS,"title");
   titleE.appendChild(doc.createTextNode(title));
   entry.appendChild(titleE);
   if (this.collection) {
      var authorE = DOMUtil.createElementNS(doc,ATOM_NS,"author");
      var authorNameE = DOMUtil.createElementNS(doc,ATOM_NS,"name");
      authorNameE.appendChild(doc.createTextNode(this.collection.workspace.service.author));
      authorE.appendChild(authorNameE);
      entry.appendChild(authorE);
   }
   var aentry = new AtomEntry(this,entry);
   aentry.local = true;
   return aentry;
}

AtomFeed.prototype.addEntry = function(entry) {
   this.xml.appendChild(entry.xml);
   this.entries[entry.id] = entry;
}

AtomFeed.prototype.copyEntry = function(otherEntry) {
   return this.addEntryFromXML(otherEntry.xml);
}

AtomFeed.prototype.addEntryFromXML = function(otherEntry,removeEditLink) {
   if (!this.xml) {
      this._bootstrap();
   }
   var entry = AtomPub.importNode(this.xml.ownerDocument,otherEntry,true);
   var id = null;
   _Atom_forChild(entry,"id",ATOM_NS,function(target) { id = target; } );
   if (!id) {
      id = DOMUtil.createElementNS(entry.ownerDocument,ATOM_NS,"id");
      entry.appendChild(id);
      var idValue = "temp:"+(new Date()).getTime();
      id.appendChild(this.xml.ownerDocument.createTextNode(idValue));
   }

   if (removeEditLink) {
      var editLink = null;
      _Atom_forChild(entry,"link",ATOM_NS,
          function(target) { 
             if (target.getAttribute("rel")=="edit") { 
                editLink = target; 
             }
          }
      );
      if (editLink) {
         entry.removeChild(editLink);
      }
   }
   this.xml.appendChild(entry);
   var aentry = new AtomEntry(this,entry);
   aentry.local = true;
   this.entries[aentry.id] = aentry;
   return aentry;
}

AtomFeed.prototype.removeEntry = function(entry,options) {
   if (this.entries[entry.id]) {
      if (entry.local) {
         delete this.entries[entry.id];
         this.xml.removeChild(entry.xml);
         if (options) {
            options.onSuccess();
         }
      } else {
         if (!entry.editURL) {
            throw "Entry "+entry.id+" does not have an edit link.";
         }
         var current = this;
         var headers = {};
         var hasService = this.collection && this.collection.workspace && this.collection.workspace.service;
         if (hasService && this.collection.workspace.service.authorization) {
            headers["authorization"] = this.collection.workspace.service.authorization;
         }
         HTTP("DELETE",entry.editURL.spec,{
            username: hasService ? this.collection.workspace.service.username : null,
            password: hasService ? this.collection.workspace.service.password : null,
            headers: headers,
            timeout: options.timeout,
            overrideMimeType: "text/xml",
            onSuccess: function(status,entryDoc,entryText,headers) {
               delete current.entries[entry.id];
               current.xml.removeChild(entry.xml);
               if (options.onSuccess) {
                  options.onSuccess();
               }
            },
            onFailure: options.onFailure
         });
      }
   } else {
      throw "Entry "+entry.id+" does not belong to this feed.";
   }
}

AtomFeed.prototype.getEntriesByCategory = function(scheme,term,value)
{
   var matches = [];
   for (var id in this.entries) {
      var entry = this.entries[id];
      var match = null;
      var matchValue = null;
     _Atom_forChild(entry.xml,"category",ATOM_NS,function(e) { 
        if (scheme && e.getAttribute("scheme")!=scheme) {
           return;
        }
        if (term && e.getAttribute("term")!=term) {
           return; 
        }
        if (value) {
           matchValue = DOMUtil.text(match);
           if (matchValue==value) {
              match = e;
           }
        } else if (!value) {
           match = e;
        }
     });
     if (match) {
        matches.push({
           value: matchValue,
           entry: entry
        });
     }
   }
   return matches;   
}

AtomFeed.prototype.sortByCategory = function(scheme,term,sortby) {
   // copy the entries;
   var sorted = [];
   for (var id in this.entries) {
      var entry = this.entries[id];
      var match = null;
     _Atom_forChild(entry.xml,"category",ATOM_NS,function(e) { 
        if (scheme && e.getAttribute("scheme")!=scheme) {
           return;
        }
        if (term && e.getAttribute("term")!=term) {
           return; 
        }
        if (!match) {
           match = e;
        }
     });
     sorted.push({
        value: match ? DOMUtil.text(match) : null,
        entry: entry           
     });
   }
   sorted.sort(sortby ? sortby : function(a,b) {
      if (a.value && b.value) {
         return a.value < b.value ? -1 : (a.value>b.value ? 1 : 0);
      } else if (!a.value) {
         return 1;
      } else {
         return -1;
      }
   });
   return sorted;
}

function AtomNumericSort(a,b) {
   if (a.value && b.value) {
      return parseInt(a.value)-parseInt(b.value);
   } else if (!a.value) {
      return 1;
   } else {
      return -1;
   }
}


AtomFeed.prototype.sortByElement = function(name,ns,sortby) {
   // copy the entries;
   var sorted = [];
   for (var id in this.entries) {
      var entry = this.entries[id];
      sorted.push({
         element: entry.getElement(name,ns),
         entry: entry
      });
   }
   sorted.sort(sortby ? sortby : function(a,b) {
      var avalue = DOMUtil.text(a.element);
      var bvalue = DOMUtil.text(b.element);
      return avalue < bvalue ? -1 : (avalue>bvalue ? 1 : 0);
   });
   return sorted;
}
function AtomEntry(feed,entry) {
   this.type = 4;
   this.feed = feed;
   this.xml = entry;
   this.id = null;
   this.local = false;
   this.editURL = null;
   this._idChanged();
   this._editURLChanged();
}

AtomEntry.prototype.addLink = function(rel,href,type,title) {
   var link = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"link");
   link.setAttribute("rel",rel);
   link.setAttribute("href",href);
   if (type) {
      link.setAttribute("type",type);
   }
   if (title) {
      link.setAttribute("title",type);
   }
   this.xml.appendChild(link);
   return link;
}

AtomEntry.prototype.getAlternateLink = function() {
   var link = null;
   _Atom_forChild(this.xml,"link",ATOM_NS,
   function(target) { 
      if (target.getAttribute("rel")=="alternate") {
         link = target; 
      }
   }
   );
   return link;
}
   
AtomEntry.prototype.getElement = function(name,ns) {
   var e = null;
   _Atom_forChild(this.xml,name,ns ? ns : ATOM_NS,
      function(target) { 
         e = target; 
      }
   );
   return e;
}

AtomEntry.prototype.getElements = function(name,ns) {
   var list = [];
   _Atom_forChild(this.xml,name,ns ? ns : ATOM_NS,
      function(target) { 
         list.push(target); 
      }
   );
   return list;
}

AtomEntry.prototype.getTitle = function() {
   var title = null;
   _Atom_forChild(this.xml,"title",ATOM_NS,function(target) { title = target; } );
   if (!title) {
      throw "Entry "+this.id+" is missing a title.";
   }
   return new AtomText(title);
}
AtomEntry.prototype.getSummary = function(create) {
   var summary = null;
   _Atom_forChild(this.xml,"summary",ATOM_NS,function(target) { summary = target; } );
   if (create && !summary) {
      summary = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"summary");
      this.xml.appendChild(summary);
      summary.setAttribute("type","text");
   }
   return summary ? new AtomText(summary) : null;
}

AtomEntry.prototype.getPublished = function() {
   var e = null;
   _Atom_forChild(this.xml,"published",ATOM_NS,function(target) { e = target; } );
   return e ? DOMUtil.text(e) : null;
}

AtomEntry.prototype.getUpdated = function() {
   var e = null;
   _Atom_forChild(this.xml,"updated",ATOM_NS,function(target) { e = target; } );
   return e ? DOMUtil.text(e) : null;
}

AtomEntry.prototype.getEdited = function() {
   var e = null;
   _Atom_forChild(this.xml,"edited",ATOM_PUB_INTROSPECT_NS,function(target) { e = target; } );
   return e ? DOMUtil.text(e) : null;
}


AtomEntry.prototype.getContent = function(create) {
   var content = null;
   _Atom_forChild(this.xml,"content",ATOM_NS,function(target) { content = target; } );
   if (create && !content) {
      content = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"content");
      this.xml.appendChild(content);
      content.setAttribute("type","text");
   }
   return content ? new AtomContent(this,content) : null;
}


AtomEntry.prototype.getLinks = function(rel) {
   var values = [];
   _Atom_forChild(this.xml,"link",ATOM_NS,function(e) {
      if (!rel || rel && e.getAttribute("rel")==rel) {
         values[values.length] = new AtomLink(e); 
      }
   });
   return values;
}

AtomEntry.prototype.removeLink = function(link) {
   this.xml.removeChild(link.xml);
}

AtomEntry.prototype.removeAllLinks = function(matchRel) {
   var toRemove = [];
   _Atom_forChild(this.xml,"link",ATOM_NS,
      function(e) { 
         var rel = e.getAttribute("rel");
         if (matchRel && rel==mathcRel) {
            toRemove[toRemove.length] = e;
         } else if (!matchRel && rel!="edit" && rel!="self") {
            toRemove[toRemove.length] = e;
         }
      }
   );
   for (var i=0; i<toRemove.length; i++) {
      this.xml.removeChild(toRemove[i]);
   }
}

AtomEntry.prototype.getCategories = function() {
   var values = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { values[values.length] = new AtomCategory(e); } );
   return values;
}

AtomEntry.prototype.getCategory = function(scheme,term) {
   var values = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { 
      var cat = new AtomCategory(e);
      if (scheme && cat.scheme!=scheme) {
         return;
      }
      if (term && cat.term!=term) {
         return;
      }
      values.push(cat);
   });
   return values;
}



AtomEntry.prototype.addCategory = function(scheme,term,value) {
   var category = DOMUtil.createElementNS(this.xml.ownerDocument,ATOM_NS,"category");
   if (scheme && scheme.length>0) {
      category.setAttribute("scheme",scheme);
   }
   category.setAttribute("term",term);
   if (value) {
      category.appendChild(this.xml.ownerDocument.createTextNode(value));
   }     
   this.xml.appendChild(category);
   return new AtomCategory(category);
}

AtomEntry.prototype.removeCategory = function(category) {
   this.xml.removeChild(category.xml);
}

AtomEntry.prototype.removeAllCategories = function() {
   var toRemove = [];
   _Atom_forChild(this.xml,"category",ATOM_NS,function(e) { toRemove[toRemove.length] = e; });
   for (var i=0; i<toRemove.length; i++) {
      this.xml.removeChild(toRemove[i]);
   }
}

AtomEntry.prototype._xmlChanged = function(xml) {
   if ((DOMUtil.localName(xml) == "parsererror") &&
       (DOMUtil.namespaceURI(xml) == "http://www.mozilla.org/newlayout/xml/parsererror.xml")) {
      throw "There was a parse error on the returned XML: "+DOMUtil.text(xml.firstChild);
   }
   if (DOMUtil.namespaceURI(xml)!=ATOM_NS || DOMUtil.localName(xml)!="entry") {
      throw "The entry element is {"+DOMUtil.namespaceURI(xml)+"}"+DOMUtil.localName(xml)+" which is not {"+ATOM_NS+"}entry as expected.";
   }
   var newXML = AtomPub.importNode(this.feed.xml.ownerDocument,xml,true);
   if (!this.local) {
      this.feed.xml.replaceChild(newXML,this.xml);
   }
   this.xml = newXML;
   this._idChanged();
   this._editURLChanged();
}

AtomEntry.prototype._idChanged = function() {
   var id = null;
   _Atom_forChild(this.xml,"id",ATOM_NS,function(target) { id = DOMUtil.text(target); } );
   if (!id) {
      return;
   }
   if (this.feed && this.feed.entries[this.id] && id!=this.id) {
      var oldId = this.id;
      delete this.feed.entries[oldId];
   }
   this.id = id;
   if (this.feed) {
      this.feed.entries[id] = this;
   }
}

AtomEntry.prototype._editURLChanged = function() {
   this.editURL = null;
   var currentEntry = this;
   if (this.feed) {
     _Atom_forChild(this.xml,"link",ATOM_NS,
         function(target) { 
            if (target.getAttribute("rel")=="edit") { 
               currentEntry.editURL = currentEntry.feed.uri.resolve(target.getAttribute("href")); 
            }
         }
     );
   }
}

AtomEntry.prototype.refresh = function(options) {
   var url = null;
   var self = this.getLinks("self");
   if (self.length>0) {
      url = this.feed.uri.resolve(self[0].href);
   } else {
      var edit = this.getLinks("edit");
      if (edit.length>0) {
         url = this.feed.uri.resolve(edit[0].href);
      } else if (this.id.substring(0,9)=="urn:uuid:") {
         url = this.feed.uri.resolve("_/"+this.id.substring(9)+"/");
      }
   }
   var current = this;
   HTTP("GET",url.spec, {
      username: this.feed.collection ? this.feed.collection.workspace.service.username : null,
      password: this.feed.collection ? this.feed.collection.workspace.service.password : null,
      timeout: options.timeout,
      overrideMimeType: "text/xml",
      onSuccess: function(status,entryDoc,entryText) {
         if (!entryDoc || !entryDoc.documentElement) {
            try {
              var parser = new DOMParser();
              entryDoc = parser.parseFromString(entryText,"text/xml");
            } catch (ex) {
               if (options.onFailure) {
                  options.onFailure(status,entryDoc,entryText);
               }
            }
         }
         current._xmlChanged(entryDoc.documentElement);
         options.onSuccess(status);
      },
      onFailure: options.onFailure
   });
}

AtomEntry.prototype.save = function(options) {
   var serializer = _newXMLSerializer();
   var content = serializer.serializeToString(this.xml);
   if (this.local) {
      // new entry
      var currentEntry = this;
      var headers = {};
      if (this.feed.collection && this.feed.collection.workspace.service.authorization) {
         headers["authorization"] = this.feed.collection.workspace.service.authorization;
      }
      HTTP("POST",this.feed.collection ? this.feed.collection.uri.spec : this.feed.editURL.spec,{
         username: this.feed.collection ? this.feed.collection.workspace.service.username : null,
         password: this.feed.collection ? this.feed.collection.workspace.service.password : null,
         headers: headers,
         timeout: options.timeout,
         overrideMimeType: "text/xml",
         body: content,
         contentType: "application/atom+xml;charset=utf-8",
         returnHeaders: true,
         onSuccess: function(status,entryDoc,entryText,headers) {
             if ((!entryDoc || !entryDoc.documentElement) && entryText.length>0) {
                try {
                  var parser = new DOMParser();
                  entryDoc = parser.parseFromString(entryText,"text/xml");
                } catch (ex) {
                   if (options.onFailure) {
                      options.onFailure(status,entryDoc,entryText);
                   }
                }
             }
            if (entryText.length>0) {
               try {
                  currentEntry._xmlChanged(entryDoc.documentElement);
                  currentEntry.feed.addEntry(currentEntry);
                  currentEntry.local = false;
                  options.onSuccess();
               } catch (ex) {
                  options.onFailure(500,null,ex);
               }
            } else {
               var location = headers["Location"];
               if (!location) {
                  throw "There was no entry or location header returned.";
               }
               HTTP("GET",location, {
                  username: currentEntry.feed.collection ? currentEntry.feed.collection.workspace.service.username : null,
                  password: currentEntry.feed.collection ? currentEntry.feed.collection.workspace.service.password : null,
                  timeout: options.timeout,
                  overrideMimeType: "text/xml",
                  onSuccess: function(status,entryDoc,entryText) {
                     if (!entryDoc || !entryDoc.documentElement) {
                        try {
                          var parser = new DOMParser();
                          entryDoc = parser.parseFromString(entryText,"text/xml");
                        } catch (ex) {
                           if (options.onFailure) {
                              options.onFailure(status,entryDoc,entryText);
                           }
                        }
                     }
                     currentEntry._xmlChanged(entryDoc.documentElement);
                     currentEntry.feed.addEntry(currentEntry);
                     currentEntry.local = false;
                     options.onSuccess();
                  },
                  onFailure: options.onFailure
               });
            }
         },
         onFailure: options.onFailure
      });
   } else {
      // existing entry
      if (!this.editURL) {
         throw new "Cannot save changed to entry "+this.id+" as the entry does not have an edit relation link.";
      }
      var currentEntry = this;
      var headers = {};
      if (this.feed.collection  && this.feed.collection.workspace.service.authorization) {
         headers["authorization"] = this.feed.collection.workspace.service.authorization;
      }
      HTTP("PUT",this.editURL.spec,{
         username: this.feed.collection ? this.feed.collection.workspace.service.username : null,
         password: this.feed.collection ? this.feed.collection.workspace.service.password : null,
         headers: headers,
         timeout: options.timeout,
         overrideMimeType: "text/xml",
         body: content,
         contentType: "application/atom+xml;charset=utf-8",
         returnHeaders: true,
         onSuccess: function(status,entryDoc,entryText,headers) {
             if ((!entryDoc || !entryDoc.documentElement) && entryText.length>0) {
                try {
                  var parser = new DOMParser();
                  entryDoc = parser.parseFromString(entryText,"text/xml");
                } catch (ex) {
                   if (options.onFailure) {
                      options.onFailure(status,entryDoc,text);
                   }
                }
             }
            if (entryText.length>0) {
               try {
                  currentEntry._xmlChanged(entryDoc.documentElement);
                  options.onSuccess();
               } catch (ex) {
                  options.onFailure(0,null,ex);
               }
            } else {
               var location = headers["Location"];
               if (!location) {
                  location = currentEntry.editURL.spec;
               }
               HTTP("GET",location, {
                  username: currentEntry.feed.collection ? currentEntry.feed.collection.workspace.service.username : null,
                  password: currentEntry.feed.collection ? currentEntry.feed.collection.workspace.service.password : null,
                  timeout: options.timeout,
                  overrideMimeType: "text/xml",
                  onSuccess: function(status,entryDoc,entryText) {
                     if (!entryDoc || !entryDoc.documentElement) {
                        try {
                          var parser = new DOMParser();
                          entryDoc = parser.parseFromString(entryText,"text/xml");
                        } catch (ex) {
                           if (options.onFailure) {
                              options.onFailure(status,entryDoc,entryText);
                           }
                        }
                     }
                     currentEntry._xmlChanged(entryDoc.documentElement);
                     options.onSuccess();
                  },
                  onFailure: options.onFailure
               });
            }
         },
         onFailure: options.onFailure
      });
   }
}

function AtomText(element) {
   this.xml = element;
}


AtomText.prototype.getType = function() { 
   return this.xml.getAttribute("type"); 
}

AtomText.prototype.setType = function(v) {
   this.xml.setAttribute("type",v); 
   this.xml.removeAttribute("src");
}

AtomText.prototype.getXHTMLContent = function() {
   var div = null;
   _Atom_forChild(this.xml,"div","http://www.w3.org/1999/xhtml",function(target) { div = target; } );
   return div;
}

AtomText.prototype.getNodeContent = function() {
   var current = this.xml.firstChild;
   while (current) {
      if (current.nodeType==1) {
         return current;
      }
      current = current.nextSibling;
   }
   return null;
}
AtomText.prototype.setTextContent = function(text) {
   _Atom_removeChildren(this.xml);
   this.xml.appendChild(this.xml.ownerDocument.createTextNode(text));
}

AtomText.prototype.setNodeContent = function(node) {
   if (this.xml.ownerDocument!=node.ownerDocument) {
      node = AtomPub.importNode(this.xml.ownerDocument,node,true);
   }
   _Atom_removeChildren(this.xml);
   this.xml.appendChild(node);
}

function AtomContent(entry,element) {
   this.entry = entry;
   this.xml = element;
}
AtomContent.prototype = new AtomText();
AtomContent.prototype.getMediaReference = function() {
   return this.xml.getAttribute("src");
}
AtomContent.prototype.setMediaReference = function(src,mediaType) {
   this.xml.setAttribute("src",src); 
   this.xml.removeAttribute("type");
   if (mediaType) {
      this.xml.setAttribute("type",mediaType); 
   }
   _Atom_removeChildren(this.xml);
}

AtomContent.prototype.getMediaType = function() {
   return this.xml.getAttribute("type");
}
AtomContent.prototype.setMediaType = function(mediaType) {
   this.xml.setAttribute("type",mediaType); 
}

AtomContent.prototype.getLocation = function() {
   var baseURI = this.entry.xml.getAttribute("xml:base");
   if (baseURI) {
      var uri = AtomPub.newURI(baseURI);
      return uri.resolve(this.xml.getAttribute("src"));
   } else if (this.entry.feed) {
      return this.entry.feed.uri.resolve(this.xml.getAttribute("src"));
   } else {
      return this.xml.getAttribute("src");
   }
}

AtomContent.prototype.update = function(content,options) 
{

   var currentFeed = this;
   var headers = {};
   if (this.entry.feed.collection.workspace.service.authorization) {
      headers["authorization"] = this.entry.feed.collection.workspace.service.authorization;
   }
   var contentType = options.contentType ? options.contentType : this._type;
   var req = HTTP("PUT",this.getLocation().spec,
      {
         username: this.entry.feed.collection.workspace.service.username,
         password: this.entry.feed.collection.workspace.service.password,
         headers: headers,
         contentType: contentType,
         body: content,
         returnHeaders: true,
         onSuccess: function(status,entryDoc,entryText,headers) {
            options.onSuccess();
         },
         onFailure: function(status,xml,text,headers) {
            options.onFailure(status,xml,text,headers);
         }
      }
   );
}

AtomContent.prototype.getMedia = function(options) 
{
   var req = HTTP("GET",this.getLocation().spec,
      {
         username: this.entry.feed.collection.workspace.service.username,
         password: this.entry.feed.collection.workspace.service.password,
         onSuccess: options.onSuccess,
         onFailure: options.onFailure
      }
   );
}

function AtomCategory(element) {
   this.xml = element;
   this.scheme = element ? element.getAttribute("scheme") : null;
   this.term = element ? element.getAttribute("term") : null;
   this.value = element ? DOMUtil.text(element) : null;
}

AtomCategory.prototype.set = function(scheme,term,value) {
   if (scheme) {
      this.xml.setAttribute("scheme",scheme);
   } else {
      this.xml.removeAttribute("scheme");
   }
   this.xml.setAttribute("term",term);
   _Atom_removeChildren(this.xml);
   if (value) {
      this.xml.appendChild(this.xml.ownerDocument.createTextNode(value)); 
   }
   this.scheme = scheme;
   this.term = term;
   this.value = value;
}


function AtomLink(element) {
   this.xml = element;
   this.rel = element ? element.getAttribute("rel") : null;
   this.href = element ? element.getAttribute("href") : null;
   this.type = element ? element.getAttribute("type") : null;
   this.title = element ? element.getAttribute("title") : null;
}

AtomLink.prototype.set = function(rel,href,type,title)
{
   this.xml.setAttribute("rel",rel);
   this.xml.setAttribute("href",href);
   if (type) {
      this.xml.setAttribute("type",type);
   } else {
      this.xml.removeAttribute("type")
   }
   if (title) {
      this.xml.setAttribute("type",type);
   } else {
      this.xml.removeAttribute("title")
   }
   this.rel = rel;
   this.href = href;
   this.type = type;
   this.title = title;
}

AtomPub.newURI = function(spec) {
   return new AtomURI(spec);
}

function AtomURI(spec) {
   this.spec = spec;
   this.parse();
}

AtomURI.prototype.parse = function() {
   var pos = 0;
   for (; pos<this.spec.length && this.spec.charAt(pos)!=':'; pos++);
   if (pos==this.spec.length) {
      throw "Bad URI value, no scheme: "+this.spec;
   }
   this.scheme = this.spec.substring(0,pos);
   this.schemeSpecificPart = this.spec.substring(pos+1);
   pos++;
   if (this.spec.charAt(pos)=='/' && this.spec.charAt(pos+1)=='/') {
      this.parseGeneric();
   }
}

AtomURI.prototype.parseGeneric = function() {
   if (this.schemeSpecificPart.charAt(0)!='/' || this.schemeSpecificPart.charAt(1)!='/') {
      throw "Generic URI values should start with '//':"+this.spec;
   }
   
   var work = this.schemeSpecificPart.substring(2);
   var pathStart = work.indexOf("/");
   if (pathStart<0) {
      throw "There must be a server specification: "+this.spec;
   }
   this.authority = work.substring(0,pathStart);
   this.path = work.substring(pathStart);
   var hash = this.path.indexOf('#');
   if (hash>=0) {
      this.fragment = this.path.substring(hash+1);
      this.path = this.path.substring(0,hash);
   }
   var questionMark = this.path.indexOf('?');
   if (questionMark>=0) {
      this.query = this.path.substring(questionMark+1);
      this.path = this.path.substring(0,questionMark);
   }
   if (this.path=="/" || this.path=="") {
      this.segments = [];
   } else {
      this.segments = this.path.split(/\//);
      if (this.segments.length>0 && this.segments[0]=='' && this.path.length>1 && this.path.charAt(1)!='/') {
         // empty segment at the start, remove it
         this.segments.shift();
      }
      if (this.segments.length>0 && this.path.length>0 && this.path.charAt(this.path.length-1)=='/' && this.segments[this.segments.length-1]=='') {
         // we may have an empty the end
         // check to see if it is legimate
         if (this.path.length>1 && this.path.charAt(this.path.length-2)!='/') {
            this.segments.pop();
         }
      }
   }
   this.isGeneric = true;
}
   
AtomURI.prototype.resolve = function(href) {
   if (!href) {
      return new AtomURI(this.spec);
   }
   if (!this.isGeneric) {
      throw "Cannot resolve uri against non-generic URI: "+this.spec;
   }
   if (href.charAt(0)=='/') {
      return new AtomURI(this.scheme+"://"+this.authority+href);
   } else if (href.charAt(0)=='.' && href.charAt(1)=='/') {
      if (this.path.charAt(this.path.length-1)=='/') {
         return new AtomURI(this.scheme+"://"+this.authority+this.path+href.substring(2));
      } else {
         var last = this.path.lastIndexOf('/');
         return new AtomURI(this.scheme+"://"+this.authority+this.path.substring(0,last)+href.substring(1));
      }
   } else {
      return new AtomURI(this.scheme+"://"+this.authority+this.path+href);
   }
}

AtomURI.prototype.relativeTo = function(otherURI) {
   if (otherURI.scheme!=this.scheme) {
      return this.spec;
   }
   if (!this.isGeneric) {
      throw "A non generic URI cannot be made relative: "+this.spec;
   }
   if (!otherURI.isGeneric) {
      throw "Cannot make a relative URI against a non-generic URI: "+otherURI.spec;
   }
   if (otherURI.authority!=this.authority) {
      return this.spec;
   }
   var i=0;
   for (; i<this.segments.length && i<otherURI.segments.length; i++) {
      if (this.segments[i]!=otherURI.segments[i]) {
         //alert(this.path+" different from "+otherURI.path+" at '"+this.segments[i]+"' vs '"+otherURI.segments[i]+"'");
         var relative = "";
         for (var j=i; j<otherURI.segments.length; j++) {
            relative += "../";
         }
         for (var j=i; j<this.segments.length; j++) {
            relative += this.segments[j];
            if ((j+1)<this.segments.length) {
               relative += "/";
            }
         }
         if (this.path.charAt(this.path.length-1)=='/') {
            relative += "/";
         }
         return relative;
      }
   }
   if (this.segments.length==otherURI.segments.length) {
      return this.hash ? this.hash : (this.query ? this.query : "");
   } else if (i<this.segments.length) {
      var relative = "";
      for (var j=i; j<this.segments.length; j++) {
         relative += this.segments[j];
         if ((j+1)<this.segments.length) {
            relative += "/";
         }
      }
      if (this.path.charAt(this.path.length-1)=='/') {
         relative += "/";
      }
      return relative;
   } else {
      throw "Cannot calculate a relative URI for "+this.spec+" against "+otherURI.spec;
   }
}

