function APPEditor(appBase,serviceURI,uploadBase) {
   this.base = appBase;
   this.service = new AtomService(serviceURI ? serviceURI : appBase);
   this.uploadBase = uploadBase ? uploadBase : appBase+"uploader/";
   this.pathPrefix = null;
}

APPEditor.prototype.showService = function(div) {
   DOMUtil.clearChildren(div);
   this._text(div,"Loading service, please wait...");
   var current = this;
   this.service.introspect({
      onSuccess: function() {
         for (var id in current.service.workspaces) {
            current.workspace = current.service.workspaces[id];
         }
         setTimeout(function() {
            current._initServiceEditor(div,"en");
         },10);
      },
      onFailure: function(status) {
         current._text(div,"Cannot load service, status="+status);
      }
   });
}

APPEditor.prototype._text = function(e,message) {
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

APPEditor.prototype._initServiceEditor = function(div,lang) {
   DOMUtil.clearChildren(div);
   var current = this;
   HTTP("GET",this.base+"service-editor-"+lang+".html", {
      overrideMimeType: "text/xml",
      onSuccess: function(status,xml) {
         var imported = DOMUtil.importNode(document,xml.documentElement,true);
         setTimeout(function() {
            div.appendChild(imported);
            DOMUtil.forChild(imported,null,"div",function(div) {
               var className = div.getAttribute("class");
               if (className=="app-service-editor-list") {
                  current.collectionList = div;
                  // attach to the controls
                  DOMUtil.forChild(current.collectionList,null,"p",function(p) {
                     DOMUtil.findDescendant(div,null,"a",function(a) {
                        var rel = a.getAttribute("rel");
                        if (rel=="add") {
                           a.onclick = function() {
                              current._text(current.status);
                              current.showCollectionAdd();
                              return false;
                           };
                        } else if (rel=="delete") {
                           a.onclick = function() {
                              current._text(current.status);
                              current.doDeleteCollections(current._getCheckedCollections(current.collectionListTable),
                                 function() {
                                 },
                                 function(status) {
                                    current._text(current.status,"Cannot delete collections, status="+status);
                                 }
                              );
                              return false;
                           };
                        }
                     });
                  });
                  // get the table
                  var table = DOMUtil.firstChild(current.collectionList,null,"table");
                  current.collectionListTable = DOMUtil.firstChild(table,null,"tbody");
                  current._loadCollectionTable();
               } else if (className=="app-service-editor-add") {
                  current.collectionAdd = div;
               } else if (className=="app-service-editor-status") {
                  current.status = div;
               } else if (className=="app-media-manager") {
                  current.mediaManagerDiv = div;
                  DOMUtil.findDescendant(DOMUtil.firstChild(current.mediaManagerDiv,null,"p"),null,"a",function(a) {
                     a.onclick = function() {
                        current.mediaManagerDiv.style.display = "none";
                        current.collectionList.style.display = "block";
                        return false;
                     };
                  });
               } else if (className=="app-entry-editor") {
                  current.entryEditor = new APPEntryEditor(current.base,div);
                  current.entryEditor.uploadBase = current.uploadBase;
               } else if (className=="app-feed-editor") {
                  current.feedEditor = new APPFeedEditor(div);
               } else if (className=="app-feed-entry-editor") {
                  current.feedEntryEditor = new APPFeedEntryEditor(div);
                  current.feedEntryEditor.uploadBase = current.uploadBase;
                  DOMUtil.findDescendant(current.feedEntryEditor.div,null,"a",function(a) {
                     if (a.getAttribute("rel")=="return") {
                        a.onclick = function() {
                           current.feedEntryEditor.div.style.display = "none";
                           current.collectionList.style.display = "block";
                           return false;
                        };
                     }
                  });
               }
            });
            if (current.mediaManager) {
               current.mediaManager = new APPMediaManager(current.base,current.mediaManagerDiv);
               current.mediaManager.status = current.status;
               current.mediaManager.entryEditor = current.entryEditor;
            }
            if (current.feedEntryEditor) {
               current.feedEntryEditor.entryEditor = current.entryEditor;
               current.feedEntryEditor.status = current.status;
            }
         },10);
      }
   });
}

APPEditor.prototype._loadCollectionTable = function() {
   var current = {
      target: this.workspace,
      index: 0,
      parent: null
   };

   while (current) {
      if (current.index<current.target.children.length) {
         var child = {
            target: current.target.children[current.index],
            index: 0,
            parent: current
         };
         this._addCollectionRow(child.target);
         current.index++;
         current = child;
      } else {
         current = current.parent;
      }
   }

}

APPEditor.prototype._addCollectionRow = function(collection) {
   var current = this;
   var href = collection.uri.relativeTo(collection.workspace.uri);
   if (this.pathPrefix) {
      href = href.substring(this.pathPrefix.length);
   }
   var row = DOMUtil.element(this.collectionListTable,{
      localName: "tr",
      namespace: "http://www.w3.org/1999/xhtml",
      className: "collection",
      children: [
         { localName: "td",
           children: [ 
              { localName: "input",
                attributes: {type: "checkbox", value: collection.href}}
           ]},
         { localName: "td",
           children: [ href]},
         { localName: "td",
           className: "operations",
           children: [ 
              { localName: "a",
                attributes: { href: "#", rel: "edit", title: "Edit feed details (e.g. title)"},
                children: ["[edit]"]
              },
              " ",
              { localName: "a",
                attributes: { href: "#", rel: "entries", title: "Edit collection entries."},
                children: ["[entries]"]
              }
           ]
         },
         { localName: "td",
           children: [ collection.title]}
      ]
   },true,true);
   collection.row = row;
   DOMUtil.findDescendant(row,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="edit") {
         var showEditor = function() {
            current._text(current.status);
            current.feedEditor.init(collection.feed);
            current.feedEditor.onCancel = function() {
               current.feedEditor.div.style.display = "none";
               current.collectionList.style.display = "block";
            };
            current.feedEditor.onSave = function() {
               current.feedEditor.updateFeed();
               collection.feed.updateByPut({
                  onSuccess: function() {
                     current._text(row.childNodes[3],DOMUtil.text(collection.feed.getTitle().xml))
                     current.feedEditor.div.style.display = "none";
                     current.collectionList.style.display = "block";
                  },
                  onFailure: function(status) {
                     current._text("status","Cannot save changes to feed, status="+status);
                  }
               });
            };
            current.collectionList.style.display = "none";
            current.feedEditor.div.style.display = "block";
         };
         a.onclick = function() {
            if (!collection.feed.loaded) {
               current.collectionList.style.display = "none";
               current._text(current.status,"Loading feed, please wait...");
               collection.feed.load({
                  onSuccess: function() {
                     showEditor();
                  },
                  onFailure: function(status) {
                     current._text("status","Cannot load feed "+collection.feed.uri.spec+", status="+status);
                  }
               });
            } else {
               showEditor();
            }
            return false;
         };
      } else if (rel=="entries") {
         var showEditor = function() {
            current._text(current.status);
            current.feedEntryEditor.init(collection.feed);
            current.collectionList.style.display = "none";
            current.feedEntryEditor.div.style.display = "block";
         };
         a.onclick = function() {
            if (!collection.feed.loaded) {
               current.collectionList.style.display = "none";
               current._text(current.status,"Loading feed, please wait...");
               collection.feed.load({
                  onSuccess: function() {
                     showEditor();
                  },
                  onFailure: function(status) {
                     current._text("status","Cannot load feed "+collection.feed.uri.spec+", status="+status);
                  }
               });
            } else {
               showEditor();
            }
            return false;
         };
      } else if (rel=="media") {
         var showManager = function() {
            current._text(current.status);
            current.mediaManager.init(collection);
            current.collectionList.style.display = "none";
            current.mediaManagerDiv.style.display = "block";
         };
         a.onclick = function() {
            if (!collection.feed.loaded) {
               current.collectionList.style.display = "none";
               current._text(current.status,"Loading feed, please wait...");
               collection.feed.load({
                  onSuccess: function() {
                     showManager();
                  },
                  onFailure: function(status) {
                     current._text("status","Cannot load feed "+collection.feed.uri.spec+", status="+status);
                  }
               });
            } else {
               showManager();
            }
            return false;
         };
      }
   });
}

APPEditor.prototype.showCollectionAdd = function() {
   var current = this;
   this.collectionList.style.display = "none";
   this.collectionAdd.style.display = "block";
   var path = null;
   var title = null;
   DOMUtil.findDescendant(this.collectionAdd,null,"input",function(i) {
      if (i.name=="add-path") {
         path = i;
      } else if (i.name=="add-title") {
         title = i;
      }
   });
   DOMUtil.findDescendant(this.collectionAdd,null,"button",function(button) {
      if (button.value=="ok") {
         button.onclick = function() {
            //var href = (current.pathPrefix ? current.pathPrefix : "") + (path.value.length>0 && path.value.charAt(0)=='/' ? path.value : '/'+path.value);
            var href = (current.pathPrefix ? current.pathPrefix : "") + path.value;
            if (href.charAt(href.length-1)!="/") { href += "/"; }
            var collection = current.workspace.newCollection(href,title.value);
            //alert("'" + current.workspace.uri.spec + "' '" +href+"' "+collection.uri.spec);
            current.workspace.createByPost(collection,{
               onSuccess: function() {
                  current.collectionAdd.style.display = "none";
                  current.collectionList.style.display = "block";
                  var ancestors = current.workspace.checkCollectionHierarchy(collection);
                  current.workspace.treeSort();
                  for (var i=0; i<ancestors.length; i++) {
                     current._addCollectionRow(ancestors[i]);
                  }
                  current._addCollectionRow(collection);
               },
               onFailure: function(status) {
                  current._text(current.status,"Cannot create collection, status="+status);
                  if (current.workspace.collections[collection.href]) {
                     delete current.workspace.collections[collection.href];
                  }
               }
            });
         };
      } else if (button.value=="cancel") {
         button.onclick = function() {
            current.collectionAdd.style.display = "none";
            current.collectionList.style.display = "block";
            current._text(current.status);
         };
      }
   });
}

APPEditor.prototype._getCheckedCollections = function(body) {
   var current = this;
   var list = [];
   DOMUtil.findDescendant(body,null,"input",function(e) {
      if (e.checked) {
         var row = e.parentNode.parentNode;
         list.push({
           collection: current.workspace.collections[e.value],
           row: row
         });
      }
   });
   return list;
}

APPEditor.prototype.doDeleteCollections = function(checked,success,failure) {
   var current = this;
   var collectionURI = function(uri) {
      if (uri.charAt(uri.length-1)=="/") {
         uri = uri.substring(0,uri.length-1);
      }
      return uri+".col";
   }
   var operation = function(index) {
      if (index<checked.length) {
         current.workspace.removeByDelete(checked[index].collection,{
            uri: collectionURI(checked[index].collection.uri.spec),
            onFailure: function(status) {
               failure(status);
            },
            onDelete: function(collection) {
               DOMUtil.remove(collection.row);
            },
            onSuccess: function(status,xml,text) {
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
