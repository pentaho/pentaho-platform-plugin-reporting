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
var prefix =
// environment configured
(typeof ENVIRONMENT_CONFIG !== "undefined" && typeof ENVIRONMENT_CONFIG.paths !== "undefined" &&
  typeof ENVIRONMENT_CONFIG.paths["reportviewer"] !== "undefined") ? ENVIRONMENT_CONFIG.paths["reportviewer"] :
// production
(typeof CONTEXT_PATH != "undefined") ? CONTEXT_PATH+'content/reporting/reportviewer' :
// build
'reportviewer';
if(typeof KARMA_RUN !== "undefined" || typeof document == "undefined" || document.location.href.indexOf("debug=true") > 0){
  requireCfg['paths']['reportviewer'] = prefix;
  requireCfg['paths']['pentaho/reportviewer'] = prefix+'/dojo/pentaho/reportviewer';
} else {
  requireCfg['paths']['reportviewer'] = prefix+'/compressed';
  requireCfg['paths']['pentaho/reportviewer'] = prefix+'/dojo/pentaho/reportviewer';
}