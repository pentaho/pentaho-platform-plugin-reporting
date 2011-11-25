/**
 * The Pentaho proprietary code is licensed under the terms and conditions
 * of the software license agreement entered into between the entity licensing
 * such code and Pentaho Corporation. 
 */

// Determine the size of an object (# of properties)
var sizeOf = function(obj) {
  var size = 0, key;
  for (key in obj) {
    if (obj.hasOwnProperty(key)) size++;
  }
  return size;
};

doh.register("Report Viewer Tests", [
  {
    name: "1. Test Report Viewer Object",
    runTest: function() {
      doh.assertTrue(pentaho.reporting.Viewer);
      doh.assertTrue(pentaho.reporting.Prompt);
    }
  },
  {
    name: "2. Handle Session Timeout",
    runTest: function() {
      var prompting = new pentaho.reporting.Prompt();
      doh.assertFalse(prompting.isSessionTimeoutResponse("ok response"));
      doh.assertTrue(prompting.isSessionTimeoutResponse("j_spring_security_check"));
      var handled = false;
      prompting.handleSessionTimeout = function() {
        handled = true;
      }

      prompting.checkSessionTimeout("ok response");
      doh.assertFalse(handled);

      prompting.checkSessionTimeout("j_spring_security_check");
      doh.assertTrue(handled);
    }
  },
  {
    name: "3. Create required JavaScript hooks",
    runTest: function() {
      doh.assertFalse(window.reportViewer_openUrlInDialog);
      doh.assertFalse(window.reportViewer_hide);
      var prompt = new pentaho.reporting.Prompt();
      var viewer = new pentaho.reporting.Viewer(prompt);
      viewer.createRequiredHooks();
      doh.assertTrue(window.reportViewer_openUrlInDialog);
      doh.assertTrue(window.reportViewer_hide);
    }
  }
]);