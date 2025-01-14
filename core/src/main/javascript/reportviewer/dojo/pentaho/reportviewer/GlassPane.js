/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

define(["dojo/_base/declare", "dijit/_WidgetBase", "dijit/_Templated", "dojo/on", "dojo/query",
"pentaho/common/button", "pentaho/common/Dialog", "dojo/text!pentaho/reportviewer/GlassPane.html", "common-ui/dompurify"],
    function(declare, _WidgetBase, _Templated, on, query, button, Dialog, templateStr, DOMPurify){
      return declare("pentaho.reportviewer.GlassPane", [Dialog],
      {
        buttons: ['ok'],
        imagePath: '',
        hasTitleBar: false,

        setTitle: function(title) {
            this.glasspanetitle.innerHTML = DOMPurify.sanitize(title);
        },

        setText: function(text) {
            this.glasspanemessage.innerHTML = DOMPurify.sanitize(text);
        },

        setButtonText: function(text) {
          this.buttons[0] = text;
          query("#button"+0, this.domNode).forEach(function(node, index, arr){
            node.innerHTML = DOMPurify.sanitize(text);
          });
        },
    
        templateString: templateStr,

        postCreate: function() {
           this.inherited(arguments);
       }
      });
    });
