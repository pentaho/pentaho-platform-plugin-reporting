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


module.exports = function (config) {
    config.set({
        basePath: '',

        frameworks: ['jasmine', 'requirejs'],

        // list of files / patterns to load in the browser
        files: [
            'context.js',
            "**/*-require-js-cfg.js",
            'karma-require-js-cfg.js',
            {pattern: '*.+(js|min.js|xml|properties|html|css|png|gif)', included: false, watched: false},
            {pattern: '**/*.+(js|min.js|xml|properties|html|css|png|gif)', included: false, watched: false}
        ],

        reporters: ["mocha"],

        colors: true,

        // level of logging
        // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
        logLevel: config.LOG_INFO,

        // enable / disable watching file and executing tests whenever any file changes
        autoWatch: true,

        // The configuration setting tells Karma how long to wait (in milliseconds) after any changes have occurred before starting the test process again.
        autoWatchBatchDelay: 250,

        browsers: ["Chrome"],

        // If browser does not capture in given timeout [ms], kill it
        captureTimeout: 600000,

        browserNoActivityTimeout: 600000,

        // Continuous Integration mode
        // if true, it capture browsers, run tests and exit
        singleRun: false

    });
};
