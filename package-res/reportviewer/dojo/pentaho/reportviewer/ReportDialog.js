dojo.provide('pentaho.reportviewer.ReportDialog');
dojo.require('dijit._Widget');
dojo.require('dijit._Templated');
dojo.require('pentaho.common.button');
dojo.require('pentaho.common.Dialog');
dojo.declare(
  'pentaho.reportviewer.ReportDialog',
  [pentaho.common.Dialog],
  {
    hasCloseIcon: false,

    width: '800px',
    height: '600px',

    buttons: ['OK'],

    templateString: '<div style="padding: 5px 10px 10px 10px;" class="dialog-content"><iframe dojoAttachPoint="iframe" width="100%" height="100%"></iframe></div>',

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

      dojo.style(this.domNode, 'width', this.cleanseSizeAttr(width, this.width));
      dojo.style(this.domNode, 'height', this.cleanseSizeAttr(height, this.height));

      dojo.attr(this.iframe, 'src', url);
      this.show();
    },

    postCreate: function() {
      this.inherited(arguments);
      this.callbacks = [dojo.hitch(this, this.onCancel)];
    }
  }
);