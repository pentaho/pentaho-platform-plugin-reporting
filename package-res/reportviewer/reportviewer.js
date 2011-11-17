var ReportViewer = {

  requiredModules: ['formatter', 'dojo'],

  /**
   * Inform the report viewer that a module has been loaded. When all required modules have been loaded the report
   * viewer will load itself.
   */
  moduleLoaded: function(name) {
    if (this._requiredModules === undefined) {
      // Create a private required modules hash where the value represents if it has been loaded
      this._requiredModules = {};
      $.each(this.requiredModules, function(i, m) {
        this._requiredModules[m] = false; // Modules are by default not loaded
      }.bind(this));
    }
    this._requiredModules[name] = true;

    var everythingLoaded = true;
    $.each(this._requiredModules, function(i, m) {
      everythingLoaded &= m;
      return !everythingLoaded; // break when any module is not loaded
    });
    if (everythingLoaded) {
      ReportViewer._load();
    }
  },

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

    $('#reportContent').load(function() {
      // Schedule the resize after the document has been rendered and CSS applied
      setTimeout(ReportViewer.view.resizeIframe.bind(this));
    });

    this.createPromptPanel();
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

    resizeIframe: function() {
      var t = $(this);
      // Reset the iframe height before polling its contents so the size is correct.
      t.width(0);
      t.height(0);

      var d = $(this.contentWindow.document);
      t.height(d.height());
      t.width(d.width());

      $('#reportPageOutline').width(t.outerWidth());
      ReportViewer.view.resize();
    },

    showMessageBox: function( message, dialogTitle, button1Text, button1Callback, button2Text, button2Callback, blocker ) {

      var messageBox = dijit.byId('messageBox');

      messageBox.setTitle(dialogTitle);
      messageBox.setMessage(message);
      
      if (blocker) {
        messageBox.setButtons([]);
      } else {
        var closeFunc = function() {
          Dashboards.hideProgressIndicator();
          messageBox.hide.call(messageBox);
        }

        if(!button1Text) {
          button1Text = Messages.getString('OK');
        }
        if(!button1Callback) {
          button1Callback = closeFunc;
        }

        messageBox.onCancel = closeFunc;

        if(button2Text) {
          messageBox.callbacks = [
            button1Callback, 
            button2Callback
          ];
          messageBox.setButtons([button1Text,button2Text]);
        } else {
          messageBox.callbacks = [
            button1Callback 
          ];
          messageBox.setButtons([button1Text]);
        }
      }
      Dashboards.showProgressIndicator();
      messageBox.show();
    }
  },

  createPromptPanel: function() {
    ReportViewer.fetchParameterDefinition(undefined, function(paramDefn) {
    var panel = new pentaho.common.prompting.PromptPanel(
      'promptPanel',
      paramDefn);
    panel.submit = ReportViewer.submitReport;
    panel.getParameterDefinition = ReportViewer.fetchParameterDefinition.bind(ReportViewer);
    panel.schedule = ReportViewer.scheduleReport;

    // Provide our own text formatter
      panel.createDataTransportFormatter = ReportFormatUtil.createDataTransportFormatter.bind(ReportFormatUtil);
      panel.createFormatter = ReportFormatUtil.createFormatter.bind(ReportFormatUtil);

    var init = panel.init;
    panel.init = function() {
      this.view.init(init, panel);
    }.bind(this);

    // Provide our own i18n function
    panel.getString = Messages.getString;

    panel.init();
    }.bind(this));
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
      top.reportViewer_openUrlInDialog = ReportViewer.openUrlInDialog;
    }
    window.reportViewer_openUrlInDialog = top.reportViewer_openUrlInDialog;
    window.reportViewer_hide = ReportViewer.hide;
  },

  getLocale: function() {
    var locale = this.getUrlParameters().locale;
    if (locale && locale.length > 2) {
      locale = locale.substring(0, 2);
    }
    return locale;
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
    ReportViewer.view.resize();
  },

  parameterParser: new pentaho.common.prompting.ParameterXmlParser(),
  parseParameterDefinition: function(xmlString) {
    // Provide a custom parameter normalization method unique to report viewer
    this.parameterParser.normalizeParameterValue = ReportFormatUtil.normalizeParameterValue.bind(ReportFormatUtil);
    return this.parameterParser.parseParameterXml(xmlString);
  },

  getUrlParameters: function() {
    var urlParams = {};
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        reg = /([^&=]+)=?([^&]*)/g,
        decode = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        query = window.location.search.substring(1);

    while (e = reg.exec(query)) {
      var paramName = decode(e[1]);
      var paramVal = decode(e[2]);

      if (urlParams[paramName] !== undefined) {
        paramVal = $.isArray(urlParams[paramName]) 
          ? urlParams[paramName].concat([paramVal])
          : [urlParams[paramName], paramVal];
      }
      urlParams[paramName] = paramVal;
    }
    return urlParams;
  },

  /**
   * Loads the parameter xml definition from the server.
   * @param promptPanel panel to fetch parameter definition for
   * @param mode Render Mode to request from server: {INITIAL, MANUAL, USERINPUT}. If not provided, INITIAL will be used.
   */
  fetchParameterDefinition: function(promptPanel, callback, mode) {
    var options = this.getUrlParameters();
    // If we aren't passed a prompt panel this is the first request
    if (promptPanel) {
      $.extend(options, promptPanel.getParameterValues());
    }

    options['renderMode'] = 'XML';

    if (mode === 'USERINPUT' && !promptPanel.paramDefn.allowAutoSubmit()) {
      // only parameter without pagination of content ..
      options['renderMode'] = 'PARAMETER';
    }

    // options['renderMode'] = promptPanel ? 'XML': 'PARAMETER';

    // Never send the session back. This is generated by the server.
    delete options['::session'];

    var authenticationCallback = function() {
      var newParamDefn = ReportViewer.fetchParameterDefinition.call(this, promptPanel, callback, mode);
      promptPanel.refresh(newParamDefn);
    }.bind(this);

    var showFatalError = function(e) {
      var errorMsg = Messages.getString('ErrorParsingParamXmlMessage');
      if (console) {
        console.log(errorMsg + ": " + e);
      }
      ReportViewer.view.showMessageBox(
        errorMsg,
        Messages.getString('FatalErrorTitle'));
    };

    var newParamDefn;
    $.ajax({
      async: false,
      cache: false,
      type: 'POST',
      url: webAppPath + '/content/reporting',
      data: options,
      dataType:'text',
      success: function(xmlString) {
        if (ReportViewer.handleSessionTimeout(xmlString, authenticationCallback)) {
          return;
        }
        try {
          newParamDefn = ReportViewer.parseParameterDefinition(xmlString);
          // Make sure we retain the current auto-submit setting
          var currentAutoSubmit = promptPanel ? promptPanel.getAutoSubmitSetting() : undefined;
          if (currentAutoSubmit != undefined) {
            newParamDefn.autoSubmitUI = currentAutoSubmit;
          }
        } catch (e) {
          showFatalError(e);
        }
      }.bind(this),
      error: showFatalError
    });
    callback(newParamDefn);
  },

  _updateReport: function(promptPanel, renderMode) {
    if (promptPanel.paramDefn.promptNeeded) {
      $('#' + this.htmlObject).attr('src', 'about:blank');
      return; // Don't do anything if we need to prompt
    }
    var options = this.getUrlParameters();
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
    ReportViewer._updateReport(promptPanel, 'REPORT');
  },

  scheduleReport: function(promptPanel) {
    ReportViewer._updateReport(promptPanel, 'SUBSCRIBE');
  },

  /**
   * Prompts the user to relog in if they're within PUC, otherwise displays a dialog
   * indicating their session has expired.
   *
   * @return true if the session has timed out
   */
  handleSessionTimeout: function(content, callback) {
    if (this.isLoginPageContent(content)) {
      this.reauthenticate(callback);
      return true;
    }
    return false;
  },

  /**
   * @return true if the content is the login page.
   */
  isLoginPageContent: function(content) {
    if(content.indexOf('j_spring_security_check') != -1) {
        // looks like we have the login page returned to us
        return true;
    }
    return false;
  },

  reauthenticate: function(f) {
    if(top.mantle_initialized) {
      var callback = {
        loginCallback : f
      }
      window.parent.authenticate(callback);
    } else {
      ReportViewer.view.showMessageBox(
        Messages.getString('SessionExpiredComment'),
        Messages.getString('SessionExpired'),
        Messages.getString('OK'), 
        ReportViewer.view.closeMessageBox,
        undefined,
        undefined,
        true
      );
        // ,
        // Messages.getString('No_txt'), 
        // ReportViewer.view.closeMessageBox);
    }
  }
};