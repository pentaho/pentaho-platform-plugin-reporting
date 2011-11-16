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
      doh.assertTrue(ReportViewer);
    }
  },
  {
    name: "2. Handle Session Timeout",
    runTest: function() {
      doh.assertFalse(ReportViewer.isLoginPageContent("ok response"));
      doh.assertTrue(ReportViewer.isLoginPageContent("j_spring_security_check"));

      // Prevent any attempts to reauthenticate
      ReportViewer.reauthenticate = function() {}
      doh.assertFalse(ReportViewer.handleSessionTimeout("ok response"));
      doh.assertTrue(ReportViewer.handleSessionTimeout("j_spring_security_check"));
    }
  },
  {
    name: "3. Create required JavaScript hooks",
    runTest: function() {
      doh.assertFalse(window.reportViewer_openUrlInDialog);
      doh.assertFalse(window.reportViewer_hide);
      ReportViewer.createRequiredHooks();
      doh.assertTrue(window.reportViewer_openUrlInDialog);
      doh.assertTrue(window.reportViewer_hide);
    }
  }
]);