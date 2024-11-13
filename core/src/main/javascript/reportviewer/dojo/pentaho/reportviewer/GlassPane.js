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
"pentaho/common/button", "pentaho/common/Dialog", "dojo/text!pentaho/reportviewer/GlassPane.html", "common-ui/util/xss"],
    function(declare, _WidgetBase, _Templated, on, query, button, Dialog, templateStr, xssUtil){
      return declare("pentaho.reportviewer.GlassPane", [Dialog],
      {
        buttons: ['ok'],
        imagePath: '',
        hasTitleBar: false,

        setTitle: function(title) {
            this.xssUtil.setHtml(this.glasspanetitle, title);
        },

        setText: function(text) {
            this.xssUtil.setHtml(this.glasspanemessage, text);
        },

        setButtonText: function(text) {
          this.buttons[0] = text;
          query("#button"+0, this.domNode).forEach(function(node, index, arr){
            xssUtil.setHtml(node, text);
          });
        },
    
        templateString: templateStr,

        postCreate: function() {
           this.inherited(arguments);
       }
      });
    });
