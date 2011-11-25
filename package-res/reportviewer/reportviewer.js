var pentaho = pentaho || {};
pentaho.reporting = pentaho.reporting || {};

pentaho.reporting.Viewer = function(reportPrompt) {
  if (!reportPrompt) {
  	alert("report prompt is required");
  	return;
  }
  var v = {
    prompt: reportPrompt,

    _load: function() {
      dojo.require('pentaho.common.Messages');
      Messages.addUrlBundle('reportviewer', '../../ws-run/ReportViewerLocalizationService/getJSONBundle');
      this.view.localize();

      this.createRequiredHooks();

      this.view.updatePageBackground();

      dojo.connect(dijit.byId('toolbar-parameterToggle'), "onClick", this, function() {
        this.view.togglePromptPanel();
      }.bind(this));

      this.view.resize();
      var resizeIframe = function(iframe) {
        this.view.resizeIframe(iframe);
      }.bind(this);
      $('#reportContent').load(function() {
        // Schedule the resize after the document has been rendered and CSS applied
        setTimeout(resizeIframe(this));
      });

      this.prompt.schedule = this.scheduleReport.bind(this);
      this.prompt.submit = this.submitReport.bind(this);

      var decorated = this.prompt.initPromptPanel.bind(this.prompt);

      this.prompt.initPromptPanel = function() {
        // Decorate the original init to first initialize our view then the panel
        var init = this.prompt.panel.init;
          this.prompt.panel.init = function() {
          this.view.init(init, this.prompt.panel);
        }.bind(this);
        decorated();
      }.bind(this);

      this.prompt.createPromptPanel();
    },

    view: {
      /**
       * Localize the Report Viewer.
       */
      localize: function() {
        $('#toolbar-parameterToggle').attr('title', Messages.getString('parameterToolbarItem_title'));
        dijit.byId('pageControl').registerLocalizationLookup(Messages.getString);
      },

      /**
       * Update the page background when we're not in PUC or we're embedded in an
       * iframe to make sure the translucent styling has some contrast.
       */
      updatePageBackground: function() {
        /**
         * If we're not in PUC or we're in an iframe
         */
        if(!top.mantle_initialized || top !== self) {
          dojo.addClass(document.body, 'pentaho-page-background');
        }
      },

      init: function(init, promptPanel) {
        if (!promptPanel.paramDefn.showParameterUI()) {
          // Hide the toolbar elements
          dojo.addClass('toolbar-parameter-separator', 'hidden');
          dojo.addClass('toolbar-parameterToggle', 'hidden');
        }
        this.showPromptPanel(promptPanel.paramDefn.showParameterUI());
        init.call(promptPanel);
        this.refreshPageControl(promptPanel);
      },

      refreshPageControl: function(promptPanel) {
        var pc = dijit.byId('pageControl');
        pc.registerPageNumberChangeCallback(undefined);
        if (!promptPanel.paramDefn.paginate) {
          pc.setPageCount(1);
          pc.setPageNumber(1);
          // pc.disable();
        } else {
          var total = promptPanel.paramDefn.totalPages;
          var page = promptPanel.paramDefn.page;
          // We can't accept pages out of range. This can happen if we are on a page and then change a parameter value
          // resulting in a new report with less pages. When this happens we'll just reduce the accepted page.
          page = Math.max(0, Math.min(page, total - 1));

          // add our default page, so we can keep this between selections of other parameters, otherwise it will not be on the
          // set of params are default back to zero (page 1)
          promptPanel.setParameterValue(promptPanel.paramDefn.getParameter('accepted-page'), '' + page);
          pc.setPageCount(total);
          pc.setPageNumber(page + 1);
        }
        pc.registerPageNumberChangeCallback(function(pageNumber) {
          this.pageChanged(promptPanel, pageNumber);
        }.bind(this));
      },

      pageChanged: function(promptPanel, pageNumber) {
        promptPanel.setParameterValue(promptPanel.paramDefn.getParameter('accepted-page'), '' + (pageNumber - 1));
        promptPanel.submit(promptPanel);
      },

      togglePromptPanel: function() {
        this.showPromptPanel(dijit.byId('toolbar-parameterToggle').checked);
      },

      showPromptPanel: function(visible) {
        if (visible) {
          dijit.byId('toolbar-parameterToggle').set('checked', true);
          dojo.removeClass('reportControlPanel', 'hidden');
        } else {
          dijit.byId('toolbar-parameterToggle').set('checked', false);
          dojo.addClass('reportControlPanel', 'hidden');
        }
        this.resize();
      },

      resize: function() {
        var ra = dojo.byId('reportArea');
        var c = dojo.coords(ra);
        var windowHeight = dojo.dnd.getViewport().h;

        dojo.marginBox(ra, {h: windowHeight - c.y});
      },

      resizeIframe: function(iframe) {
        var t = $(iframe);
        // Reset the iframe height before polling its contents so the size is correct.
        t.width(0);
        t.height(0);

        var d = $(iframe.contentWindow.document);
        t.height(d.height());
        t.width(d.width());

        $('#reportPageOutline').width(t.outerWidth());
        this.resize();
      }
    },

    createRequiredHooks: function(promptPanel) {
      if (window.reportViewer_openUrlInDialog || top.reportViewer_openUrlInDialog) {
        return;
      }
      if (!top.mantle_initialized) {
        top.mantle_openTab = function(name, title, url) {
          window.open(url, '_blank');
        }
      }
      if (top.mantle_initialized) {
        top.reportViewer_openUrlInDialog = function(title, url, width, height) {
          top.urlCommand(url, title, true, width, height);
        }
      } else {
        top.reportViewer_openUrlInDialog = this.openUrlInDialog.bind(this);
      }
      window.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
      window.reportViewer_hide = this.hide.bind(this);
    },

    openUrlInDialog: function(title, url, width, height) {
      if (this.dialog === undefined) {
        dojo.require('pentaho.reportviewer.ReportDialog');
        this.dialog = new pentaho.reportviewer.ReportDialog();
        this.dialog.setLocalizationLookupFunction(Messages.getString);
      }
      this.dialog.open(title, url, width, height);
    },

    /**
     * Hide the Report Viewer toolbar.
     */
    hide: function() {
      $('#toppanel').empty();
      this.view.resize();
    },

    _updateReport: function(promptPanel, renderMode) {
      if (promptPanel.paramDefn.promptNeeded) {
        $('#' + this.htmlObject).attr('src', 'about:blank');
        return; // Don't do anything if we need to prompt
      }
      var options = this.prompt.getUrlParameters();
      $.extend(options, promptPanel.getParameterValues());
      options['renderMode'] = renderMode;

      // SimpleReportingComponent expects name to be set
      if (options['name'] === undefined) {
        options['name'] = options['action'];
      }

      // Never send the session back. This is generated by the server.
      delete options['::session'];

      var url = "/pentaho/content/reporting?";
      var params = [];
      var addParam = function(encodedKey, value) {
        if(value.length > 0) {
          params.push(encodedKey + '=' + encodeURIComponent(value));
        }
      }
      $.each(options, function(key, value) {
        if (value === null || typeof value == 'undefined') {
          return; // continue
        }
        var encodedKey = encodeURIComponent(key);
        if ($.isArray(value)) {
          var val = [];
          $.each(value, function(i, v) {
            addParam(encodedKey, v);
          });
        } else {
          addParam(encodedKey, value);
        }
      });

      url += params.join("&");
      var iframe = $('#reportContent');
      iframe.attr("src", url);
    },

    submitReport: function(promptPanel) {
      this._updateReport(promptPanel, 'REPORT');
    },

    scheduleReport: function(promptPanel) {
      this._updateReport(promptPanel, 'SUBSCRIBE');
    }
  }

  reportPrompt._load = v._load.bind(v);
  return v;
};