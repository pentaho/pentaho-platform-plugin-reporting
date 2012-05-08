pentaho = typeof pentaho == "undefined" ? {} : pentaho;
pentaho.reporting.SchedulePrompt = function(prompt) {
  return {
    file: undefined, // Name of file to load parameters for
    parameterValidityCallback: undefined, // Function to be called when parameters' validity changes

    createHooks: function() {
      window.initSchedulingParams = this.initSchedulingParams.bind(this);
      window.getParams = this.getParams.bind(this);
    },

    /**
     * Initialize this prompt for the file provided.
     *
     * @param filePath File to fetch parameters for
     * @param validParamsCallback Function to be called when parameters' validity changes
     */
    initSchedulingParams: function(filePath, validParamsCallback) {
      this.file = filePath;
      this.parameterValidityCallback = validParamsCallback;

      // We're not valid until the prompt is refreshed and tells us we are
	if (this.parameterValidityCallback) {
      	this.parameterValidityCallback(false);
	}

      prompt.submit = this.submit.bind(this);
      prompt.getParameterUrl = this.getParameterUrl.bind(this);
      prompt.initPromptPanel = this.initPromptPanel.bind(this);
      prompt.createPromptPanel();
    },

    initPromptPanel: function() {
      prompt.panel.shouldBuildSubmitPanel = function() { return false; };
      prompt.panel.init();
    },

    getParameterUrl: function() {
      return '/pentaho/api/repos/' + encodeURI(this.file.replace(/\//g, ":")) + '/parameter';
    },

    submit: function() {
	var isValid = this.checkParams();
      $('#validFlag').css('background-color', isValid ? 'green' : 'red');
      $('#validFlag').html(isValid ? 'Valid' : 'Invalid');

      $('#parameterValues').html('');
      $('#showparamsbutton').attr('disabled', !isValid);
	if (this.parameterValidityCallback) {
      	this.parameterValidityCallback(isValid);
	}
    },

    /**
     * Check if parameters are valid.
     *
     * @return true if parameters are valid, false otherwise.
     */
    checkParams: function() {
      return prompt.panel && !prompt.panel.paramDefn.promptNeeded;
    },

    /**
     * Get parameter values as an array of name/value pairs.
     *
     * @return array of {name, [value1..valueN]}
     */
    getParams: function() {
      return prompt.panel && prompt.panel.getParameterValues();
    },

    onFatalError: function(e) {
      var errorMsg = Messages.getString('ErrorParsingParamXmlMessage');
      if (console) {
        console.log(errorMsg + ": " + e);
      }
      this.showMessageBox(
        errorMsg,
        Messages.getString('FatalErrorTitle'));
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
  }
};

// function initSchedulingParams(filePath, validParamsCallback) {
//   isValidParams = validParamsCallback;
//   document.getElementById('filePath').innerHTML = filePath;
//   isValidParams (false);
//   lastReportedValue = false;
// }

// function checkParams() {
//   if (!document.getElementById('param').value) {
//     if (lastReportedValue) {
//       isValidParams (false);
//       lastReportedValue = false;
//     }
//   } else {
//     if (!lastReportedValue) {
//       isValidParams (true);
//       lastReportedValue = true;
//     }
//   }
// }

// function getParams() {
//   var params = new Array();
//   params[document.getElementById('param').getAttribute('name')] = document.getElementById('param').value;
//   return params;
// }
