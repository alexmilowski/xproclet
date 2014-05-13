
String.prototype.trim =
function() {
  return this.replace( /^[ \n\r\t]+|[ \n\r\t]+$/g, "" );
}

function APPFeedEntryEditor(div) {
   this.feed = null;
   this.div = div;
   this.saveButton = null;
   this.cancelButton = null;
   this.entryEditor = null;
   var current = this;
   DOMUtil.forChild(this.div,null,"div",function(div) {
      if (div.getAttribute("class")=="list") {
         current.list = div;
      }
   });
   this.header = DOMUtil.firstChild(this.div,null,"h2");
   DOMUtil.findDescendant(current.list,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="add") {
         a.onclick = function() {
            current.showEntryEditor();
            return false;
         };
      } else if (rel=="delete") {
         a.onclick = function() {
            current.doDeleteEntries(current._getChecked(current.body),
               function() {
               },
               function(status) {
                  current._showStatus("Cannot delete entries, status="+status);
               }
            );
            return false;
         };
      } else if (rel=="upload") {
         a.onclick = function() {
            current.showUpload();
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
   current.table.style.display = "none";
}

APPFeedEntryEditor.prototype._text = function(e,message) {
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

APPFeedEntryEditor.prototype.init = function(feed) {
   this.table.style.display = "none";
   this.feed = feed;
   DOMUtil.clearChildren(this.body);
   if (this.header) {
      var title = DOMUtil.text(this.feed.getTitle().xml);
      if (title=="") {
         title = "(no title)"
      }
      this._text(this.header,title);
   }
   for (var id in this.feed.entries) {
      var entry = this.feed.entries[id];
      this._addRow(entry);
   }
}

APPFeedEntryEditor.prototype._addRow = function(entry,atStart) {
   this.table.style.display = "block";
   var current = this;
   var edited = entry.getEdited();
   if (!edited) {
      edited = entry.getUpdated();
   }
   
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
           children: [ DOMUtil.text(entry.getTitle().xml)]},
         { localName: "td",
           children: [ 
              { localName: "a",
                attributes: { href: "#", rel: "edit", title: "Edit the entry."},
                children: ["[edit]"]
              } 
           ]
         },
         { localName: "td",
           children: [ current._formatDate(edited) ]}
      ]
   },false,true);
   if (atStart && this.body.firstChild) {
      this.body.insertBefore(row,this.body.firstChild);
   } else {
      this.body.appendChild(row);
   }
   DOMUtil.findDescendant(row,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="edit") {
         a.onclick = function() {
            current.showEntryEditor(row,entry);
            return false;
         };
      }
   });
}

APPFeedEntryEditor.prototype._showStatus = function(message) {
   if (this.status) {
      this._text(this.status,message);
   } else {
      throw message;
   }
}

APPFeedEntryEditor.prototype._formatDate = function(date)
{
   return date.substring(0,11)+" "+date.substring(12);
}

APPFeedEntryEditor.prototype.showEntryEditor = function(row,entry)
{
   if (!this.entryEditor) {
      return;
   }
   var current = this;
   this.entryEditor.allowContentEdit = true;
   this.entryEditor.init(entry);
   this.entryEditor.onCancel = function() {
      current.div.style.display = "block";
      current.entryEditor.div.style.display = "none";
   };
   this.entryEditor.onSave = function() {
      var newEntry = false;
      if (!entry) {
         newEntry = true;
         entry = current.feed.createEntry("");
         current.entryEditor.setEntry(entry);
      }
      current.entryEditor.updateEntry();
      entry.save({
         onSuccess: function() {
            if (newEntry) {
               current._addRow(entry,true);
            } else {
               current._text(row.childNodes[1],DOMUtil.text(entry.getTitle().xml))
            }
            current.div.style.display = "block";
            current.entryEditor.div.style.display = "none";
         },
         onFailure: function(status,doc,text) {
            current._showStatus("Cannot save changes to entry, status="+status+", "+text);
         }
      });
   };
   this._text(this.entryEditor.saveButton,entry ? "Update" : "Add");
   this.div.style.display = "none";
   this.entryEditor.div.style.display = "block";
}

APPFeedEntryEditor.prototype._getChecked = function(body) {
   var current = this;
   var list = [];
   DOMUtil.findDescendant(body,null,"input",function(e) {
      if (e.checked) {
         var row = e.parentNode.parentNode;
         list.push({
           entry: current.feed.entries[e.value],
           row: row
         });
      }
   });
   return list;
}

APPFeedEntryEditor.prototype.doDeleteEntries = function(checked,success,failure) {
   var current = this;
   var operation = function(index) {
      if (index<checked.length) {
         current.feed.removeEntry(checked[index].entry,{
            onFailure: function(status) {
               failure(status);
            },
            onSuccess: function(status,xml,text) {
               DOMUtil.remove(checked[index].row);
               if (!current.body.firstChild) {
                  current.table.style.display = "none";
               }
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

APPFeedEntryEditor.prototype.showUpload = function() {
   this.list.style.display = "none";
   var current = this;
   var uploadDiv = DOMUtil.element(this.div,
   { localName: "div", className: "upload",
      children: [ {localName: "h2", children: [ "Upload File"]}]
   }, true);
   var upload = new Upload();
   upload.onCancel = function() {
      DOMUtil.remove(uploadDiv);
      current.list.style.display = "block";
   };
   upload.onReady = function(status) {
      if (status>=200 && status<300) {
         upload.showForm(uploadDiv,uploadDiv.offsetWidth,uploadDiv.offsetHeight);
      } else {
         upload.onCancel();
      }
   }
   upload.onUpload = function(name) {
      upload.name = name;
      upload.iframe.style.display = "none";
      upload.statusLine = DOMUtil.element(uploadDiv, {
         localName: "p",
         className: "upload-status",
         children: [ "Uploading "+name+" ..." ]
      },true);
      setTimeout(function() {
         upload.requestStatus();
      },500);
   }
   var k = 1024;
   var mb = 1024*k;
   upload.onStatus = function(data) {
      DOMUtil.clearChildren(upload.statusLine);
      if (data.status>=200 && data.status<300) {
         var uploaded = (data.size-data.progress)<100;
         if (uploaded) {
            DOMUtil.text(upload.statusLine,"Uploaded complete.  Processing...");
            if (data.status==201) {
               var entryLocation = current.feed.uri.resolve("_/"+data.entryId.substring(9)+".atom");
               // created entry
               setTimeout(function() {
               HTTP("GET",entryLocation.spec,
               {
                  onSuccess: function(status,xml) {
                     var entry = current.feed.addEntryFromXML(xml.documentElement);
                     entry.local = false;
                     upload.onCancel();
                     current._addRow(entry,true);
                  },
                  onFailure: function(status) {
                     DOMUtil.text(upload.statusLine,"Upload succeeded but cannot load entry, status="+status);
                  }
               });
               },10);
            } else {
               // This shouldn't happen from here since we don't update from this UI
               upload.onCancel();
            }
         }
      } else if (data.cancelled) {

      } else if (data.status==400) {
         DOMUtil.text(upload.statusLine,"The file name you have choosen already exists. ");
      } else if (data.status) {
         DOMUtil.text(upload.statusLine,"Upload failed, status="+data.status);
      } else {
         var percent = (data.progress*1.0)/(data.size*1.0)*100.0;
         var amount = null;
         if (data.size<mb) {
            var p = Math.round(10*data.progress/k)/10;
            var s = Math.round(10*data.size/k)/10;
            amount = p+"K of "+s+"K";
         } else {
            var p = Math.round(10*data.progress/mb)/10;
            var s = Math.round(10*data.size/mb)/10;
            amount = p+"MB of "+s+"MB";
         }
         DOMUtil.text(upload.statusLine,"Progress on "+upload.name+" : "+amount+" "+Math.round(percent)+"%");
         setTimeout(function() {
            upload.requestStatus();
         },500);
      }
   }


   upload.start(this.uploadBase,this.feed.collection.href);
}