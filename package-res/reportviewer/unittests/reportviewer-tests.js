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
* Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
*/

pen.require(['reportviewer/reportviewer', 'reportviewer/reportviewer-prompt'], function(Viewer, Prompt) {
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
      name: "Test Report Viewer Object",
      runTest: function() {
        doh.assertTrue(Viewer);
        doh.assertTrue(Prompt);
      }
    },
    {
      name: "Handle Session Timeout",
      runTest: function() {
        var prompting = new Prompt();
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
      name: "Create required JavaScript hooks",
      runTest: function() {
        doh.assertFalse(window.reportViewer_openUrlInDialog);
        doh.assertFalse(window.reportViewer_hide);
        var prompt = new Prompt();
        var viewer = new Viewer(prompt);
        viewer.createRequiredHooks();
        doh.assertTrue(window.reportViewer_openUrlInDialog);
        doh.assertTrue(window.reportViewer_hide);
      }
    }
  ]);
});
