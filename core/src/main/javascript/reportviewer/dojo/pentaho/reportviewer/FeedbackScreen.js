/*!
 * Copyright 2010 - 2017 Hitachi Vantara.  All rights reserved.
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
      "pentaho/common/button", "pentaho/common/Dialog", "dojo/text!pentaho/reportviewer/FeedbackScreen.html"],
    function (declare, _WidgetBase, _Templated, on, query, button, Dialog, templateStr) {
      return declare("pentaho.reportviewer.FeedbackScreen", [Dialog],
          {
            buttons: ['cancel'],

            imagePath: '',

            hasTitleBar: false,

            setTitle: function (title) {
              this.feedbacktitle.innerHTML = title;
            },

            setText: function (text) {
              this.feedbackmessage.innerHTML = text;
            },

            setText2: function (text) {
              this.feedbackmessage2.innerHTML = text;
            },

            setText3: function (text) {
              this.feedbackmessage3.innerHTML = text;
            },

            setCancelText: function (text) {
              var i = this.buttons.length > 1 ? 1 : 0;
              this.buttons[i] = text;
              query("#button" + i, this.domNode).forEach(function (node, index, arr) {
                node.innerHTML = text;
              });
            },

            hideBackgroundBtn: function () {
              if (this.buttons.length > 1) {
                this.buttons.shift();
              }
            },

            showBackgroundBtn: function (text) {
              if (this.buttons.length == 1) {
                this.buttons.unshift(text);
                query("#button" + 1, this.domNode).forEach(function (node, index, arr) {
                  node.innerHTML = text;
                });
              }
            },

            templateString: templateStr,

            postCreate: function () {
              this.inherited(arguments);
            }

          }
      );
    });

