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

({
  //The top level directory that contains your app. If this option is used
  //then it assumed your scripts are in a subdirectory under this path.
  //If this option is specified, then all the files from the app directory
  //will be copied to the dir: output area, and baseUrl will assume to be
  //a relative path under this directory.
  appDir: "${project.build.directory}/src-javascript",

  //How to optimize all the JS files in the build output directory.
  optimize: "${js.build.optimizer}",

  //By default, all modules are located relative to this path. If appDir is set, then
  //baseUrl should be specified as relative to the appDir.
  baseUrl: ".",

  //The directory path to save the output. All relative paths are relative to the build file.
  dir: "${project.build.directory}/build-javascript",

  //As of RequireJS 2.0.2, the dir above will be deleted before the
  //build starts again. If you have a big build and are not doing
  //source transforms with onBuildRead/onBuildWrite, then you can
  //set keepBuildDir to true to keep the previous dir. This allows for
  //faster rebuilds, but it could lead to unexpected errors if the
  //built code is transformed in some way.
  keepBuildDir: false,

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

  mainConfigFile: '${project.build.directory}/requireCfg.js',

  //If using UglifyJS2 for script optimization, these config options can be
  //used to pass configuration values to UglifyJS2.
  //For possible `output` values see:
  //https://github.com/mishoo/UglifyJS2#beautifier-options
  //For possible `compress` values see:
  //https://github.com/mishoo/UglifyJS2#compressor-options
  uglify2: {
    output: {
      max_line_len: 80,
      beautify: false
    },
    warnings: false,
    mangle: true
  },

  //If set to true, any files that were combined into a build bundle will be
  //removed from the output folder.
  removeCombined: true,

  //By default, comments that have a license in them are preserved in the
  //output when a minifier is used in the "optimize" option.
  //However, for a larger built files there could be a lot of
  //comment files that may be better served by having a smaller comment
  //at the top of the file that points to the list of all the licenses.
  //This option will turn off the auto-preservation, but you will need
  //work out how best to surface the license information.
  //NOTE: As of 2.1.7, if using xpcshell to run the optimizer, it cannot
  //parse out comments since its native Reflect parser is used, and does
  //not have the same comments option support as esprima.
  preserveLicenseComments: false,

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
      exclude: [	
        "css!cdf/dashboard/Dashboard.notifications"
        ],
      create: true
    }
  ]
})
