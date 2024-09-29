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


var isRunningIFrameInSameOrigin = true;
var SESSION_NAME = "dummy";
var SESSION_LOCALE = "en-US";
var CONTEXT_PATH = "/pentaho/";
var allTestFiles = [];
var TEST_REGEXP = /reportviewer.*(spec|test)\.js$/i;

// Get a list of all the test files to include
Object.keys(window.__karma__.files).forEach(function (file) {
    if (TEST_REGEXP.test(file)) {
        // Normalize paths to RequireJS module names.
        // If you require sub-dependencies of test files to be loaded as-is (requiring file extension)
        // then do not normalize the paths
        var normalizedTestModule = file.replace(/^\/base\/|\.js$/g, '');
        allTestFiles.push(normalizedTestModule);
    }
});

requireCfg["baseUrl"] = '/base';
requireCfg["deps"] = allTestFiles;
requireCfg["callback"] = window.__karma__.start;

require.config(requireCfg);
