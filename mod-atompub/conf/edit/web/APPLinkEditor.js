function APPLinkEditor(div) {
   this.div = div;
   this.links = [];
   this.hideRelations = {};
   this.hideRelations['edit'] = true;
   this.hideRelations['edit-media'] = true;
   this.hideRelations['self'] = true;
   var current = this;
   DOMUtil.findDescendant(div,null,"table",function(table) {
      current.table = table;
   });
   DOMUtil.forChild(this.table,null,"tbody",function(tbody) {
      current.table = tbody;
   });
   DOMUtil.findDescendant(div,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="add-link") {
         a.onclick = function() {
            current._addRow();
            return false;
         };
      } else if (rel=="delete-links") {
         a.onclick = function() {
            var count = 0;
            for (var i=0; i<current.links.length; i++) {
               if (current.links[i].selected) {
                  current.links[i].selected = false;
                  current.links[i].deleted = true;
                  DOMUtil.remove(current.table.childNodes[current.links[i].tableIndex]);
                  for (var j=i+1; j<current.links.length; j++) {
                     if (!current.links[j].deleted) {
                        current.links[j].tableIndex--;
                     }
                  }
               }
               if (!current.links[i].deleted) {
                  count++;
               }
               //alert(i+": "+current.links[i].deleted);
            }
            if (count==0) {
               current.table.parentNode.style.display = "none";
            }
            return false;
         };
      }
   });
}

APPLinkEditor.prototype.init = function(target)
{
   this.target = target;
   this.links = [];
   DOMUtil.clearChildren(this.table);
   this.table.parentNode.style.display = "none";
   if (this.target) {
      var links = this.target.getLinks();
      for (var i=0; i<links.length; i++) {
         if (!this.hideRelations[links[i].rel]) {
            this._addRow(links[i]);
         }
      }
   }
}

APPLinkEditor.prototype._addRow = function(link)
{
   this.table.parentNode.style.display = "block";
   var spec = {
      link: link,
      deleted: false,
      selected: false
   };
   this.links.push(spec);
   var row = DOMUtil.element(this.table,{
      localName: "tr",
      namespace: "http://www.w3.org/1999/xhtml",
      children: [
         { localName: "td",
           children: [ 
              { localName: "input",
                attributes: {name: "delete",type: "checkbox"}}
           ]},
         { localName: "td",
           children: [
              { localName: "input",
                attributes: {name: "rel", size: "10", value: link && link.rel ? link.rel : ""}}
           ]},
         { localName: "td",
           children: [
              { localName: "input",
                attributes: {name: "href", size: "40", value: link && link.href ? link.href : ""}}
           ]},
         { localName: "td",
           children: [
              { localName: "input",
                attributes: {name: "type", size: "20", value: link && link.type ? link.type : ""}}
           ]},
         { localName: "td",
           children: [
              { localName: "input",
                attributes: {name: "title", size: "20", value: link && link.title ? link.title : ""}}
           ]}
      ]
   },true,true);
   spec.tableIndex = 0;
   var currentRow = row;
   while (currentRow.previousSibling) {
      spec.tableIndex++;
      currentRow = currentRow.previousSibling;
   }
   var current = this;
   DOMUtil.findDescendant(row,null,"input",function(input) {
      var name = input.getAttribute("name");
      if (name=="delete") {
        input.addEventListener("click",function() {
           spec.selected = input.checked;
        },false);
      }
   });
}

APPLinkEditor.prototype.updateTarget = function() {
  for (var i=0; i<this.links.length; i++) {
     //alert("Link spec "+i);
     var spec = this.links[i];
     if (spec.deleted) {
        if (spec.link) {
           //alert("Removing...");
           //alert(spec.link.xml.localName+" parent "+this.target.xml.localName+" "+spec.link.xml.parentNode);
           this.target.removeLink(spec.link);
        }
     } else {
        // new link
        //alert(i+": "+spec.tableIndex);
        var row = this.table.childNodes[spec.tableIndex];
        var rel = row.childNodes[1].firstChild.value;
        if (rel.length==0) {
           rel = "related";
        }
        var href = row.childNodes[2].firstChild.value;
        if (href.length==0) {
           continue;
        }
        var type = row.childNodes[3].firstChild.value;
        if (type.length==0) {
           type = null;
        }
        var title = row.childNodes[3].firstChild.value;
        if (title.length==0) {
           title = null;
        }
        if (spec.link) {
           spec.link.set(rel,href,type,title);
        } else {
           this.target.addLink(rel,href,type,title);
        }
     }
  }
}