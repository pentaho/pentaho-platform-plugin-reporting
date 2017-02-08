/*!
* Copyright 2010 - 2016 Pentaho Corporation.  All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
*/
define(["dojo/_base/declare", "dijit/_WidgetBase", "dijit/_Templated", "dojo/on", "dojo/query",
"pentaho/common/button", "pentaho/common/Dialog", "dojo/text!pentaho/reportviewer/GlassPane.html"],
    function(declare, _WidgetBase, _Templated, on, query, button, Dialog, templateStr){
      return declare("pentaho.reportviewer.GlassPane", [Dialog],
      {
        buttons: ['ok'],
        imagePath: '',
        hasTitleBar: false,

        setTitle: function(title) {
            this.glasspanetitle.innerHTML = title;
        },

        setText: function(text) {
            this.glasspanemessage.innerHTML = text;
        },

        setButtonText: function(text) {
          this.buttons[0] = text;
          query("#button"+0, this.domNode).forEach(function(node, index, arr){
            node.innerHTML = text;
          });
        },
    
        templateString: templateStr,

        postCreate: function() {
           this.inherited(arguments);
       }
      });
    });
