function APPMediaManager(base,div) {
   this.base = base;
   this.upload = base+"upload/";
   this.div = div;
   this.status = null;
   this.allowEdit = true;
   this.entryEditor = null;
   var current = this;
   this.doCheckMediaEntry = function(entry) {
      var content = entry.getContent();
      return content && content.getMediaReference();
   };
   this.doAdd = function(entry) {
      current.addEntry(entry);
   };
   this.doCreateRow = function(entry) {
      return current.createEntryRow(entry);
   };
   this.doAppendRow = function(entry,row) {
      current.body.appendChild(row);
   };
   this.doLoadEntries = function() {
      current.loadEntries();
   };
   DOMUtil.forChild(this.div,null,"div",function(div) {
      if (div.getAttribute("class")=="list") {
         current.list = div;
      }
   });
   this.header = DOMUtil.firstChild(this.div,null,"h2");
   DOMUtil.findDescendant(current.list,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="upload") {
         a.onclick = function() {
            //current.newUpload.src = current.collection.href.charAt(0)=='/' ? current.upload+current.collection.href.substring(1) : current.upload+current.collection.href;
            current.newUpload.src = current.upload+current.collection.uri.relativeTo(current.collection.workspace.service.uri);
            current.list.style.display = "none";
            current.newUpload.style.display = "block";
            return false;
         };
      } else if (rel=="delete") {
         a.onclick = function() {
            current.doDeleteFiles(current._getChecked(current.body),
               function() {
               },
               function(status) {
                  current._showStatus("Cannot delete media resources, status="+status);
               }
            );
            return false;
         };
      }
   });
   DOMUtil.forChild(current.list,null,"table",function(table) {
      if (table.getAttribute("class")=="files") {
         current.table = table;
         current.body = DOMUtil.firstChild(table,null,"tbody");
         if (!current.body) {
            current.body = table;
         }
      }
   });
   this.newUpload = DOMUtil.element(this.div,{
      localName: "iframe",
      namespace: "http://www.w3.org/1999/xhtml",
      attributes: {width: '100%', height:"25px", frameborder: '0'}
   },true,true);
   this.newUpload.style.display = "none";
   this.newUpload.onload = function() {
      var cancel = current.newUpload.contentDocument.getElementById("cancel");
      if (cancel) {
         cancel.onclick = function() {
            current.newUpload.style.display = "none";
            current.list.style.display = "block";
         };
      }
      var uploadForm = current.newUpload.contentDocument.getElementById("form");
      if (uploadForm) {
         uploadForm.onsubmit = function() {
            if (uploadForm.elements[0].value=="") {
               return false;
            } else {
               return true;
            }
         };
      } else if (current.newUpload.contentDocument.getElementById("ok")){
         var file = DOMUtil.text(current.newUpload.contentDocument.getElementById("ok"));
         var id = DOMUtil.text(current.newUpload.contentDocument.getElementById("id"));
         current.newUpload.style.display = "none";
         current.list.style.display = "block";
         id = id.substring(9);
         HTTP("GET",current.collection.feed.uri.spec+"_/"+id,
         {
            onSuccess: function(status,xml) {
               var entry = current.collection.feed.addEntryFromXML(xml.documentElement);
               entry.local = false;
               current.doAdd(entry);
            },
            onFailure: function(status) {
               if (current.status) {
                  current._text("status","Cannot get entry "+id+" for media resource, status="+status);
               } else {
                  throw "Cannot get entry "+id+" for media resource, status="+status;
               }
            }
         });
      } else if (current.newUpload.contentDocument.getElementById("error")) {
         var msg = DOMUtil.text(current.newUpload.contentDocument.getElementById("error"));
         var status = DOMUtil.text(current.newUpload.contentDocument.getElementById("status"))
         if (status=="400" || status=="405") {
            current._showStatus("Cannot upload product icon because media with the same name already exists.");
         } else {
            current._showStatus(msg);
         }
         current.newUpload.style.display = "none";
         current.list.style.display = "block";
      }
   };
}

APPMediaManager.prototype._text = function(e,message) {
   if (typeof(e)=='string') {
      e = document.getElementById(e);
   }
   if (e) {
      DOMUtil.clearChildren(e);
      if (message) {
         e.appendChild(e.ownerDocument.createTextNode(message));
      }
   }
}

APPMediaManager.prototype._showStatus = function(message) {
   if (this.status) {
      this._text(this.status,message);
   } else {
      throw message;
   }
}

APPMediaManager.prototype.init = function(collection) {
   this.collection = collection;
   DOMUtil.clearChildren(this.body);
   if (this.header) {
      this._text(this.header,DOMUtil.text(collection.feed.getTitle().xml));
   }
   this.doLoadEntries();
}

APPMediaManager.prototype.loadEntries = function() {
   for (var id in this.collection.feed.entries) {
      var entry = this.collection.feed.entries[id];
      if (this.doCheckMediaEntry(entry)) {
         this.doAdd(entry);
      }
   }
}

APPMediaManager.prototype.addEntry = function(entry) {
   var row = this.doCreateRow(entry);
   this.doAppendRow(entry,row);
}
   
APPMediaManager.prototype.createEntryRow = function(entry) {
   var content = entry.getContent();
   var current = this;
   var row = DOMUtil.element(this.body,{
      localName: "tr",
      namespace: "http://www.w3.org/1999/xhtml",
      attributes: {id: entry.id},
      children: [
         { localName: "td",
           children: [ 
              { localName: "input",
                attributes: {type: "checkbox", value: entry.id}}
           ]},
         { localName: "td",
           children: [ {
              localName: "a",
              attributes: { href: content.getLocation().spec, title: "Download"},
              children: [content.getMediaReference()]
           }]},
         { localName: "td",
           children: [ 
              { localName: "a",
                attributes: { href: "#", rel: "update", title: "Upload a new version."},
                children: ["[update]"]
              }
              ,
              " ",
              current.allowEdit ?
              { localName: "a",
                attributes: { href: "#", rel: "edit", title: "Edit the entry."},
                children: ["[edit]"]
              } : ""
           ]
         },
         { localName: "td",
           children: [ DOMUtil.text(entry.getTitle().xml)]}
      ]
   },false,true);
   DOMUtil.findDescendant(row,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="update") {
         a.onclick = function() {
            current.showFileUpdate(a,row,entry);
            a.style.display = "none";
            return false;
         };
      } else if (rel=="edit") {
         a.onclick = function() {
            current.showEntryEditor(row,entry);
            return false;
         };
      }
   });
   return row;
}

APPMediaManager.prototype.showFileUpdate = function(a,row,entry) {
   var fileCell = row.childNodes[1];
   var br = DOMUtil.element(fileCell,{
      localName: "br",
      namespace: "http://www.w3.org/1999/xhtml"
   },true,true);
   var uploadFrame = DOMUtil.element(fileCell,{
      localName: "iframe",
      namespace: "http://www.w3.org/1999/xhtml",
      attributes: {width: '450px', height:"50px", frameborder: '0'}
   },true,true);
   var current = this;
   uploadFrame.onload = function() {
      var cancel = uploadFrame.contentDocument.getElementById("cancel");
      if (cancel) {
         cancel.onclick = function() {
            a.style.display = "inline";
            DOMUtil.remove(uploadFrame);
            DOMUtil.remove(br);
         };
      }
      var uploadForm = uploadFrame.contentDocument.getElementById("form");
      if (uploadForm) {
         uploadForm.onsubmit = function() {
            if (uploadForm.elements[0].value=="") {
               return false;
            } else {
               return true;
            }
         };
      } else if (uploadFrame.contentDocument.getElementById("ok")){
         var file = DOMUtil.text(uploadFrame.contentDocument.getElementById("ok"));
         var id = DOMUtil.text(uploadFrame.contentDocument.getElementById("id"));
         a.style.display = "inline";
         DOMUtil.remove(uploadFrame);
         DOMUtil.remove(br);
      } else if (uploadFrame.contentDocument.getElementById("error")) {
         var msg = DOMUtil.text(uploadFrame.contentDocument.getElementById("error"));
         var status = DOMUtil.text(uploadFrame.contentDocument.getElementById("status"))
         if (status=="400" || status=="405") {
            current._showStatus("Cannot upload product icon because media with the same name already exists.");
         } else {
            current._showStatus(msg);
         }
         a.style.display = "inline";
         DOMUtil.remove(uploadFrame);
         DOMUtil.remove(br);
      }
   };
   /*
   var href = this.collection.href.charAt(0)=='/' ? this.upload+this.collection.href.substring(1) : this.upload+this.collection.href;
   href += entry.getContent().getMediaReference();
   uploadFrame.src = href;
  */
   //alert("Feed: "+entry.feed.uri.spec+"\nMedia: "+entry.getContent().getLocation().spec+"\nService: "+this.collection.workspace.service.uri.spec+"\nRelative: "+entry.getContent().getLocation().relativeTo(this.collection.workspace.service.uri));
   uploadFrame.src = this.upload+entry.getContent().getLocation().relativeTo(this.collection.workspace.service.uri);
}

APPMediaManager.prototype._getChecked = function(body) {
   var current = this;
   var list = [];
   DOMUtil.findDescendant(body,null,"input",function(e) {
      if (e.checked) {
         var row = e.parentNode.parentNode;
         list.push({
           entry: current.collection.feed.entries[e.value],
           row: row
         });
      }
   });
   return list;
}

APPMediaManager.prototype.doDeleteFiles = function(checked,success,failure) {
   var current = this;
   var operation = function(index) {
      if (index<checked.length) {
         current.collection.feed.removeEntry(checked[index].entry,{
            onFailure: function(status) {
               failure(status);
            },
            onSuccess: function(status,xml,text) {
               DOMUtil.remove(checked[index].row);
               setTimeout(function() {
                  operation(index+1);
               },10);
            }
         });
      } else {
         success();
      }
   };
   operation(0);
}

APPMediaManager.prototype.showEntryEditor = function(row,entry) {
   if (!this.entryEditor) {
      return;
   }
   var current = this;
   this.entryEditor.allowContentEdit = false;
   this.entryEditor.init(entry);
   this.entryEditor.onCancel = function() {
      current.div.style.display = "block";
      current.entryEditor.div.style.display = "none";
   };
   this.entryEditor.onSave = function() {
      current.entryEditor.updateEntry();
      entry.save({
         onSuccess: function() {
            current._text(row.childNodes[3],DOMUtil.text(entry.getTitle().xml))
            current.div.style.display = "block";
            current.entryEditor.div.style.display = "none";
         },
         onFailure: function(status) {
            current._showStatus("Cannot save changes to entry, status="+status);
         }
      });
   };
   this._text(this.entryEditor.saveButton,"Update");
   this.div.style.display = "none";
   this.entryEditor.div.style.display = "block";
}
