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

({
  appDir: "./module-scripts",
  optimize: "uglify",
  baseUrl: ".",
  dir: "../bin/scriptOutput",
  optimizeCss: "none",

  skipDirOptimize: true,
  //Put in a mapping so that 'requireLib' in the
  //modules section below will refer to the require.js
  //contents.
  paths: {
    requireLib: 'require',
    'dojo/selector/_loader' : "empty:",
    'dojo/query': 'empty:',
    'dojo/request': 'empty:',
    'dijit/layout/ContentPane': 'empty:',
    'dijit/Dialog' : 'empty:',
    'dojo/text' : 'common-ui/util/text',
    'dijit/layout/StackController': 'empty:',
    'dijit/layout/StackContainer': 'empty:',
    'dijit/layout/TabController': 'empty:',
    'dijit/form/ValidationTextBox':'empty:',
    'dijit/form/_ComboBoxMenuMixin':'empty:',
    'dijit/_TemplatedMixin':'empty:',
    'dijit/form/Select':'empty:',
    'dijit/ColorPalette':'empty:',
    'dojo/date/locale':'empty:',
    'pir/i18n' : 'empty:',
    'dojox/widget/ColorPicker':'empty:',
    'dojo/number':'empty:',
    'reportviewer/formatter' : 'empty:'
  },


  mainConfigFile: 'requireCfg.js',

  //Indicates the namespace to use for require/requirejs/define.
  namespace: "pen",

  modules: [
    {
      name: "reportviewer/reportviewer-main-module",
      include: ["reportviewer/reportviewer-main-module"],
      create: true
    },
    {
      name: "reportviewer/reportviewer-app",
      include: ["reportviewer/reportviewer-app"],
      create: true
    }
  ]
})
