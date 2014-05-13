function Upload() {
   this.baseURL = null;
   this.id = null;
   this.onReady = function() {};
   this.onStatus = function() {};
   this.onCancel = function() {};
   this.onUpload = function() {};
   this.validate = function() {return true};
}



Upload.prototype.start = function(baseURL,path,resource)
{
   this.baseURL = baseURL;
   var current = this;
   HTTP("POST",this.baseURL+"start/",{
      contentType: "application/x-www-form-urlencoded",
      body: "update="+(resource ? "true" : "false"),
      onSuccess: function(status,xml) {
         current.id = xml.documentElement.getAttribute("id");
         current.uploadURL = current.baseURL+current.id+"/upload/"+path+(resource ? resource : "");
         current.statusURL = current.baseURL+current.id+"/status/";
         current.cancelURL = current.baseURL+current.id+"/cancel/";
         current.onReady(status);
      },
      onFailure: function(status) {
         current.onReady(status);
      }
   });
}

Upload.prototype.requestStatus = function() {
   var current = this;
   HTTP("GET",this.statusURL,{
      onSuccess: function(statusCode,xml) {
         var size = xml.documentElement.getAttribute("size");
         var progress = xml.documentElement.getAttribute("progress");
         var status = xml.documentElement.getAttribute("status");
         var entryId = xml.documentElement.getAttribute("entry-id");
         var cancelled = xml.documentElement.getAttribute("cancelled")=="true";
         var data = {
            size: parseInt(size),
            progress: parseInt(progress),
            entryId: entryId,
            cancelled: cancelled
         };
         if (status) {
            data.status = parseInt(status);
         }
         current.onStatus(data);
      },
      onFailure: function(statusCode) {
         current.onStatus({ status: statusCode});
      }
   })
}

Upload.prototype.cancel = function() {
   HTTP("GET",this.cancelURL,{
      onSuccess: function(status,xml) {
      }
   })
}
   
Upload.prototype.showForm = function(parent,width,height) {
   this.iframe = DOMUtil.element(parent,{
      localName: "iframe",
      namespace: "http://www.w3.org/1999/xhtml",
      attributes: {width: width.toString(), height: height.toString(), frameborder: '0'}
   },true,true);
   var current = this;
   this.iframe.onload = function() {
      var cancel = current.iframe.contentDocument.getElementById("cancel");
      if (cancel) {
         cancel.onclick = function() {
            current.onCancel();
         };
      }
      var uploadForm = current.iframe.contentDocument.getElementById("form");
      if (uploadForm) {
        uploadForm.onsubmit = function() {
           if (uploadForm.elements[0].value=="") {
              return false;
           } else {
              if (current.validate(uploadForm.elements[0].value)) {
                setTimeout(function() {
                   current.onUpload(uploadForm.elements[0].value);
                },10);
                return true;
              } else {
                return false;
              }
           }
        };
      }
   };
   this.iframe.src = this.uploadURL;
   
}
