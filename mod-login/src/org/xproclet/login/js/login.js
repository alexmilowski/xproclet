String.prototype.trim =
function() {
  return this.replace( /^[ \n\r\t]+|[ \n\r\t]+$/g, "" );
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

function Login(base) {
   this.msie = typeof ActiveXObject != "undefined";
   this.base = base;
   this.loginId = null;
   if (!this.base) {
      this.base = document.location.protocol+"//"+document.location.host+"/services/login/";
   }
   //alert(this.base);
   var searchPos = document.location.search.indexOf("url=");
   if (searchPos>=0) {
      this.afterLogin = document.location.search.substring(searchPos+4);
   } else {
      this.afterLogin = document.location.protocol+"//"+document.location.host+"/";
   }
   this.forgotPassword = null;
   //alert(this.afterLogin);
}

Login.prototype.check = function(id) {
   var current = this;
   HTTP("GET",this.base+"status/check",
     {
        onSuccess: function(status) {
           window.location = current.afterLogin;
        },
        onFailure: function(status) {
           current.showLogin(id);
        }
     }
  );
}

Login.prototype.onForgotPassword = function(alias)
{
   if (!alias || alias.length==0) {
      if (this.onPromptForgotPassword) {
         this.onPromptForgotPassword();
      }
      return;
   }
   this.doForgotPassword(alias);
}

Login.prototype.doForgotPassword = function(alias,email)
{
   if (!this.forgotPassword) {
      return;
   }
   if (this.onBeforeForgotPassword) {
      this.onBeforeForgotPassword();
   }
   var current = this;
   HTTP("POST",this.forgotPassword,
   {
      body: alias ? "alias="+escape(alias) : "email="+escape(email),
      contentType: "application/x-www-form-urlencoded",
      onSuccess: function(status) {
         if (current.onForgotPasswordSuccess) {
            current.onForgotPasswordSuccess();
         }
      },
      onFailure: function(status) {
         current.showMessage("Failed to request password recovery.");
      }
   });
   if (current.onAfterForgotPassword) {
      current.onAfterForgotPassword();
   }
}

Login.prototype.showMessage = function(message)
{
   var loginDiv = document.getElementById(this.loginId);
   this._findChild(loginDiv,null,"P",function(e) {
      loginDiv.removeChild(e);
   });         
   this._findChild(loginDiv,null,"p",function(e) {
      loginDiv.removeChild(e);
   });         
   var msg = loginDiv.ownerDocument.createElement("p");
   msg.setAttribute("class","login-message");
   loginDiv.appendChild(msg);
   msg.appendChild(loginDiv.ownerDocument.createTextNode(message));
}

Login.prototype.doLogin = function(form)
{
   // clear messages
   var loginDiv = document.getElementById(this.loginId);
   this._findChild(loginDiv,null,"P",function(e) {
      loginDiv.removeChild(e);
   });         
   var alias = form.username.value+"";
   alias = alias.trim();
   var current = this;
   var extras = "";
   if (this.domain) {
      extras += "&domain="+this.domain;
   }
   var password = form.password.value;
   if (this.onBeforeLogin) {
      var data = this.onBeforeLogin(alias,form.password.value);
      if (!data) { return false; }
      if (data.alias) {
         alias = data.alias;
      }
      if (data.password) {
         password = data.password;
      }
   }
   this.showMessage("Sending login...");
   HTTP("POST",this.base+"auth",
   {
      body: "username="+escape(alias)+"&password="+escape(password)+extras,
      contentType: "application/x-www-form-urlencoded",
      onSuccess: function(status) {
         document.location = current.afterLogin;
      },
      onFailure: function(status) {
         current.showMessage("Login failed.");
      }
   });
   return false;
}

var _loginForms = {};

Login.prototype.showLogin = function(id)
{
   this.loginId = id;
   var current = this;
   var loginDiv = document.getElementById(id);
   if (!loginDiv) {
      alert("Cannot find login div with id "+id);
   }
   HTTP("GET",this.base,
      {
         onSuccess: function(status,xml,text)
         {
            //try {
            current._clearChildren(loginDiv);
            if (!xml || !xml.documentElement) {
               loginDiv.innerHTML = text;
            } else {
               var child = current._importNode(document,xml.documentElement,true);
               loginDiv.appendChild(child);
            }
            current._findDescendant(loginDiv,null,"FORM",function(form) {
               form.onsubmit = function() {
                  current.doLogin(form);
                  return false;
               };
               current.form = form;
            });
            current._findDescendant(loginDiv,null,"form",function(form) {
               form.onsubmit = function() {
                  current.doLogin(form);
                  return false;
               };
               current.form = form;
            });
            current._findDescendant(loginDiv,null,"A",function(a) {
               if (current.forgotPasword) {
                  a.onclick = function() {
                     var alias = current.form.username.value;
                     alias = alias.trim();
                     current.onForgotPassword(alias);
                     return false;
                  }
               } else {
                  a.style.display = "none";
               }
            });
            current._findDescendant(loginDiv,null,"a",function(a) {
               if (current.forgotPassword) {
                  a.onclick = function() {
                     var alias = current.form.username.value;
                     alias = alias.trim();
                     current.onForgotPassword(alias);
                     return false;
                  }
               } else {
                  a.style.display = "none";
               }
            });
            //} catch(ex) {
            //   alert(ex);
            //}
         }
      }
   );
}
   
Login.prototype._clearChildren = function(parent) {
   while (parent.childNodes.length>0) {
      parent.removeChild(parent.childNodes.item(0));
   }
}

Login.prototype._findDescendant = function(parent,namespace,name,handler) {
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      if (current.localName) {
         if (!namespace && current.localName==name) {
            handler(current);
         } else if (current.localName==name && current.namespaceURI==namespace) {
            handler(current);
         }
      } else if (current.nodeName==name) {
         handler(current);
      }
      this._findDescendant(current,namespace,name,handler)
      current = current.nextSibling;
   }
}

Login.prototype._findChild = function(parent,namespace,name,handler) {
   var current = parent.firstChild;
   while (current) {
      if (current.nodeType!=1) {
         current = current.nextSibling;
         continue;
      }
      var next = current.nextSibling;
      if (current.localName) {
         if (!namespace && current.localName==name) {
            handler(current);
         } else if (current.localName==name && current.namespaceURI==namespace) {
            handler(current);
         }
      } else if (current.nodeName==name) {
         handler(current);
      }
      current = next;
   }
}

Login.prototype._importNode = function(doc,node, allChildren) {
  switch (node.nodeType) {
    case 1:
      var newNode = doc.createElement(node.nodeName);
      /* does the node have any attributes to add? */
      if (node.attributes && node.attributes.length > 0) {
         for (var i = 0; i < node.attributes.length; i++) {
            newNode.setAttribute(node.attributes[i].nodeName, node.getAttribute(node.attributes[i].nodeName));
         }
      }
      /* are we going after children too, and does the node have any? */
      if (allChildren && node.childNodes && node.childNodes.length > 0) {
         for (var i = 0; i < node.childNodes.length; i++) {
            newNode.appendChild(this._importNode(doc,node.childNodes[i], allChildren));
         }
      }
      return newNode;
      break;
    case 3:
    case 4:
      return doc.createTextNode(node.nodeValue);
      break;
  }
}

