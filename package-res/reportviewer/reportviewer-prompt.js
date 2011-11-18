var pentaho = pentaho || {};
pentaho.reporting = pentaho.reporting || {};

pentaho.reporting.Prompt = function() {

  return {
    _requiredModules: ['formatter', 'dojo'],

    /**
     * Inform the report viewer that a module has been loaded. When all required modules have been loaded the report
     * viewer will load itself.
     */
    moduleLoaded: function(name) {
      if (this.__requiredModules === undefined) {
        // Create a private required modules hash where the value represents if it has been loaded
        this.__requiredModules = {};
        $.each(this._requiredModules, function(i, m) {
          this.__requiredModules[m] = false; // Modules are by default not loaded
        }.bind(this));
      }
      this.__requiredModules[name] = true;

      var everythingLoaded = true;
      $.each(this.__requiredModules, function(i, m) {
        everythingLoaded &= m;
        return !everythingLoaded; // break when any module is not loaded
      });
      if (everythingLoaded) {
        this._load();
      }
    },

    _load: function() {
      dojo.require('pentaho.common.Messages');
      Messages.addUrlBundle('reportviewer', '../../ws-run/ReportViewerLocalizationService/getJSONBundle');

      this.createPromptPanel();
    },

    /**
     * Create the prompt panel
     *
     * @param callback Function to call when panel is created
     */
    createPromptPanel: function() {
      this.fetchParameterDefinition(undefined, function(paramDefn) {
        this.panel = new pentaho.common.prompting.PromptPanel(
          'promptPanel',
          paramDefn);

        this.panel.submit = this.submit.bind(this);
        this.panel.schedule = this.schedule.bind(this);
        this.panel.getParameterDefinition = this.fetchParameterDefinition.bind(this);

        // Provide our own text formatter
        this.panel.createDataTransportFormatter = ReportFormatUtil.createDataTransportFormatter.bind(ReportFormatUtil);
        this.panel.createFormatter = ReportFormatUtil.createFormatter.bind(ReportFormatUtil);

        // Provide our own i18n function
        this.panel.getString = Messages.getString;

        this.initPromptPanel();
      }.bind(this));
    },

    initPromptPanel: function() {
      this.panel.init();
    },

    /**
     * Called when the submit button is pressed or an auto-submit happens.
     */
    submit: function(promptPanel) {
      alert('submit fired for panel: ' + promptPanel);
    },

    /**
     * Called when the schedule button is pressed.
     */
    schedule: function(promptPanel) {
      alert('schedule fired for panel: ' + promptPanel);
    },

    parameterParser: new pentaho.common.prompting.ParameterXmlParser(),
    parseParameterDefinition: function(xmlString) {
      // Provide a custom parameter normalization method unique to report viewer
      this.parameterParser.normalizeParameterValue = ReportFormatUtil.normalizeParameterValue.bind(ReportFormatUtil);
      return this.parameterParser.parseParameterXml(xmlString);
    },

    /**
     * Called when there is a fatal error during parameter definition fetching/parsing
     *
     * @param e Error/exception encountered
     */
    onFatalError: function(e) {
      alert('fatal error fetching parameters: ' + e);
    },

    checkSessionTimeout: function(content, arguments) {
      if (this.isSessionTimeoutResponse(content)) {
        this.handleSessionTimeout(arguments);
        return true;
      }
      return false;
    },

    /**
     * @return true if the content is the login page.
     */
    isSessionTimeoutResponse: function(content) {
      if(content.indexOf('j_spring_security_check') != -1) {
        // looks like we have the login page returned to us
        return true;
      }
      return false;
    },

    /**
     * Handle the session timeout. This can simply inform the user or attempt to allow them to reauthenticate somehow.
     * 
     * @param arguments Original arguments passed in to fetchParameterDefinition so that it may be recalled after reauthentication.
     */
    handleSessionTimeout: function(arguments) {
      alert('session timed out');
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

      var newParamDefn;
      $.ajax({
        async: false,
        cache: false,
        type: 'POST',
        url: webAppPath + '/content/reporting',
        data: options,
        dataType:'text',
        success: function(xmlString) {
          if (this.checkSessionTimeout(xmlString, arguments)) {
            return;
          }
          try {
            newParamDefn = this.parseParameterDefinition(xmlString);
            // Make sure we retain the current auto-submit setting
            var currentAutoSubmit = promptPanel ? promptPanel.getAutoSubmitSetting() : undefined;
            if (currentAutoSubmit != undefined) {
              newParamDefn.autoSubmitUI = currentAutoSubmit;
            }
          } catch (e) {
            this.onFatalError(e);
          }
        }.bind(this),
        error: this.onFatalError
      });
      callback(newParamDefn);
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
    }
  }
}