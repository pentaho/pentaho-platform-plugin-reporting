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