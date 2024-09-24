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
