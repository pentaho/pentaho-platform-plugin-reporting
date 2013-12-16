pen.define(['common-ui/util/util', 'common-ui/util/formatting'], function(util, ReportFormatUtil) {
  return function() {
    return logged({
      // The current prompt mode
      mode: 'INITIAL',

      load: function() {
        dojo.require('pentaho.common.Messages');
        pentaho.common.Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
      },

      /**
       * Create the prompt panel
       */
      createPromptPanel: function() {
        // Obtain initial parameter definition and
        // only then create the PromptPanel
        this.fetchParameterDefinition(
          /*promptPanel*/null,
          this._createPromptPanelFetchCallback.bind(this),
          /*promptMode*/'INITIAL');
      },

      _createPromptPanelFetchCallback: function(paramDefn) {
        var panel = this.panel = new pentaho.common.prompting.PromptPanel('promptPanel', paramDefn);

        panel.submit = this.submit.bind(this);
        panel.submitStart = this.submitStart.bind(this);
        panel.ready = this.ready.bind(this);

        // User changes the value of a parameter:
        //
        // PromptingComponent:postChange -> 
        //    PromptPanel.parameterChanged -> .refreshPrompt -> .getParameterDefinition -> 
        //      (x) We Are Here
        //      Prompt.fetchParameterDefinition ->
        //        (callback)
        //        PromptPanel.refresh -> .init ->
        //           Dashboards.init ->
        // 
        //  (...a few setTimeouts later...)
        //  
        //  SubmitPromptComponent.update -> 
        //    PromptPanel._submit -> (When auto Submit)
        //               .submit  ->
        //       ReportViewer.submitReport
        // 
        //  ScrollingPromptPanelLayoutComponent.postExecute ->
        //    PromptPanel._ready ->
        //    
        panel.getParameterDefinition = function(promptPanel, callback) {
          // Show glass pane when updating the prompt.
          dijit.byId('glassPane').show();

          // promptPanel === panel
          this.fetchParameterDefinition(promptPanel, callback, /*promptMode*/'USERINPUT');
        }.bind(this);

        // Provide our own text formatter
        panel.createDataTransportFormatter = ReportFormatUtil.createDataTransportFormatter.bind(ReportFormatUtil);
        panel.createFormatter = ReportFormatUtil.createFormatter.bind(ReportFormatUtil);

        // Provide our own i18n function
        panel.getString = pentaho.common.Messages.getString;

        this.initPromptPanel();

        this._hideLoadingIndicator();
      },

      _hideLoadingIndicator: function() {
        if (window.top.hideLoadingIndicator) {
          window.top.hideLoadingIndicator();
        } else if (window.parent.hideLoadingIndicator) {
          window.parent.hideLoadingIndicator();
        }
      },

      initPromptPanel: function() {
        this.panel.init();
      },

      ready: function(promptPanel) {
        dijit.byId('glassPane').hide();
      },

      /**
       * Called by the prompt-panel component when the CDE components have been updated.
       */
      submit: function(promptPanel, options) {
        alert('submit fired for panel: ' + promptPanel);
      },

      /**
       * Called when the prompt-panel component's submit button is pressed (mouse-down only).
       */
      submitStart: function(promptPanel) {
        alert('submit start fired for panel: ' + promptPanel);
      },

      parameterParser: new pentaho.common.prompting.ParameterXmlParser(),

      parseParameterDefinition: function(xmlString) {
        // Provide a custom parameter normalization method unique to report viewer
        this.parameterParser.normalizeParameterValue = ReportFormatUtil.normalizeParameterValue.bind(ReportFormatUtil);
        xmlString = this.removeControlCharacters(xmlString);
        return this.parameterParser.parseParameterXml(xmlString);
      },

      /**
       * This method will remove illegal control characters from the text in the range of &#00; through &#31;
       * SEE:  PRD-3882 and ESR-1953
       */
      removeControlCharacters : function(inStr) {
        for (var i = 0; i <= 31; i++) {
          var safe = i;
          if (i < 10) {
            safe = '0' + i;
          }
          eval('inStr = inStr.replace(/\&#' + safe + ';/g, "")');
        }
        return inStr;
      },

      checkSessionTimeout: function(content, args) {
        if (content.status == 401 || this.isSessionTimeoutResponse(content)) {
          this.handleSessionTimeout(args);
          return true;
        }
        return false;
      },

      /**
       * @return true if the content is the login page.
       */
      isSessionTimeoutResponse: function(content) {
        if(String(content).indexOf('j_spring_security_check') != -1) {
          // looks like we have the login page returned to us
          return true;
        }
        return false;
      },

      /**
       * Prompts the user to relog in if they're within PUC, otherwise displays a dialog
       * indicating their session has expired.
       *
       * @return true if the session has timed out
       */
      handleSessionTimeout: function(args) {
        var callback = function() {
          this.fetchParameterDefinition.apply(this, args);
        }.bind(this);

        this.reauthenticate(callback);
      },

      reauthenticate: function(f) {
        if(top.mantle_initialized) {
          var callback = {
            loginCallback : f
          }
          window.parent.authenticate(callback);
        } else {
          this.showMessageBox(
            pentaho.common.Messages.getString('SessionExpiredComment'),
            pentaho.common.Messages.getString('SessionExpired'),
            pentaho.common.Messages.getString('OK'),
            undefined,
            undefined,
            undefined,
            true
          );
        }
      },

      /**
       * @private Sequence number to detect concurrent fetchParameterDefinition calls.
       * Only the response to the last call will be processed.
       */
      _fetchParamDefId: -1,

      /**
       * Loads the parameter xml definition from the server.
       * @param promptPanel panel to fetch parameter definition for
       * @param {function} callback function to call when successful.
       * The callback signature is:
       * <pre>void function(newParamDef)</pre>
       *  and is called in the context of the report viewer prompt instance.
       * @param {string} [promptMode='MANUAL'] the prompt mode to request from server: 
       *  {INITIAL, MANUAL, USERINPUT}.
       * If not provided, 'MANUAL' will be used.
       */
      fetchParameterDefinition: function(promptPanel, callback, promptMode) {
        var me = this;
        
        var fetchParamDefId = ++me._fetchParamDefId;

        if(!promptMode) { promptMode = 'MANUAL'; }

        if(me.clicking) {
          // If "Upgrading" a Change to a Submit we do not want to process the next Submit Click, if any
          var upgrade = (promptMode === 'USERINPUT');

          me.ignoreNextClickSubmit = upgrade;

          // Also, force the Change to behave as if AutoSubmit was on!
          if(promptPanel) { promptPanel.forceAutoSubmit = upgrade; }

          delete me.clicking;
        }
        
        // Store mode so we can check if we need to refresh the report content or not in the view
        // As only the last request's response is processed, the last value of mode is actually the correct one.
        me.mode = promptMode;
        
        // -------------
        var options = util.getUrlParameters();
        
        // If we aren't passed a prompt panel this is the first request
        if(promptPanel) {
          $.extend(options, promptPanel.getParameterValues());
        }
        options['renderMode'] = this._getParameterDefinitionRenderMode(promptPanel, promptMode);

        // Never send the session back. This is generated by the server.
        delete options['::session'];
        // -------------
        
        var args = arguments;
        
        var onSuccess = logged('fetchParameterDefinition_success', function(xmlString) {
          if(me.checkSessionTimeout(xmlString, args)) { return; }

          // Another request was made after this one, so this one is ignored.
          if(fetchParamDefId !== me._fetchParamDefId) { return; }

          try {
            var newParamDefn = me.parseParameterDefinition(xmlString);

            // A first request is made,
            // With promptMode='INITIAL' and renderMode='PARAMETER'.
            //
            // The response will not have page count information (pagination was not performed),
            // but simply information about the prompt parameters (newParamDef).
            // 
            // When newParamDefn.allowAutoSubmit() is true, 
            // And no validation errors/required parameters exist to be specified, TODO: Don't think that this is being checked here!
            // Then a second request is made, 
            // With promptMode='MANUAL' and renderMode='XML' is performed.
            // 
            // When the response to the second request arrives,
            // Then the prompt panel is rendered, including with page count information,
            // And  the report content is loaded and shown.
            if(promptMode === 'INITIAL' && newParamDefn.allowAutoSubmit()) {
              // assert promptPanel == null
              me.fetchParameterDefinition(/*promptPanel*/null, callback, /*promptMode*/'MANUAL');
              return;
            }

            // Make sure we retain the current auto-submit setting
            //  pp.getAutoSubmitSetting -> pp.autoSubmit, which is updated by the check-box
            var autoSubmit = promptPanel && promptPanel.getAutoSubmitSetting();
            if(autoSubmit != null) {
              newParamDefn.autoSubmitUI = autoSubmit;
            }

            callback.call(me, newParamDefn);
          } catch (e) {
            me.onFatalError(e);
          }
        });
        
        var onError = function(e) {
          if (!me.checkSessionTimeout(e, args)) {
            me.onFatalError(e);
          }
        };
        
        $.ajax({
          async:   true,
          traditional: true, // Controls internal use of $.param() to serialize data to the url/body.
          cache:   false,
          type:    'POST',
          url:     me.getParameterUrl(),
          data:    options,
          dataType:'text',
          success: onSuccess,
          error:   onError
        });
      },
      
      _getParameterDefinitionRenderMode: function(promptPanel, promptMode) {
        switch(promptMode) {
          case 'INITIAL':
              return 'PARAMETER';
              
          case 'USERINPUT':
            if (!promptPanel || !promptPanel.getAutoSubmitSetting()) {
              return 'PARAMETER';
            }
            break;
        }
        
        return 'XML';
      },
      
      getParameterUrl: function() {
        return 'parameter';
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
            button1Text = pentaho.common.Messages.getString('OK');
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
      },

      /**
       * Called when there is a fatal error during parameter definition fetching/parsing
       *
       * @param e Error/exception encountered
       */
      onFatalError: function(e) {
        var errorMsg = pentaho.common.Messages.getString('ErrorParsingParamXmlMessage');
        if (typeof console !== 'undefined' && console.log) {
          console.log(errorMsg + ": " + e);
        }
        this.showMessageBox(
          errorMsg,
          pentaho.common.Messages.getString('FatalErrorTitle'));
      }
    }); // return logged
  }; // return function
});