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
* Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
*/

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
