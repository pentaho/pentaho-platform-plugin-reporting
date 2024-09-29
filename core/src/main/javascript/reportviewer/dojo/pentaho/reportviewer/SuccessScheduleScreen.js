/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

define(["dojo/_base/declare", "dijit/_WidgetBase", "dijit/_Templated", "dojo/on", "dojo/query",
"pentaho/common/button", "pentaho/common/Dialog", "dojo/text!pentaho/reportviewer/ScheduleScreen.html"],
    function(declare, _WidgetBase, _Templated, on, query, button, Dialog, templateStr){
      return declare("pentaho.reportviewer.SuccessScheduleScreen", [Dialog],
      {
        buttons: ['ok'],

        imagePath: '',

        setTitle: function(title) {
          this.glasspanetitle.innerHTML = title;
        },

        setText: function(text) {
          this.glasspanemessage.innerHTML = text;
        },

        setOkBtnText: function(text) {
          var i = 0;
          this.buttons[i] = text;
          query("#button"+i, this.domNode).forEach(function(node, index, arr){
            node.innerHTML = text;
          });
        },

        templateString: templateStr,

        postCreate: function() {
           this.inherited(arguments);
       }
      });
    });
