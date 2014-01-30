/*!
* This program is free software; you can redistribute it and/or modify it under the
* terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
* Foundation.
*
* You should have received a copy of the GNU Lesser General Public License along with this
* program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
* or from the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*
* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
* See the GNU Lesser General Public License for more details.
*
* Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
*/
define(["dojo/_base/declare", "dojo/on", "dojo/query", "dojo/_base/lang", 'dijit/_WidgetBase',
  'dijit/_TemplatedMixin',
  'pentaho/common/button',
  'pentaho/common/Dialog', "dojo/dom-style", "dojo/dom-attr"],
    function(declare, on, query, lang, _WidgetBase, _TemplatedMixin, button, Dialog, style, attr){
      return declare(
  'pentaho.reportviewer.ReportDialog',
  [pentaho.common.Dialog],
  {
    hasCloseIcon: false,

    width: '800px',
    height: '600px',

    buttons: ['OK'],

    templateString: '<div style="padding: 5px 10px 10px 10px;" class="dialog-content"><iframe data-dojo-attach-point="iframe" width="100%" height="100%"></iframe></div>',

    cleanseSizeAttr: function(attr, defaultValue) {
      if (attr === undefined) {
        return defaultValue;
      }
      if (!/.*px$/.exec(attr)) {
        attr = attr + 'px';
      }
      return attr;
    },

    open: function(title, url, width, height) {
      this.setTitle(title);

      style.set(this.domNode, 'width', this.cleanseSizeAttr(width, this.width));
      style.set(this.domNode, 'height', this.cleanseSizeAttr(height, this.height));

      attr.set(this.iframe, 'src', url);
      this.show();
    },

    postCreate: function() {
      this.inherited(arguments);
      this.callbacks = [lang.hitch(this, this.onCancel)];
    }
  }
);
});
