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

module.exports = function (config) {
    config.set({
        basePath: '${basedir}',

        frameworks: ['jasmine', 'requirejs'],

        files: [
            {pattern: 'target/dependency/**/*.+(js|html|properties)', included: false},

            {pattern: 'target/test-javascript/**/*.js', included: false},
            {pattern: 'src/test/javascript/**/*.js', included: false},
            'src/test/config/karma/context.js',
            'target/requireCfg.js',
            'src/test/config/karma/karma-require-js-cfg.js'
        ],

        reporters: ["mocha"],

        colors: true,

        logLevel: config.LOG_INFO,

        autoWatch: true,

        browsers: ["Chrome"]

    });
};
