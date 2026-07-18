/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



var ENVIRONMENT_CONFIG = ENVIRONMENT_CONFIG || {};
ENVIRONMENT_CONFIG.paths = ENVIRONMENT_CONFIG.paths || {};
ENVIRONMENT_CONFIG.paths["cdf"] = ENVIRONMENT_CONFIG.paths["cdf"] || "cdf/js";
ENVIRONMENT_CONFIG.paths["cdf/lib"] = ENVIRONMENT_CONFIG.paths["cdf/lib"] || "cdf/js/lib";
var requireCfg = {
    paths: {},
    shim: {},
    map: {
        "*": {
            "pentaho/reportviewer" : "reportviewer/dojo/pentaho/reportviewer"
        }
    },
    bundles: {},
    config: {
        "pentaho/modules": {}
    },
    packages: []
};

