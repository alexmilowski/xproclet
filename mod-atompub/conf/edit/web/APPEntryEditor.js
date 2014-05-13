
String.prototype.trim =
function() {
  return this.replace( /^[ \n\r\t]+|[ \n\r\t]+$/g, "" );
}

function APPEntryEditor(base,div) {
   this.base = base ? base : "";
   this.entry = null;
   this.div = div;
   this.saveButton = null;
   this.cancelButton = null;
   this.allowContentEdit = true;
   this.linksShown = false;
   this.contentShown = false;
   this.categoriesShown = false;
   this.contentType = null;
   var current = this;
   var entryEditor = this;
   this.onCancel = function() {
   };
   this.onSave = function() {
   };
   this.textEditor = {
      input: null,
      attach: function(parent,type,xml) {
         DOMUtil.element(parent, {
            localName: "p",
            namespace: "http://www.w3.org/1999/xhtml",
            children: ["Type: "+type]
         },true,true);
         this.input = DOMUtil.element(parent, {
            localName: "textarea",
            namespace: "http://www.w3.org/1999/xhtml",
            attributes: {rows: "25", cols: "80"},
            children: [ xml ? DOMUtil.text(xml) : "" ]
         },true,true);
      },
      getContent: function() {
         return this.input.value;
      }
   };
   this.xhtmlEditor = {
      attach: function(parent,type,xml) {

         var current = this;
         var firstElement = xml ? xml.firstChild : null;
         while (firstElement && firstElement.nodeType!=1) {
            firstElement = firstElement.nextSibling;
         }
         
         var tabs = DOMUtil.element(parent,{
            localName: "ul",
            className: "tabs",
            children: [
               {localName: "li", className: "selected", children: [ {localName: "a", attributes: {href:"#", rel: "formatted"}, children: ["Formatted"]}]},
               {localName: "li", children: [ {localName: "a", attributes: {href:"#", rel: "source"}, children: ["Source"]}]}
            ]
         },true,true);
         var editorBase = entryEditor.base ? entryEditor.base : "";
         var content = DOMUtil.element(parent,{
            localName: "div",
            className: "tabs-content",
            children: [
               {localName: "div", className: "xhtml-editor", children: [
                  {localName: "div", className: "controls", children: [
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "unwrap", title: "Unwrap contents"}, children: [ {localName: "img", attributes: {src: editorBase+"unwrap-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "div", title: "Insert division"}, children: [ {localName: "img", attributes: {src: editorBase+"div-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "p", title: "Insert paragraph"}, children: [ {localName: "img", attributes: {src: editorBase+"p-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "h1", title: "Insert heading, level 1"}, children: [ {localName: "img", attributes: {src: editorBase+"h1-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "h2", title: "Insert heading, level 2"}, children: [ {localName: "img", attributes: {src: editorBase+"h2-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "h3", title: "Insert heading, level 3"}, children: [ {localName: "img", attributes: {src: editorBase+"h3-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "h4", title: "Insert heading, level 4"}, children: [ {localName: "img", attributes: {src: editorBase+"h4-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "h5", title: "Insert heading, level 5"}, children: [ {localName: "img", attributes: {src: editorBase+"h5-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "ul", title: "Insert bulleted list"}, children: [ {localName: "img", attributes: {src: editorBase+"ul-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "ol", title: "Insert numbered list"}, children: [ {localName: "img", attributes: {src: editorBase+"ol-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "img", title: "Insert image"}, children: [ {localName: "img", attributes: {src: editorBase+"img-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "a", title: "Insert link"}, children: [ {localName: "img", attributes: {src: editorBase+"a-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "i", title: "Make italics"}, children: [ {localName: "img", attributes: {src: editorBase+"i-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "b", title: "Make bold"}, children: [ {localName: "img", attributes: {src: editorBase+"b-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "em", title: "Make emphasized"}, children: [ {localName: "img", attributes: {src: editorBase+"em-icon.png"}}]},
                     {localName: "a", className: "image-button", attributes: {href: "#", rel: "code", title: "Make into code"}, children: [ {localName: "img", attributes: {src: editorBase+"code-icon.png"}}]}
                  ]},
                  {localName: "div", className: "content", attributes: {contenteditable: "true"}}
               ]},
               {localName: "div", className: "source-editor", children: [
                  {
                     localName: "textarea",
                     namespace: "http://www.w3.org/1999/xhtml",
                     attributes: {rows: "30", cols: "100"}
                  }
               ]}
            ]
         },true,true);
         this.editorContent = content.firstChild.childNodes[1];
         DOMUtil.findDescendant(content, null, "textarea", function(t) {
            current.text = t;
         });
         DOMUtil.findDescendant(content, null, "a", function(a) {
            var rel = a.getAttribute("rel");
            if (rel=="unwrap") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection && selection.anchorNode) {
                     var container = selection.anchorNode.parentNode;
                     if (container!=current.editorContent) {
                        var range = container.ownerDocument.createRange();
                        range.selectNodeContents(container);
                        var fragment = range.extractContents();
                        var contextNode = container.nextSibling;
                        var parent = container.parentNode;
                        DOMUtil.remove(container);
                        if (contextNode) {
                           parent.insertBefore(fragment,contextNode);
                        } else {
                           parent.appendChild(fragment);
                        }
                     }
                  }
                  return false;
               }
            } else if (rel=="div" || rel=="p" || rel=="h1" || rel=="h2" || rel=="h3" || rel=="h4" || rel=="h5") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection && selection.anchorNode) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement(this.getAttribute("rel")));
                     } else {
                        var div = DOMUtil.element(content,{localName: this.getAttribute("rel"), children: [ "..."]});
                        var node = selection.anchorNode;
                        if (node.nodeType!=1) {
                           node = node.parentNode;
                        }
                        if (node==current.editorContent) {
                           node.appendChild(div)
                        } else {
                           if (node.nextSibling) {
                              node.parentNode.insertBefore(div,node.nextSibling);
                           } else {
                              node.parentNode.appendChild(div);
                           }
                        }
                        selection.selectAllChildren(div);
                     }
                  }
                  return false;
               }
            } else if (rel=="p") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection && selection.anchorNode) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement("p"));
                     } else {
                        var p = DOMUtil.element(content,{localName: "p", children: [ "..."]});
                        var node = selection.anchorNode;
                        if (node.nodeType!=1) {
                           node = node.parentNode;
                        }
                        if (node==current.editorContent) {
                           node.appendChild(p)
                        } else {
                           if (node.nextSibling) {
                              node.parentNode.insertBefore(p,node.nextSibling);
                           } else {
                              node.parentNode.appendChild(p);
                           }
                        }
                        selection.selectAllChildren(p);
                     }
                  }
                  return false;
               }
            } else if (rel=="ul") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection && selection.anchorNode) {
                     var list = DOMUtil.element(content,{localName: "ul", children: [ {localName: "li", children:[" "]}]});
                     var node = selection.anchorNode;
                     if (node.nodeType!=1) {
                        node = node.parentNode;
                     }
                     if (node==current.editorContent) {
                        node.appendChild(list)
                     } else {
                        if (node.nextSibling) {
                           node.parentNode.insertBefore(list,node.nextSibling);
                        } else {
                           node.parentNode.appendChild(list);
                        }
                     }
                     selection.selectAllChildren(list.firstChild);
                  }
                  return false;
               }
            } else if (rel=="ol") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection && selection.anchorNode) {
                     var list = DOMUtil.element(content,{localName: "ol", children: [ {localName: "li", children:[" "]}]});
                     var node = selection.anchorNode;
                     if (node.nodeType!=1) {
                        node = node.parentNode;
                     }
                     if (node==current.editorContent) {
                        node.appendChild(list)
                     } else {
                        if (node.nextSibling) {
                           node.parentNode.insertBefore(list,node.nextSibling);
                        } else {
                           node.parentNode.appendChild(list);
                        }
                     }
                     selection.selectAllChildren(list.firstChild);
                  }
                  return false;
               }
            } else if (rel=="img") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (same) {
                        var src = prompt("Image URL?");
                        if (src) {
                           var img = DOMUtil.element(content,{localName: "img", attributes: {src: src, alt: "image"}});
                           var range = selection.getRangeAt(0);
                           range.insertNode(img);
                        }
                     }
                  }
                  return false;
               }
            } else if (rel=="a") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var href = prompt("Link URL?");
                        if (href) {
                           var link = DOMUtil.element(content,{localName: "a", attributes: {href: href, title: "link"}});
                           var range = selection.getRangeAt(0);
                           range.surroundContents(link);
                        }
                     }
                  }
                  return false;
               }
            } else if (rel=="i") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement("i"));
                     }
                  }
                  return false;
               }
            } else if (rel=="b") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement("b"));
                     }
                  }
                  return false;
               }
            } else if (rel=="em") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement("em"));
                     }
                  }
                  return false;
               }
            } else if (rel=="code") {
               a.onclick = function() {
                  var selection = content.ownerDocument.defaultView.getSelection();
                  if (selection) {
                     var same = selection.anchorNode==selection.focusNode && selection.anchorOffset==selection.focusOffset;
                     if (!same) {
                        var range = selection.getRangeAt(0);
                        range.surroundContents(content.ownerDocument.createElement("code"));
                     }
                  }
                  return false;
               }
            } else {
               a.onclick = function() {
                  return false;
               }
            }
         });
         content.childNodes[1].style.display = "none";
         var makeTab = function(tabAnchor) {
            return function() {
               if (tabAnchor.parentNode.className=="selected") {
                  return false;
               }
               for (var i=0; i<tabs.childNodes.length; i++) {
                  if (tabs.childNodes[i].firstChild==tabAnchor) {
                     tabs.childNodes[i].className = "selected";
                     content.childNodes[i].style.display = "block";
                  } else {
                     tabs.childNodes[i].className = "";
                     content.childNodes[i].style.display = "none";
                  }
               }
               if (tabAnchor.getAttribute("rel")=="formatted") {
                  try {
                     var doc = current.getTextContentAsDocument();
                     current.setEditorContent(doc.documentElement);
                  } catch (ex) {
                     for (var i=0; i<tabs.childNodes.length; i++) {
                        if (tabs.childNodes[i].firstChild==current.sourceTab) {
                           tabs.childNodes[i].className = "selected";
                           content.childNodes[i].style.display = "block";
                        } else {
                           tabs.childNodes[i].className = "";
                           content.childNodes[i].style.display = "none";
                        }
                     }
                     alert("Cannot parse content: "+ex);
                     return false;
                  }
                  current.formatted = true;
               } else if (tabAnchor.getAttribute("rel")=="source") {
                  current.formatted = false;
                  current.text.value = current.getEditorContent();
               }
               return false;
            }
         }
         DOMUtil.findDescendant(tabs, null, "a", function(a){
            a.onclick = makeTab(a);
            if (a.getAttribute("rel")=="source") {
               current.sourceTab = a;
            }
         });
         if (firstElement) {
            current.setEditorContent(firstElement);
         } else {
            DOMUtil.element(this.editorContent,{localName: "p", children: [ "Enter your content here..."]},true,true);
         }
         current.formatted = true;
      },
      getTextContentAsDocument: function() {
         var parser = new DOMParser();
         var doc = parser.parseFromString(this.text.value,"text/xml");
         var parseerrors = doc.getElementsByTagName("parsererror");
         if (parseerrors.length > 0) {
            var div = null;
            DOMUtil.findDescendant(parseerrors[0],null,"div",function(e) {div = e;});
            throw DOMUtil.text(div ? div : parseerrors[0]);
         } else {
            return doc;
         }

      },
      setEditorContent: function(divWrapper) {
         DOMUtil.clearChildren(this.editorContent);
         for (var child=divWrapper.firstChild; child; child=child.nextSibling) {
            this.editorContent.appendChild(DOMUtil.importNode(this.editorContent.ownerDocument, child, true));
         }
      },
      getEditorContent: function() {
         var serializer = new XHTMLSerializer();
         serializer.forceLowerCase = true;
         return serializer.serializeToString(this.getEditorContentAsDocument().documentElement);
         /*
         var xhtml = "<div xmlns='http://www.w3.org/1999/xhtml'>";
         for (var child=this.editorContent.firstChild; child; child=child.nextSibling) {
            xhtml += serializer.serializeToString(child);
         }
         xhtml += "</div>";
         return xhtml;
         */
      },
      getEditorContentAsDocument: function() {
         var parser = new DOMParser();
         var doc = parser.parseFromString("<div xmlns='http://www.w3.org/1999/xhtml'/>","text/xml");
         for (var child=this.editorContent.firstChild; child; child=child.nextSibling) {
            doc.documentElement.appendChild(DOMUtil.importNode(doc, child, true));
         }
         return doc;
      },
      getContent: function() {
         if (this.formatted) {
            return this.getEditorContentAsDocument().documentElement;
         } else {
            return this.getTextContentAsDocument().documentElement;
         }
      }
   };
   this.xmlEditor = {
      attach: function(parent,type,xml) {
         
         var serialized = "<doc>\n\n</doc>";
         if (xml) {
            var current = xml.firstChild;
            while (current && current.nodeType!=1) {
               current = current.nextSibling;
            }
            if (current) {
               var serializer = new XMLSerializer();
               serialized = serializer.serializeToString(current);
            }
         }
         DOMUtil.element(parent, {
            localName: "p",
            namespace: "http://www.w3.org/1999/xhtml",
            children: ["Type: "+type]
         },true,true);
         this.input = DOMUtil.element(parent, {
            localName: "textarea",
            namespace: "http://www.w3.org/1999/xhtml",
            attributes: {rows: "25", cols: "80"},
            children: [ serialized ]
         },true,true);
      },
      getContent: function() {
         var parser = new DOMParser();
         var doc = parser.parseFromString(this.input.value,"text/xml");
         return doc.documentElement;
      }
   };
   DOMUtil.findDescendant(div,null,"button",function(button) {
      if (button.value=="ok") {
         current.saveButton = button;
         button.onclick = function() {
            current.onSave();
         };
      } else if (button.value=="cancel") {
         current.cancelButton = button;
         button.onclick = function() {
            current.onCancel();
         };
      }
   });
   DOMUtil.findDescendant(div,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="links") {
         current.linksLink = a;
         a.onclick = function() {
            current.toggleLinks(!current.linksShown);
            return false;
         };
      } else if (rel=="categories") {
         current.categoriesLink = a;
         a.onclick = function() {
            current.toggleCategories(!current.categoriesShown);
            return false;
         };
      } else if (rel=="content") {
         current.contentLink = a;
         a.onclick = function() {
            current.toggleContent(!current.contentShown);
            return false;
         };
      }
   });
   this.title = null;
   this.summary = null;
   DOMUtil.findDescendant(div,null,"input",function(i) {
      if (i.name=="title") {
         current.title = i;
      } else if (i.name=="summary") {
         current.summary = i;
      }
   });
   DOMUtil.findDescendant(div,null,"textarea",function(i) {
      if (i.name=="title") {
         current.title = i;
      } else if (i.name=="summary") {
         current.summary = i;
      }
   });
   DOMUtil.findDescendant(div,null,"div",function(div) {
      var className = div.getAttribute("class");
      if (className=="links") {
         current.links = div;
         current.linksEditor = new APPLinkEditor(div);
         current.toggleLinks(false);
      } else if (className=="categories") {
         current.categories = div;
         current.categoriesEditor = new APPCategoryEditor(div);
         current.toggleCategories(false);
      } else if (className=="content") {
         current.content = div;
         current.toggleContent(false);
      }
   });
}

APPEntryEditor.prototype.setEntry = function(entry) {
   this.entry = entry;
   if (this.linksEditor) {
      this.linksEditor.target = this.entry;
   }
   if (this.categoriesEditor) {
      this.categoriesEditor.target = this.entry;
   }
}

APPEntryEditor.prototype.init = function(entry) {
   this.toggleLinks(false);
   this.toggleCategories(false);
   this.toggleContent(false);
   if (this.contentLink) {
      if (this.allowContentEdit) {
         this.contentLink.style.display = "inline";
      } else {
         this.contentLink.style.display = "none";
      }
   }
   this.entry = entry;
   if (this.content) {
      DOMUtil.clearChildren(this.content);
   }
   if (this.entry) {
      DOMUtil.text(this.cancelButton,"Close");
      if (this.title) {
         var content = entry.getTitle();
         this.title.value = content ? DOMUtil.text(content.xml) : "";
      }
      if (this.summary) {
         var content = entry.getSummary();
         this.summary.value = content ? DOMUtil.text(content.xml) : "";
      }
      var current = this;
      var entryContent = this.entry.getContent();
      if (!entryContent) {
         this._showNoContent();
      } else if (entryContent.getMediaReference()){
         if (this.content) {
            var p = DOMUtil.element(this.content, {
               localName: "p",
               namespace: "http://www.w3.org/1999/xhtml",
               children: [ 
                  {localName: "a",
                    attributes: {href: entryContent.getLocation().spec, title: "Download", target: "_blank"},
                    children: [entryContent.getMediaReference()]
                  },
                  " ( ",
                  entryContent.getType(),
                  " ) ",
                  {localName: "a",
                    attributes: {href: "#", rel: "upload-update", title: "Upload an update"},
                    children: [ "[upload]"]
                  }
               ]
            },true,true);
            if (this.isEditableType(entryContent.getType())) {
               DOMUtil.element(p,{
                  localName: "a",
                  attributes: {href: "#", rel: "edit-content", title: "Edit the content"},
                  children: [ "[edit]"]
               },true,true);
            }
            var uploadDiv = DOMUtil.element(this.content, {
               localName: "div",
               className: "upload"
            },true,true);
            uploadDiv.style.display = "none";
            var editDiv = DOMUtil.element(this.content, {
               localName: "div",
               className: "edit",
               children: [
                  {
                     localName: "div",
                     className: "controls",
                     children: [
                        {localName: "button", attributes: {name: "save"}, children: [ "Save"]},
                        {localName: "button", attributes: {name: "revert"}, children: [ "Revert"]},
                        {localName: "button", attributes: {name: "cancel"}, children: [ "Close"]},
                        {localName: "span"}
                     ]
                  },
                  {
                     localName: "textarea",
                     attributes: {rows: "40", cols: "80"}
                  }
               ]
            },true,true);
            var editStatus = editDiv.firstChild.childNodes[3];
            editDiv.style.display = "none";
            var editText = editDiv.firstChild.nextSibling;
            var editButtons = {};
            DOMUtil.findDescendant(editDiv,null,"button",function(button) {
               if (button.name=="save") {
                  editButtons.save = button;
                  button.onclick = function() {
                     editText.disabled = true;
                     editButtons.save.disabled = true;
                     editButtons.revert.disabled = true;
                     DOMUtil.text(editStatus,"Saving changes...");
                     HTTP("PUT",entryContent.getLocation().spec,
                     {
                        contentType: entryContent.getType(),
                        body: editText.value,
                        onSuccess: function(status) {
                           editText.disabled = false;
                           editButtons.save.disabled = false;
                           editButtons.revert.disabled = false;
                           DOMUtil.clearChildren(editStatus);
                        },
                        onFailure: function(status) {
                           DOMUtil.text(editStatus,"Failure to update content, status="+status);
                        }
                     });
                  }
               } else if (button.name=="revert") {
                  editButtons.revert = button;
                  button.onclick = function() {
                     editText.disabled = true;
                     editButtons.save.disabled = true;
                     editButtons.revert.disabled = true;
                     DOMUtil.text(editStatus,"Loading content, please wait...");
                     HTTP("GET",entryContent.getLocation().spec, {
                        onSuccess: function(status,xml,text) {
                           editText.value = text;
                           editText.disabled = false;
                           editButtons.save.disabled = false;
                           editButtons.revert.disabled = false;
                           DOMUtil.clearChildren(editStatus);
                        },
                        onFailure: function(status) {
                           DOMUtil.text(editStatus,"Cannot load content, status="+status);
                        }
                     });
                  }
               } else if (button.name=="cancel"){
                  editButtons.cancel = button;
                  button.onclick = function() {
                     editDiv.style.display = "none";
                  }
               }
            });
            DOMUtil.findDescendant(p,null,"a",function(a) {
               var rel = a.getAttribute("rel");
               if (rel=="upload-update") {
                  a.onclick = function() {
                     editDiv.style.display = "none";
                     uploadDiv.style.display = "block";
                     DOMUtil.clearChildren(uploadDiv);
                     var upload = new Upload();
                     upload.onCancel = function() {
                        DOMUtil.clearChildren(uploadDiv);
                        uploadDiv.style.display = "none";
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
                              upload.onCancel();
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


                     upload.start(current.uploadBase,current.entry.feed.collection.href,entryContent.getMediaReference());
                     return false;
                  };
               } else if (rel=="edit-content") {
                  a.onclick = function() {
                     editDiv.style.display = "block";
                     uploadDiv.style.display = "none";
                     editButtons.revert.onclick();
                     return false;
                  };
               }
            });
         }
      } else {
         this._initContent(entryContent.getType(),entryContent.xml);
      }
   } else {
      if (this.title) {
         this.title.value = "";
      }
      if (this.summary) {
         this.summary.value = "";
      }
      DOMUtil.text(this.cancelButton,"Cancel");
      this._showNoContent();
   }
   if (this.linksEditor) {
      this.linksEditor.init(this.entry);
   }
   if (this.categoriesEditor) {
      this.categoriesEditor.init(this.entry);
   }
}

APPEntryEditor.prototype.isEditableType = function(type) {
   return type.indexOf("text/")==0 || type=="application/xml" || type.indexOf("+xml")==(type.length-4) || type=="application/x-javascript";
}

APPEntryEditor.prototype._showNoContent = function() {
   if (!this.content) {
      return;
   }
   this.contentType = null;
   DOMUtil.element(this.content, {
      localName: "p",
      namespace: "http://www.w3.org/1999/xhtml",
      children: [ "This entry currently has no content." ]
   },true,true);
   var form = DOMUtil.element(this.content, {
      localName: "form",
      namespace: "http://www.w3.org/1999/xhtml",
      children: [ 
         {localName: "p",
           children: [ "Would you like to create one of the following content types?"]
         },
         {localName: "p",
           children: [
               {localName: "input",
                 attributes: {name: "type", type: "radio", value: "xhtml", checked: "checked"}},
               "XHTML (formatted content) ",
               {localName: "input",
                 attributes: {name: "type", type: "radio", value: "text"}},
               "Plain Text ",
               {localName: "input",
                 attributes: {name: "type", type: "radio", value: "text/xml"}},
               "XML "
           ]
         },
         {localName: "p",
           children: [ 
            {localName: "button",
              attributes: {type: "button", value: "create"},
              children: ["Yes"]
            }]
         }
      ]
   },true,true);
   var options = {};
   var current = this;
   DOMUtil.findDescendant(form,null,"input",function(i) {
      options[i.value] = i;
   });
   DOMUtil.findDescendant(form,null,"button",function(button) {
      button.onclick = function() {
         for (var value in options) {
            if (options[value].checked) {
               DOMUtil.clearChildren(current.content);
               current._initContent(value);
            }
         }
      };
   });
}

APPEntryEditor.prototype._initContent = function(type,xml) {
   this.contentType = type;
   if (type=='text') {
      this.contentEditor = this.textEditor;
   } else if (type=='xhtml') {
      this.contentEditor = this.xhtmlEditor;
   } else if (type=='text/xml') {
      this.contentEditor = this.xmlEditor;
   } else {
      this.contentEditor = this.textEditor;
   }
   if (!this.content) {
      return;
   }
   this.contentEditor.attach(this.content,type,xml);
}

APPEntryEditor.prototype._text = function(e,message) {
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

APPEntryEditor.prototype.toggleLinks = function(flag) {
   this.linksShown = flag;
   if (!this.links) {
      return;
   }
   if (this.linksShown) {
      this.links.style.display = "block";
   } else {
      this.links.style.display = "none";
   }
   this._text(this.linksLink, this.linksShown ? "[hide links]" : "[show links]");
}

APPEntryEditor.prototype.toggleCategories = function(flag) {
   this.categoriesShown = flag;
   if (!this.categories) {
      return;
   }
   if (this.categoriesShown) {
      this.categories.style.display = "block";
   } else {
      this.categories.style.display = "none";
   }
   this._text(this.categoriesLink, this.categoriesShown ? "[hide categories]" : "[show categories]");
}

APPEntryEditor.prototype.toggleContent = function(flag) {
   this.contentShown = flag;
   if (!this.content) {
      return;
   }
   if (this.contentShown) {
      this.content.style.display = "block";
   } else {
      this.content.style.display = "none";
   }
   this._text(this.contentLink, this.contentShown ? "[hide content]" : "[show content]");
}

APPEntryEditor.prototype.updateEntry = function() {
   if (this.title) {
      var value = this.title.value+"";
      value = value.trim();
      var content = this.entry.getTitle(true);
      content.setTextContent(value);
   }
   if (this.summary) {
      var value = this.summary.value+"";
      value = value.trim();
      if (value.length>0) {
         var content = this.entry.getSummary(true);
         content.setTextContent(value);
         content.setType("text");
      } else {
         var content = this.entry.getSummary();
         if (content) {
            DOMUtil.remove(content.xml);
         }
      }
   }
   if (this.contentType) {
      var entryContent = this.entry.getContent();
      if (!entryContent) {
         entryContent = this.entry.getContent(true);
         entryContent.setType(this.contentType);
      }
      var o = this.contentEditor.getContent();
      if (typeof(o)=='string') {
         entryContent.setTextContent(o);
      } else {
         entryContent.setNodeContent(o);
      }
   }
   if (this.linksEditor) {
      this.linksEditor.updateTarget();
   }
   if (this.categoriesEditor) {
      this.categoriesEditor.updateTarget();
   }

}