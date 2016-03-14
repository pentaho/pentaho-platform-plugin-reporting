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
* Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
*/

// Find and inject tests using require
(function() {
  var karma = window.__karma__;

  var tests = [];
  for(var file in karma.files) {
    if((/test\-js.*\-spec\.js$/).test(file)) {
      tests.push(file);
    }
  }

  // relativizing test paths, so relative resources also work fine
  // see https://gist.github.com/thomassuckow/6372324
  for(var i=0; i< tests.length; i++) {
    tests[i] = tests[i].replace(/^\/base\/|\.js$/g, "")
  }

  requireCfg['baseUrl'] = '/base';
  requirejs.config(requireCfg);

  // Ask Require.js to load all test files and start test run
  require(tests, karma.start);
})();
