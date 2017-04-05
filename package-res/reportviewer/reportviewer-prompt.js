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
* Copyright (c) 2002-2017 Pentaho Corporation..  All rights reserved.
*/

define(['common-ui/util/util', 'pentaho/common/Messages', "dijit/registry", 'common-ui/prompting/parameters/ParameterXmlParser', 'common-ui/prompting/PromptPanel'],

    function(util, Messages, registry, ParameterXmlParser, PromptPanel) {
  return function() {
    return logged({
      // The current prompt mode
      mode: 'INITIAL',
      forceUpdateParams: [],
      _oldParameterSet: null,

      load: function() {
        Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
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
        var panel = this.panel = new PromptPanel('promptPanel', paramDefn);

        panel.submit = this.submit.bind(this);
        panel.submitStart = this.submitStart.bind(this);
        panel.ready = this.ready.bind(this);
        panel.onAfterRender = this.onAfterRender.bind(this);

        if (!panel.onParameterChanged) {
          panel.onParameterChanged = {};
        }
        panel.onParameterChanged[''] = this._applyUserInput.bind(this);

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
          // promptPanel === panel
          this.fetchParameterDefinition(promptPanel, callback, /*promptMode*/'USERINPUT');
        }.bind(this);

        // Provide our own i18n function
        panel.getString = Messages.getString;

        this.initPromptPanel();

        this._hideLoadingIndicator();
      },

      /**
       * Called to sync the parameter definition with the latest user input
       * @param {Parameter} param
       * @param value
       * @private
       */
      _applyUserInput: function (name, value, param) {
        if (!param.values || param.values.length < 1) {
          return;
        }
        //Working with a list
        if (param.values.length > 1) {
          //Unify multivalue and singlevalue logic
          if (!$.isArray(value)) {
            value = [value];
          }
          var counter = 0;
          for (var i in param.values) {
            var paramVal = param.values[i];
            if (value.indexOf(paramVal.value) > -1) {
              if (!paramVal.selected) {
                counter++;
              }
            } else {
              if (paramVal.selected) {
                counter++;
              }
            }
          }
          //Ok, we changed something, let's ask for a parameter component refresh
          if (counter > 0) {
            this.forceUpdateParams.push(param);
          }
        } else {
          //Working with a plain parameter
          //Already has a valid value
          if (param.values[0].value === value) {
            return;
          } else {
            //Datepickers require some extra care
            if (param.attributes['parameter-render-type'] === 'datepicker') {
              var parameterValueStr = param.values[0].value;
              //A regular expression for a common timezone format like a +3000
              var tzRegex = /^.*[+-]{1}\d{4}$/;
              //A regular expression for a tricky timezone without like a +09.530 or a +12.7545
              var trickyTimezoneRegex = /(^.*[+-]{1}\d{2})(\.5|\.75)(\d{2}$)/;
              //A parameter timezone hint
              var timezoneHint = param.timezoneHint;


              var processTimezone = function (processingValue) {
                //Is a timezone present in a value?
                var matchesArr = processingValue.match(tzRegex);
                if (matchesArr && matchesArr.length > 0) {
                  //A common timezone format
                  return processingValue;
                }
                matchesArr = processingValue.match(trickyTimezoneRegex);
                if (matchesArr && matchesArr.length === 4) {
                  //A tricky timezone format
                  return matchesArr[1] + matchesArr[3];
                }

                //Timezone is not found in a value, check the hint
                if (timezoneHint) {
                  //Timezone hint is present, apply it
                  return processingValue + timezoneHint.slice(1);
                }

                //Nothing to do here
                return processingValue;
              };

              value = processTimezone(value);
              parameterValueStr = processTimezone(parameterValueStr);

              //Erase seconds and milliseconds, minutes are needed to represent a timezone
              var dtValue = new Date(value);
              dtValue.setMilliseconds(0);
              dtValue.setSeconds(0);

              var dtParamValue = new Date(parameterValueStr);
              dtParamValue.setMilliseconds(0);
              dtParamValue.setSeconds(0);

              if (isNaN(dtValue.getTime()) || isNaN(dtParamValue.getTime())) {
                //Something went wrong we have an invalid date - don't loop the UI anyway
                return;
              }

              if (dtValue.getTime() === dtParamValue.getTime()) {
                //Already has a valid value
                return;
              }

            }
            //Need to update the value and request a parameter component refresh
            param.values[0].value = value;
            this.forceUpdateParams.push(param);
          }
        }
      },

      _hideLoadingIndicator: function() {
        try{
          if (window.top.hideLoadingIndicator) {
            window.top.hideLoadingIndicator();
          } else if (window.parent.hideLoadingIndicator) {
            window.parent.hideLoadingIndicator();
          }
        } catch (ignored) {} // Ignore "Same-origin policy" violation in embedded IFrame
      },

      initPromptPanel: function() {
        this.panel.init();
      },

      showGlassPane: function() {
        // Show glass pane when updating the prompt.
        registry.byId('glassPane').show();
      },

      hideGlassPane: function() {
        registry.byId('glassPane').hide();
      },

      ready: function(promptPanel) {
        this.hideGlassPane();
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

      onAfterRender: function() {
        if (inSchedulerDialog && typeof window.parameterValidityCallback !== 'undefined') {
          var isValid = !this.panel.paramDefn.promptNeeded;
          window.parameterValidityCallback(isValid);
        }
      },

      parameterParser: new ParameterXmlParser(),

      parseParameterDefinition: function(xmlString) {
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
        var isRunningIFrameInSameOrigin = null;
        try {
          var ignoredCheckCanReachOutToParent = window.parent.mantle_initialized;
          isRunningIFrameInSameOrigin = true;
        } catch (ignoredSameOriginPolicyViolation) {
          // IFrame is running embedded in a web page in another domain
          isRunningIFrameInSameOrigin = false;
        }

        if(isRunningIFrameInSameOrigin && top.mantle_initialized) {
          var callback = {
            loginCallback : f
          }
          window.parent.authenticate(callback);
        } else {
          this.showMessageBox(
            Messages.getString('SessionExpiredComment'),
            Messages.getString('SessionExpired'),
            Messages.getString('OK'),
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
       *  x INITIAL   - first time
       *  x MANUAL    - user pressed the submit button (or, when autosubmit, after INITIAL fetch)
       *  x USERINPUT - due to a change + auto-submit
       *
       * If not provided, 'MANUAL' will be used.
       */
      fetchParameterDefinition: function(promptPanel, callback, promptMode) {
        var me = this;

        var fetchParamDefId = ++me._fetchParamDefId;

        var changedParams = me.findChangedParameters();

        me.showGlassPane();

        if(!promptMode) {
          promptMode = 'MANUAL';
        }
        else if (promptMode == 'USERINPUT') {
          // Hide glass pane to prevent user from being blocked from changing his selection
          me.hideGlassPane();
        }

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
            //
            // [PIR-1163] Used 'inSchedulerDialog' variable to make sure that the second request is not sent if it's scheduler dialog.
            // Because the scheduler needs only parameters without full XML.
            if(!inSchedulerDialog && promptMode === 'INITIAL' && newParamDefn.allowAutoSubmit()) {
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

            me._oldParameterSet = me.extractParameterValues(newParamDefn);

            //BISERVER-13170
            for (var i in me.forceUpdateParams) {
              var forced = me.forceUpdateParams[i];
              newParamDefn.mapParameters(function (param) {
                if (forced.name === param.name) {
                  param.forceUpdate = true;
                  if (param.values.length > 1 && JSON.stringify(param.values) === JSON.stringify(forced.values)) {
                    //On 6.1 the flag isn't enought to trigger an update for a list parameter
                    //We reset an old parameter values to trigger a differ here
                    forced.values = [];
                  }
                }
              }, me);
            }
            me.forceUpdateParams = [];

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

        if(changedParams){
          options['changedParameters'] = changedParams;
        }

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
        var messageBox = registry.byId('messageBox');

        messageBox.setTitle(dialogTitle);
        messageBox.setMessage(message);

        if (blocker) {
          messageBox.setButtons([]);
        } else {
          var closeFunc = function() {
            if (this.panel) {
              this.panel.dashboard.hideProgressIndicator();
            }
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
        if (this.panel) {
          this.panel.dashboard.showProgressIndicator();
        }
        messageBox.show();
      },

      findChangedParameters: function() {

        if(!this.panel){
          return;
        }

        var currentParameterSet = this.panel.getParameterValues();

        // compare currentParameterSet with oldParmaeterSet. Return an array of changed parameters. (More than one can change if we use the API).
        var changedParameters = this.compareParameters(this._oldParameterSet, currentParameterSet);
        return changedParameters;
      },


      compareParameters: function(oldParameterSet, currentParameterSet) {
        var changedParameters = [];

        $.each(oldParameterSet, function (i, parameter) {
          if (currentParameterSet.hasOwnProperty(parameter.name)) {
            if(JSON.stringify(parameter.value.toString()) !== JSON.stringify(currentParameterSet[parameter.name].toString())) {
              // add to changed
              changedParameters.push(parameter.name);
            }
          } else if("" != parameter.value) {
            // add to changed
            changedParameters.push(parameter.name);
          }
        });

        for (var parameter in currentParameterSet) {
          if (oldParameterSet && !oldParameterSet.hasOwnProperty(parameter)) {
            changedParameters.push(parameter);
          }
        }

        return changedParameters;
      },


      extractParameterValues: function(paramDefn) {
        var extractedParameters = {};
        $.each(paramDefn.parameterGroups, function (i, group) {
          var parameters = group.parameters;
          for(var i=0; i<parameters.length; i++) {
            if(parameters[i].multiSelect && parameters[i].getSelectedValuesValue().length > 0) {
              extractedParameters[parameters[i].name] = {
                value: parameters[i].getSelectedValuesValue(),
                group: group.name,
                name: parameters[i].name
              };
            } else {
              if(parameters[i].getSelectedValuesValue().length > 0) {
                extractedParameters[parameters[i].name] = {
                  value: parameters[i].getSelectedValuesValue()[0],
                  group: group.name,
                  name: parameters[i].name
                };
              }
            }
          }
        });

        return extractedParameters;
      },


      /**
       * Called when there is a fatal error during parameter definition fetching/parsing
       *
       * @param e Error/exception encountered
       */
      onFatalError: function(e) {
        var errorMsg = Messages.getString('ErrorParsingParamXmlMessage');
        if (typeof console !== 'undefined' && console.log) {
          console.log(errorMsg + ": " + e);
        }
        this.showMessageBox(
          errorMsg,
          Messages.getString('FatalErrorTitle'));
      }
    }); // return logged
  }; // return function
});
