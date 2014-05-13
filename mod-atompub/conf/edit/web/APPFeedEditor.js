
String.prototype.trim =
function() {
  return this.replace( /^[ \n\r\t]+|[ \n\r\t]+$/g, "" );
}

function APPFeedEditor(div) {
   this.feed = null;
   this.div = div;
   this.saveButton = null;
   this.cancelButton = null;
   this.linksShown = false;
   this.categoriesShown = false;
   var current = this;
   this.onCancel = function() {
   };
   this.onSave = function() {
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
      }
   });
   this.title = null;
   this.subtitle = null;
   DOMUtil.findDescendant(div,null,"input",function(i) {
      if (i.name=="title") {
         current.title = i;
      } else if (i.name=="subtitle") {
         current.subtitle = i;
      }
   });
   DOMUtil.findDescendant(div,null,"textarea",function(i) {
      if (i.name=="title") {
         current.title = i;
      } else if (i.name=="subtitle") {
         current.subtitle = i;
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
      }
   });
}

APPFeedEditor.prototype.init = function(feed) {
   this.feed = feed;
   this.toggleLinks(false);
   this.toggleCategories(false);
   if (this.title) {
      var content = this.feed.getTitle();
      this.title.value = content ? DOMUtil.text(content.xml) : "";
   }
   if (this.subtitle) {
      var content = this.feed.getSubtitle();
      this.subtitle.value = content ? DOMUtil.text(content.xml) : "";
   }
   if (this.linksEditor) {
      this.linksEditor.init(this.feed);
   }
   if (this.categoriesEditor) {
      this.categoriesEditor.init(this.feed);
   }
}

APPFeedEditor.prototype._text = function(e,message) {
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

APPFeedEditor.prototype.toggleLinks = function(flag) {
   this.linksShown = flag;
   if (!this.linksEditor) {
      return;
   }
   if (this.linksShown) {
      this.links.style.display = "block";
   } else {
      this.links.style.display = "none";
   }
   this._text(this.linksLink, this.linksShown ? "[hide links]" : "[show links]");
}

APPFeedEditor.prototype.toggleCategories = function(flag) {
   this.categoriesShown = flag;
   if (!this.categoriesEditor) {
      return;
   }
   if (this.categoriesShown) {
      this.categories.style.display = "block";
   } else {
      this.categories.style.display = "none";
   }
   this._text(this.categoriesLink, this.categoriesShown ? "[hide categories]" : "[show categories]");
}

APPFeedEditor.prototype.updateFeed = function() {
   if (this.title) {
      var value = this.title.value+"";
      value = value.trim();
      var content = this.feed.getTitle(true);
      content.setTextContent(value);
   }
   if (this.subtitle) {
      var value = this.subtitle.value+"";
      value = value.trim();
      if (value.length>0) {
         var content = this.feed.getSubtitle(true);
         content.setTextContent(value);
         content.setType("text");
      } else {
         var content = this.feed.getSubtitle();
         if (content) {
            DOMUtil.remove(content.xml);
         }
      }
   }
   if (this.categoriesEditor) {
      this.categoriesEditor.updateTarget();
   }
   if (this.linksEditor) {
      this.linksEditor.updateTarget();
   }
}