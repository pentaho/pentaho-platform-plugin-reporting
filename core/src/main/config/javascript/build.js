/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


({
  //The top level directory that contains your app. If this option is used
  //then it assumed your scripts are in a subdirectory under this path.
  //If this option is specified, then all the files from the app directory
  //will be copied to the dir: output area, and baseUrl will assume to be
  //a relative path under this directory.
  appDir: "${project.build.directory}/src-javascript",

  // Disable the included UglifyJS that only understands ES5 or earlier syntax.
  // If the source uses ES2015 or later syntax, pass "optimize: 'none'" to r.js
  // and use an ES2015+ compatible minifier after running r.js.
  optimize: "none",

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

  buildCSS: false,
  optimizeCss: "none",

  skipDirOptimize: true,
  //Put in a mapping so that 'requireLib' in the
  //modules section below will refer to the require.js
  //contents.
  paths: {
    requireLib: 'require',
    'dojo/text': 'common-ui/util/text',
    'pir/i18n': 'empty:',
    'reportviewer/formatter': 'empty:',
    'pentaho/environment': 'empty:'
  },

  mainConfigFile: '${project.build.directory}/requireCfg.js',

  // Runtime Bundles Configuration
  // ----
  // Use the following option with a r.js of version >= 2.2.0 to
  // automatically generate the RequireJS `bundles` configuration.
  // Currently, this requires to manually copy the output of this file into
  //   "src/main/javascript/scripts/reporting-require-js-bundles-cfg.js".

  bundlesConfigOutFile: "${project.build.directory}/requireCfg.bundles.js",

  // Do not write a build.txt file in the output folder.
  // Requires r.js >= 2.2.0.
  writeBuildTxt: false,

  onBuildWrite(moduleName, path, contents) {
    // check value to see if code should be minified/uglified
    if ("${js.build.optimizer}" === "none") {
      return contents;
    }

    const { minify } = require.nodeRequire("uglify-js");
    const { code, error } = minify(contents, {
      output: {
        beautify: false
      }
    });

    if (error) {
      throw new Error(error);
    }

    return code;
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

  modules: [
    {
      // this bundle is for external consumption; currently used by pir
      name: "reportviewer/reportviewer-main-module",
      include: ["reportviewer/reportviewer-main-module"],
      exclude: [
        "css",
        "amd",
        "text"
      ],
      create: true
    },
    {
      // this bundle is used internally; it setups and launches the report viewer application
      name: "reportviewer/reportviewer-app",
      include: ["reportviewer/reportviewer-app"],
      //exclude css otherwise we will not be able to use them without load issues
      //see http://jira.pentaho.com/browse/PRD-5915
      exclude: [
        "css",
        "amd",
        "text"
      ],
      create: true
    }
  ]
})
