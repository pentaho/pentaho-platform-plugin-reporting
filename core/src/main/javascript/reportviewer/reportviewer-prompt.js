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


define([
  "common-ui/util/util",
  "pentaho/common/Messages",
  "dijit/registry",
  "common-ui/prompting/api/PromptingAPI",
  "common-ui/jquery-clean",
  "common-ui/underscore",
  "cdf/dashboard/Utils",
  "dojo/dom-class",
  "common-ui/util/formatting"
], function(util, Messages, registry, PromptingAPI, $, _, Utils, domClass, FormatUtils) {

  var _api =  new PromptingAPI('promptPanel', { isSilent:true } );

  return function() {
    return logged({
      // The current prompt mode
      mode: 'INITIAL',
      _isAsync: null,
      _pollingInterval: 1000,
      _dialogThreshold: 1500,
      _promptForLocation: null,
      _defaultOutputPath: null,
      _isReportHtmlPagebleOutputFormat : null,
      _oldParameterDefinition : null,
      _oldParameterSet : null,
      _defaultValuesMap : null,

      /**
       * Gets the prompt api instance
       *
       * @type string
       * @readonly
       */
      get api () {
        return _api;
      },

      _buildReportContentOptions: function(renderMode) {
        var options = util.getUrlParameters();
        if (this._isParamDefnInitialized()) {
          $.extend(options, this.api.operation.getParameterValues());
        }
        options['renderMode'] = renderMode;

        // Never send the session back. This is generated by the server.
        delete options['::session'];

        return options;
      },

      load: function() {
        Messages.addUrlBundle('reportviewer', CONTEXT_PATH+'i18n?plugin=reporting&name=reportviewer/messages/messages');
      },

      /*
       * Gets a property value from the state object in the Prompting API.
       *
       * @param prop The property which value to fetch.
       *
       * @private
       */
      _getStateProperty: function(prop) {
        if (this._isParamDefnInitialized()) {
          return this.api.operation.state()[prop];
        }
        return null;
      },

      /**
       * Checks if the paramDefn property has already been initialized by using the `_oldParameterDefinition`.
       * We need to do make this verification this way because the getter defined on the promptingAPI throws an
       * exception when the paramDefn is not defined.
       *
       * @private
       */
      _isParamDefnInitialized: function() {
        return this._oldParameterDefinition != null;
      },

      compareParameters: function(oldParameterSet, currentParameterSet) {
        var changedParameters = [];

        $.each(oldParameterSet, function (i, parameter) {
          if (currentParameterSet.hasOwnProperty(parameter.name)) {
            var oldValue = parameter.value.toString();
            var currentValue = currentParameterSet[parameter.name].toString();
            if (parameter.parameterRenderType && parameter.parameterRenderType === 'datepicker') {
              if (!this._compareDatesOnly(oldValue, currentValue, parameter.timezoneHint, this._createFormatter(parameter))) {
                // add to changed
                changedParameters.push(parameter.name);
              }
            } else if(JSON.stringify(oldValue) !== JSON.stringify(currentValue)) {
              // add to changed
              changedParameters.push(parameter.name);
            }
          } else if("" != parameter.value) {
            // add to changed
            changedParameters.push(parameter.name);
          }
        }.bind(this));

        for (var parameter in currentParameterSet) {
          if (oldParameterSet && !oldParameterSet.hasOwnProperty(parameter)) {
            changedParameters.push(parameter);
          }
        }

        return changedParameters;
      },

      extractParameterValues: function(paramDefn) {
        var extractedParameters = {};
        $.each(paramDefn.parameterGroups, function (index, group) {
          var parameters = group.parameters;
          for(var i=0; i<parameters.length; i++) {
            if (parameters[i].getSelectedValuesValue().length > 0) {
              var value;
              if (parameters[i].multiSelect) {
                value = parameters[i].getSelectedValuesValue();
              } else {
                value = parameters[i].getSelectedValuesValue()[0];
              }
              extractedParameters[parameters[i].name] = {
                value: value,
                group: group.name,
                name: parameters[i].name,
                parameterRenderType: parameters[i].attributes['parameter-render-type'],
                timezoneHint: parameters[i].timezoneHint,
                type:parameters[i].type,
                attributes:{
                    'timezone': parameters[i].attributes['timezone'],
                	'data-format': parameters[i].attributes['data-format'],
                	'post-processor-formula': parameters[i].attributes['post-processor-formula']
                }
              };
            }
          }
        });

        return extractedParameters;
      },

      extractDefaultValues: function(paramDefn) {
        var extractedDefaultValues = {};
        $.each(paramDefn.parameterGroups, function (index, group) {
          if("system" != group.name) {
            var parameters = group.parameters;
            for(var i=0; i<parameters.length; i++) {
              if(parameters[i].getSelectedValuesValue().length > 0) {
                extractedDefaultValues[parameters[i].name] = {
                  value: parameters[i].getSelectedValuesValue()
                };
              }
            }
          }
        });

        return extractedDefaultValues;
      },

      findChangedParameters: function() {
        var currentParameterSet = this.api.operation.getParameterValues();

        // compare currentParameterSet with oldParmaeterSet. Return an array of changed parameters. (More than one can change if we use the API).
        var changedParameters = this.compareParameters(this._oldParameterSet, currentParameterSet);
        return changedParameters;
      },

      canSkipParameterChange: function(names) {
        if(this._oldParameterSet["output-target"] && this._oldParameterSet["output-target"].value != this.api.operation.getParameterValues()["output-target"]) {
          // this has to be validated on the server.
          return false;
        }

        for(var i=0; i<names.length; i++) {
          if(this._oldParameterSet.hasOwnProperty(names[i]) && "system" == this._oldParameterSet[names[i]].group && "::session" != names[i]) {
            return false; // must be validated on the server if system parameter changed.
          }
          var param = this._oldParameterDefinition.getParameter(names[i]);
          if (param.attributes["has-downstream-dependent-parameter"] == "true") {
            return false; // must be validated on the server if at least one other parameter is dependent on this changed value.
          }
          if (param.attributes["must-validate-on-server"] == "true") {
            return false; // must be validated on the server if requested by some complex validation rules.
          }
        }
        // finally, if all params pass the test, allow to skip.
        return true;
      },

      updateParameterDefinitionWithNewValues: function(callback, names) {
        var paramDefn = $.extend(true, {}, this._oldParameterDefinition); // clone previous(old) paramDefn
        for(var i=0; i<names.length; i++) {
          /*As far as we don't recieve all parameters from server we can loose
            calculated formula results. Let's handle this case. */
          var untrusted = this.api.operation.getParameterValues()[names[i]];
          if(undefined === untrusted ){
            var p = paramDefn.getParameter(names[i]);
            if(p && p.attributes && (p.attributes['must-validate-on-server'] || p.attributes['parameter-render-type'] === 'textbox')){
              untrusted = p.getSelectedValuesValue();
            }
          }
          this.api.util.validateSingleParameter(paramDefn, names[i], untrusted, this._defaultValuesMap);
        }
        try {
          this.api.util.checkParametersErrors(paramDefn); // if no errors set promptNeeded to false(show report)
          callback(undefined, paramDefn);
        } catch (e) {
          me.onFatalError(e);
        }
      },

      createPromptPanel: function() {
        this.api.operation.render(function(api, callback) {

          var paramDefnCallback = function(xml, parameterDefinition) {
            var paramDefn;
            if(!parameterDefinition) {
              // parse parameter definition from XML-response(server validation)
              paramDefn = this.parseParameterDefinition(xml);
            } else {
              // use updated parameter definition(client validation)
              paramDefn = parameterDefinition;
            }

            if(paramDefn.minimized && this._oldParameterDefinition){

              //Work with clone in order not to affect old definition
              var oldParams = $.extend(true, {}, this._oldParameterDefinition);

              //Replace all changed parameters values
              paramDefn.mapParameters( function (p) {
                oldParams.updateParameterValue(p);
                oldParams.updateParameterAttribute(p);
              });

              //Replace root attributes
              oldParams.errors = paramDefn.errors;
              for (var key in paramDefn) {
                var prop = paramDefn[key];
                if(typeof prop == 'string' || typeof prop == 'number' || typeof prop == 'boolean' ) {
                  oldParams[key] = prop;
                }
              }
              oldParams.minimized = false;
              //Assign updated definition
              paramDefn = oldParams;
            }

            try {
              var outputFormat = paramDefn.getParameter("output-target").getSelectedValuesValue();
              this._isReportHtmlPagebleOutputFormat = outputFormat.indexOf('table/html;page-mode=page') !== -1;
              this.togglePageControl();
            } catch (ignored) {
            }

            // A first request is made with promptMode='INITIAL' and renderMode='PARAMETER'.
            //
            // The response will not have page count information (pagination was not performed), but simply information about the prompt parameters (paramDefn).
            //
            // When paramDefn.allowAutoSubmit() is true,
            // And no validation errors/required parameters exist to be specified, TODO: Don't think that this is being checked here!
            // In case when asynchronous mode is off - then a second request is made with promptMode='MANUAL' and renderMode='XML' is performed.
            //
            // When the response to the second request arrives,
            // Then the prompt panel is rendered, including with page count information, and  the report content is loaded and shown.
            //
            // [PIR-1163] Used 'inSchedulerDialog' variable to make sure that the second request is not sent if it's scheduler dialog.
            // Because the scheduler needs only parameters without full XML.
            if ( (typeof inSchedulerDialog === "undefined" || !inSchedulerDialog) && this.mode === 'INITIAL' && paramDefn.allowAutoSubmit() && !this._isAsync) {
              this.fetchParameterDefinition(paramDefnCallback.bind(this), 'MANUAL');
              return;
            }

            // Make sure we retain the current auto-submit setting
            //  pp.getAutoSubmitSetting -> pp.autoSubmit, which is updated by the check-box
            var autoSubmit = this._getStateProperty('autoSubmit');

            if (autoSubmit != null) {
              paramDefn.autoSubmitUI = autoSubmit;
            }

            if (this._oldParameterDefinition == null) {
              this._defaultValuesMap = this.extractDefaultValues(paramDefn);
            }
            this._oldParameterDefinition = paramDefn;
            this._oldParameterSet = this.extractParameterValues(paramDefn);

            callback(paramDefn);

            this._createPromptPanelFetchCallback(paramDefn);
            this.hideGlassPane();
          };

          if (this._isAsync && this._isParamDefnInitialized()) {
            var names = this.findChangedParameters();
            if (this.canSkipParameterChange(names)) {
              this.updateParameterDefinitionWithNewValues(paramDefnCallback.bind(this), names);
              // we did not contact the server.
              return;
            }

            var needToUpdate = [];
            var oldParams = this._oldParameterDefinition;
            if (oldParams) {
              oldParams.mapParameters( function (p) {
                if (names && names.indexOf(p.name) >= 0) {
                  needToUpdate.push(p.name);
                } else {
                  //Request update for invalid auto-fill parameters
                  if (p.attributes && oldParams.errors[p.name] && "true" === p.attributes['autofill-selection']) {
                    needToUpdate.push(p.name);
                  }
                }
              });
            }
          }
          this.fetchParameterDefinition(paramDefnCallback.bind(this), this.mode, needToUpdate);
        }.bind(this));
        this.api.event.parameterChanged(this._applyUserInput.bind(this));
      },

      _createPromptPanelFetchCallback: _.once(function(paramDefn) {
        this.initPromptPanel();
        this._hideLoadingIndicator();
      }),

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
        this.api.operation.init();
      },

      showGlassPane: function() {
        // Show glass pane when updating the prompt.
        registry.byId('glassPane').show();
      },

      hideGlassPane: function() {
        registry.byId('glassPane').hide();
      },

      togglePageControl: function() {
        var pageControlWidget = registry.byId('pageControl');
        var toolBarSeparatorWidget = registry.byId('toolbar-parameter-separator');
        if (this._isReportHtmlPagebleOutputFormat){
          if (pageControlWidget){
            domClass.remove(pageControlWidget.domNode, "hidden");
          }
          if (toolBarSeparatorWidget){
            domClass.remove(toolBarSeparatorWidget.domNode, "hidden");
          }
        } else {
          if (pageControlWidget){
            domClass.add(pageControlWidget.domNode, "hidden");
          }
          if (toolBarSeparatorWidget){
            domClass.add(toolBarSeparatorWidget.domNode, "hidden");
          }
        }
      },

      parseParameterDefinition: function(xmlString) {
        xmlString = this.removeControlCharacters(xmlString);
        return this.api.util.parseParameterXml(xmlString);
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

        if(isRunningIFrameInSameOrigin) {
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
       * @param {function} callback function to call when successful.
       * The callback signature is:
       * <pre>void function(xmlString)</pre>
       *  and is called in the context of the report viewer prompt instance.
       * @param {string} [promptMode='MANUAL'] the prompt mode to request from server:
       *  x INITIAL   - first time
       *  x MANUAL    - user pressed the submit button (or, when autosubmit, after INITIAL fetch)
       *  x USERINPUT - due to a change + auto-submit
       *
       * If not provided, 'MANUAL' will be used.
       * @param changedParams - list of changed parameters names
       */
      fetchParameterDefinition: function(callback, promptMode, changedParams ) {
        var me = this;

        var fetchParamDefId = ++me._fetchParamDefId;

        me.showGlassPane();

        if (!promptMode) {
          promptMode = 'MANUAL';
        } else if (promptMode == 'USERINPUT') {
          // Hide glass pane to prevent user from being blocked from changing his selection
          me.hideGlassPane();
        }

        var curUrl = window.location.href.split('?')[0];
        if (this._isAsync === null) {
          var asyncConf = pentahoGet(curUrl.substring(0, curUrl.indexOf("/api/repos")) + '/plugin/reporting/api/jobs/config',
            "", null, "application/json");
          if (asyncConf) {
            try {
              asyncConf = JSON.parse(asyncConf);
              this._isAsync = asyncConf.supportAsync;
              this._pollingInterval = asyncConf.pollingIntervalMilliseconds;
              this._dialogThreshold = asyncConf.dialogThresholdMilliseconds;
              //No location prompting if mantle application is unavailable
              var isMantleAvailable = false;
              if (isRunningIFrameInSameOrigin){
                isMantleAvailable = window.top.executeCommand;
              }
              this._promptForLocation = asyncConf.promptForLocation && isMantleAvailable;
              this._defaultOutputPath = asyncConf.defaultOutputPath;
            } catch (ignored){
              //not async
            }
          }
        }

        // Store mode so we can check if we need to refresh the report content or not in the view
        // As only the last request's response is processed, the last value of mode is actually the correct one.
        me.mode = promptMode;

        var options = me._buildReportContentOptions(this._getParameterDefinitionRenderMode(promptMode));

        var args = arguments;

        var onSuccess = logged('fetchParameterDefinition_success', function(xmlString) {
          if (me.checkSessionTimeout(xmlString, args)) { return; }

          // Another request was made after this one, so this one is ignored.
          if (fetchParamDefId !== me._fetchParamDefId) { return; }

          try {
            callback(xmlString);
          } catch (e) {
            me.onFatalError(e);
          } finally {
            me.mode = 'USERINPUT';
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

      _getParameterDefinitionRenderMode: function(promptMode) {
        switch(promptMode) {
          case 'INITIAL':
              return 'PARAMETER';

          case 'USERINPUT':
            if (!this._getStateProperty('autoSubmit') || this._isAsync) {
              return 'PARAMETER';
            }
            break;

          case 'MANUAL':
            if (this._isAsync) {
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

        messageBox.setTitle('<div class="pentaho-reportviewer-errordialogtitle">' + Utils.escapeHtml(dialogTitle) + '</div>');
        messageBox.setMessage('<div class="pentaho-reportviewer-errordialogmessage">' + Utils.escapeHtml(message) + '</div>');

        if (blocker) {
          messageBox.setButtons([]);
        } else {
          var closeFunc = (function() {
            if(!this._isAsync) {
              this.api.ui.hideProgressIndicator();
            } else {
              this.hideGlassPane();
            }
            messageBox.hide.call(messageBox);
          }).bind(this);

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

        if(!this._isAsync) {
          this.api.ui.showProgressIndicator();
        }
        messageBox.show();
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
            if (value.indexOf(paramVal.value) > -1
              //Datepickers require some extra care (this is needed if the values list has a stored default value and
              // keeps it ready and selected=false.)
              // We check against value[0], because in this instance the value will be an array with 1 item in it, the
              // selected date. So we retrieve this selected date value by choosing value[0]
              || (param.attributes['parameter-render-type'] === 'datepicker'
                && this._compareDatesOnly(paramVal.value, value[0], param.timezoneHint, this._createFormatter(param)))) {
              if (!paramVal.selected) {
                paramVal.selected = true;
                counter++;
              }
            } else {
              if (paramVal.selected) {
                paramVal.selected = false;
                counter++;
              }
            }
            //Ok, we changed something, let's ask for a parameter component refresh
            if (counter > 0) {
              param.forceUpdate = true;
            }
          }
        } else {
          //Working with a plain parameter
          if (param.values[0].value === value) {
            //Already has a valid value
            return;
          } else {
            //Datepickers require some extra care
            if (param.attributes['parameter-render-type'] === 'datepicker') {
              if (this._compareDatesOnly(param.values[0].value, value, param.timezoneHint, this._createFormatter(param))) {
                return;
              }
            }
            //Need to update the value and request a parameter component refresh
            param.values[0].value = value;
            param.forceUpdate = true;
          }
        }
      },
	  _createFormatter: function(param){
		  if(param.attributes['data-format'] && param.attributes['post-processor-formula']){
			return {
				paramFormatter: FormatUtils.createFormatter(param, param.attributes['data-format']),
				transportFormatter: FormatUtils.createDataTransportFormatter(param)
			}
		}
	  },
      _compareDatesOnly: function (oldValue, newValue, timezoneHint, formatter) {
          //A regular expression for a common timezone format like a +3000
          var tzRegex = /^.*[+-]{1}\d{4}$/;
          //A regular expression for a tricky timezone without like a +09.530 or a +12.7545
          var trickyTimezoneRegex = /(^.*[+-]{1}\d{2})(\.5|\.75)(\d{2}$)/;

          var processTimezone = function (processingValue) {
            //Is a timezone present in a value?
            var matchesArr = processingValue.match(tzRegex);
            if (matchesArr && matchesArr.length > 0) {
              //A common timezone format
              return util.convertTimezoneToStandardFormat(processingValue);
            }
            matchesArr = processingValue.match(trickyTimezoneRegex);
            if (matchesArr && matchesArr.length === 4) {
              //A tricky timezone format
              return matchesArr[1] + matchesArr[3];
            }

            //Timezone is not found in a value, check the hint
            if (timezoneHint) {
              //Timezone hint will be in the format of "[+|-]xxx[x]"
              //Ex: +600 or +0000 or -0530
              //Although most browsers support this format, the standard for a timezone string should be "+00:00" for example.
              //The wrong format of timezone would not allow to create a new Date object in some browsers (i'm looking at you IE)
              timezoneHint = util.convertTimezoneToStandardFormat(timezoneHint)
              //Timezone hint is present, apply it
              return processingValue + timezoneHint;
            }

            //Nothing to do here
            return processingValue;
          };

          var applyParamFormatter = function(processingValue, formatter){
            //No formatter is present for parameter - do nothing
          	if(!formatter){
          	  return processingValue;
          	}
          	//Transport formatter to convert a Date to/from the internal transport format (ISO-8601) used by Pentaho Reporting Engine
            //and found in parameter xml generated for Report Viewer
          	var transportFormatter = formatter.transportFormatter;
          	//Formatter created based on parameter data-format
          	var paramFormatter = formatter.paramFormatter;

            if(!(transportFormatter && paramFormatter)){
          	   return processingValue;
          	}
          	//Parse date from the internal transport format via transport formatter
          	var date = transportFormatter.parse(processingValue);
          	//Apply the parameter data-format - like it's done on data-picker
          	date = paramFormatter.parse(paramFormatter.format(date));
          	//format to date value via transport formatter
          	return transportFormatter.format(date);
          }

          var isSameDate = function(date1, date2) {
            return date1.getFullYear() === date2.getFullYear() && date1.getMonth() === date2.getMonth() && date1.getDate() === date2.getDate();
          }
          //bring date values to the same parameter data-format for the correct comparison.
          oldValue = applyParamFormatter(oldValue, formatter);
          newValue = applyParamFormatter(newValue, formatter);

          oldValue = processTimezone(oldValue);
          newValue = processTimezone(newValue);

          //get the time from oldValue - example of string in oldValue - 2021-09-13T22:33:50.000+05:00 - so getting 23:33:50
          var value = oldValue.substring(11,19);
          var dtValue = new Date(oldValue);
          //date picker value does not take into account the hours. in order to not miscalculate the timezone lets make the hours the same
          //in the date picker in order to compare them after
          //case the time is different from 00:00:00 replace will not happens it means we already have some hour
          newValue = newValue.replace("00:00:00",value);
          var dtParamValue = new Date(newValue);


          if (isNaN(dtValue.getTime()) || isNaN(dtParamValue.getTime())) {
            //Something went wrong we have an invalid date - don't loop the UI anyway
            return true;
          }

          //compare the dates
          if (isSameDate(dtValue, dtParamValue)) {
            //Already has a valid value
            return true;
          }

          return false;
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
