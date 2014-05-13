function APPCategoryEditor(div) {
   this.div = div;
   this.categories = [];
   var current = this;
   DOMUtil.findDescendant(div,null,"table",function(table) {
      current.table = table;
   });
   DOMUtil.forChild(this.table,null,"tbody",function(tbody) {
      current.table = tbody;
   });
   DOMUtil.findDescendant(this.div,null,"a",function(a) {
      var rel = a.getAttribute("rel");
      if (rel=="add-category") {
         a.onclick = function() {
            current._addRow();
            return false;
         };
      } else if (rel=="delete-categories") {
         a.onclick = function() {
            var count = 0;
            for (var i=0; i<current.categories.length; i++) {
               if (current.categories[i].selected) {
                  current.categories[i].selected = false;
                  current.categories[i].deleted = true;
                  //alert("Table Index: "+current.categories[i].tableIndex);
                  DOMUtil.remove(current.table.childNodes[current.categories[i].tableIndex]);
                  for (var j=i+1; j<current.categories.length; j++) {
                     if (!current.categories[j].deleted) {
                        current.categories[j].tableIndex--;
                     }
                  }
               }
               if (!current.categories[i].deleted) {
                  count++;
               }
            }
            if (count==0) {
               current.table.parentNode.style.display = "none";
            }
            return false;
         };
      }
   });
}

APPCategoryEditor.prototype.init = function(target)
{
   this.categories = [];
   this.target = target;
   DOMUtil.clearChildren(this.table);
   
   this.table.parentNode.style.display = "none";
   if (this.target) {
      var categories = this.target.getCategories();
      for (var i=0; i<categories.length; i++) {
         this._addRow(categories[i]);
      }
   }
}

APPCategoryEditor.prototype._addRow = function(category)
{
   this.table.parentNode.style.display = "block";
   var spec = {
      category: category,
      deleted: false,
      selected: false
   };
   this.categories.push(spec);
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
                attributes: {name: "scheme", size: "40", value: category ? category.scheme : ""}}
           ]},
         { localName: "td",
           children: [ 
              { localName: "input",
                attributes: {name: "term", size: "40", value: category ? category.term : ""}}
           ]},
         { localName: "td",
           children: [ 
              { localName: "input",
                attributes: {name: "value", size: "40", value: category && category.value ? category.value : ""}}
           ]},
      ]
   },true,true);
   spec.tableIndex = 0;
   var currentRow = row;
   while (currentRow.previousSibling) {
      spec.tableIndex++;
      currentRow = currentRow.previousSibling;
   }
   DOMUtil.findDescendant(row,null,"input",function(input) {
      var name = input.getAttribute("name");
      if (name=="delete") {
        input.addEventListener("click",function() {
           spec.selected = input.checked;
        },false);
      }
   });
}

APPCategoryEditor.prototype.updateTarget = function() {
  for (var i=0; i<this.categories.length; i++) {
     var spec = this.categories[i];
     if (spec.deleted) {
        if (spec.category) {
           this.target.removeCategory(spec.category)
        }
     } else {
        //alert(spec.tableIndex+" "+this.table.childNodes.length);
        var row = this.table.childNodes[spec.tableIndex];
        var scheme = row.childNodes[1].firstChild.value;
        if (scheme.length==0) {
           scheme = null;
        }
        var term = row.childNodes[2].firstChild.value;
        if (term.length==0) {
           if (spec.category) {
              this.target.removeCategory(spec.category);
           }
           continue;
        }
        var value = row.childNodes[3].firstChild.value;
        if (value.length==0) {
           value = null;
        }
        if (spec.category) {
           spec.category.set(scheme,term,value);
        } else {
           this.target.addCategory(scheme,term,value);
        }
     }
  }
}