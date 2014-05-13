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

var DOMUtil = {
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
},

/*
append: function(parent,xml,contentType) {
   if (!parent) {
      alert("Null parent called to DOMUtil.append()");
   }
   var parser = new DOMParser();
   var doc = parser.parseFromString(xml,contentType || "application/xml");
   var node = this.importNode(parent.ownerDocument,doc.documentElement,true);
   parent.appendChild(node);
},*/

element: function(owner,options,append,defaultNS) {
   var e = null;
   var doc = owner;
   if (doc.ownerDocument) {
      doc = doc.ownerDocument;
   }
      //alert("{"+options.namespace+"}"+options.localName);
   if (options.namespace) {
      e = this.createElementNS(doc,options.namespace,options.localName);
   } else {
      e = doc.createElement(options.localName);
   }
   if (options.attributesList) {
      for (var i=0; i<options.attributesList.length; i++) {
         if (options.attributesList[i].namespace) {
            if (this.isIE) {
               var att = doc.createNode(2,options.attributesList[i].localName,options.attributesList[i].namespace);
               att.nodeValue = options.attributesList[i].value;
               e.setAttributeNode(att);
            } else {
              e.setAttributeNS(options.attributesList[i].namespace,options.attributesList[i].localName,options.attributesList[i].value);
            }
         } else {
            e.setAttribute(options.attributesList[i].localName,options.attributesList[i].value);
         }
      }
   }
   if (options.attributes) {
      for (var localName in options.attributes) {
         var attr = options.attributes[localName];
         if (typeof(attr)=='string') {
            e.setAttribute(localName,attr);
         } else if (attr && attr.namespace) {
            if (this.isIE) {
               var att = doc.createNode(2,attr.localName,attr.namespace);
               att.nodeValue = attr.value;
               e.setAttributeNode(att);
            } else {
              e.setAttributeNS(attr.namespace,attr.localName ? attr.localName : localName,attr.value);
            }
         } else if (attr){
            e.setAttribute(attr.localName ? attr.localName : localName,attr.value);
         }
      }
   }
   if (options.className) {
      e.setAttribute("class",options.className);
      e.className = options.className;
   }
   if (append) {
      owner.appendChild(e);
   }
   if (options.children) {
      for (var i=0; i<options.children.length; i++) {
         if (options.children[i]) {
            if (options.children[i].localName) {
               // an element
               if (defaultNS && options.namespace && !options.children[i].namespace) {
                  options.children[i].namespace = options.namespace;
               }
               this.element(e,options.children[i],true,defaultNS);
            } else {
               e.appendChild(doc.createTextNode(options.children[i]));
            }
         }
      }
   }
   return e;
},

remove: function(node) {
   node.parentNode.removeChild(node);
},

clearChildren: function(parent) {
   if (parent && parent.childNodes) {
      while (parent.childNodes.length>0) {
         parent.removeChild(parent.childNodes.item(0));
      }
   }
},

forChild: function(parent,namespace,name,handler) {
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      //alert("{"+current.namespaceURI+"}"+current.localName+" vs {"+namespace+"}"+name);
      if (current.localName) {
         if (namespace && (current.localName==name || (this.toLower && current.localName.toLowerCase()==name)) && current.namespaceURI==namespace) {
            handler(current);
         } else if (current.localName==name || (this.toLower && current.localName.toLowerCase()==name)) {
            handler(current);
         }
      } else if (current.nodeName==name|| (this.toLower && current.nodeName.toLowerCase()==name)) {
         handler(current);
      }
      current = current.nextSibling;
   }
},

filter: function(parent,namespace,name,handler) {
   var list = [];
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      //alert("{"+current.namespaceURI+"}"+current.localName+" vs {"+namespace+"}"+name);
      var result = null;
      if (current.localName) {
         if (namespace && (current.localName==name || (this.toLower && current.localName.toLowerCase()==name)) && current.namespaceURI==namespace) {
            result = handler(current);
         } else if (current.localName==name|| (this.toLower && current.localName.toLowerCase()==name)) {
            result = handler(current);
         }
      } else if (current.nodeName==name|| (this.toLower && current.nodeName.toLowerCase()==name)) {
         result = handler(current);
      }
      if (result) {
         list[list.length] = result;
      }
      current = current.nextSibling;
   }
   return list;
},

attr: function(name,value) {
   return function (e) { alert(e.getAttribute(name)); if (e.getAttribute(name)==value) { return e } else { return null } };
},

firstChild: function(parent,namespace,name) {
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      //alert("{"+current.namespaceURI+"}"+current.localName+" vs {"+namespace+"}"+name);
      if (current.localName) {
         if (namespace && (current.localName==name || (this.toLower && current.localName.toLowerCase()==name)) && current.namespaceURI==namespace) {
            return current;
         } else if (current.localName==name|| (this.toLower && current.localName.toLowerCase()==name)) {
            return current;
         }
      } else if (current.nodeName==name|| (this.toLower && current.nodeName.toLowerCase()==name)) {
         return current;
      }
      current = current.nextSibling;
   }
   return null;
},

text: function(e,value) {
   if (value) {
      this.clearChildren(e);
      e.appendChild(e.ownerDocument.createTextNode(value));
   } else {
      if (e.innerText) {
         return e.innerText;
      } else if (e.textContent) {
         return e.textContent;
      } else {
         var text = "";
         var current = e.firstChild;
         while (current) {
            if (current.nodeType==3) {
               text += current.nodeValue;
            } else if (current.nodeType==1) {
               text += this.text(current);
            }
            current = current.nextSibling;
         }
         return text;
      }
   }
},

textOf: function(parent,namespace,name) {
   var text = "";
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      //alert("{"+current.namespaceURI+"}"+current.localName+" vs {"+namespace+"}"+name);
      if (namespace && current.localName==name && current.namespaceURI==namespace) {
         text += this.text(current);
      } else if (current.localName==name) {
         text += this.text(current);
      }
      current = current.nextSibling;
   }
   return text;
},

findDescendant: function(parent,namespace,name,handler)
{
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      if (current.localName) {
         if (!namespace && (current.localName==name|| (this.toLower && current.localName.toLowerCase()==name))) {
            handler(current);
         } else if ((current.localName==name || (this.toLower && current.localName.toLowerCase()==name)) && current.namespaceURI==namespace) {
            handler(current);
         }
      } else if (current.nodeName==name || (this.toLower && current.nodeName.toLowerCase()==name)) {
         handler(current);
      }
      DOMUtil.findDescendant(current,namespace,name,handler)
      current = current.nextSibling;
   }
},

encodeString: function(value,attribute)
{
   if (attribute) {
      value = value.replace(/'/, '"');
      value = value.replace(/"/, "'");
      value = value.replace(/>/,"&gt;");
   }
   value = value.replace(/</,"&lt;");
   value = value.replace(/&/,"&amp;");
   return value;
},

setAttribute: function(element,name,namespace,value) {
   var doc = element.ownerDocument;
   if (namespace) {
      if (this.isIE) {
         var att = doc.createNode(2,name,namespace);
         att.nodeValue = value;
         element.setAttributeNode(att);
      } else {
         element.setAttributeNS(namespace,name,value);
      }
   } else {
      element.setAttribute(name,value);
   }
},
namespaceURI: function(node) {
   return this.isIE ? (node.namespaceURI ? node.namespaceURI : node.tagUrn) : node.namespaceURI;
},
localName: function(node) {
   if (node.localName) {
      return node.localName;
   } else {
      var name = node.nodeName;
      var colon = name.indexOf(':');
      return colon>=0 ? name.substring(colon+1) : name;
   }
}

}
