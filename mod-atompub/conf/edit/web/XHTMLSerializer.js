function XHTMLSerializer() {
   this.forceLowerCase = false;
   this.isIE = typeof ActiveXObject != "undefined";
}

XHTMLSerializer.prototype.serializeToString = function(node) {
   return this._serialize("",node);
}

XHTMLSerializer.prototype._serialize = function(text,node) {
   switch (node.nodeType) {
      case 1:
         text += "<";
         if (!this.forceLowerCase && node.namespaceURI) {
            text += node.nodeName;
         } else {
            text += node.nodeName.toLowerCase();
         }
         for (var i=0; i<node.attributes.length; i++) {
            var att = node.attributes[i];
            if (att.name=="_moz_dirty") {
               continue;
            }
            if (att.name=="xml:space") {
               continue;
            }
            if (this.isIE) {
               var attr = node.attributes[att.name];
               if (!attr || (attr && !attr.nodeValue)) {
                  continue;
               }
            }
            text += " ";
            text += att.name;
            text += "=\"";
            text += this.encodeString(att.value,true);
            text += "\"";
         }
         if (node.firstChild) {
            text += ">";
            var child = node.firstChild;
            while (child) {
               text = this._serialize(text,child);
               child = child.nextSibling;
            }

            text += "</";
            if (!this.forceLowerCase && node.namespaceURI) {
               text += node.nodeName;
            } else {
               text += node.nodeName.toLowerCase();
            }
            text += ">";
         } else {
            text += "/>";
         }
         return text;
      case 2:
         return text;
         break;
      case 3:
         text += this.encodeString(node.nodeValue);
         return text;
      case 4:
         text += "<![CDATA["+this.encodeString(node.nodeValue)+"]]>";
         return text;
      case 5:
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      case 11:
      case 12:
         return text;
   }
   return text;
}

XHTMLSerializer.prototype.encodeString = function(value,attribute)
{
   value = value.replace(/&/g,"&amp;");
   if (attribute) {
      value = value.replace(/'/g, '&quot;');
      value = value.replace(/"/g, "&apos;");
      value = value.replace(/>/g,"&gt;");
   }
   value = value.replace(/</g,"&lt;");
   return value;
}

