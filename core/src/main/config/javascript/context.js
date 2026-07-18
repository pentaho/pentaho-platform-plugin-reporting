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
ENVIRONMENT_CONFIG.paths["cdf"] = ENVIRONMENT_CONFIG.paths["cdf"] || "../src-javascript/cdf/js";
ENVIRONMENT_CONFIG.paths["cdf/lib"] = ENVIRONMENT_CONFIG.paths["cdf/lib"] || "../src-javascript/cdf/js/lib";

var requireCfg = {
    paths: {},
    shim: {},
    map: {
        "*": {}
    },
    bundles: {},
    config: {
        "pentaho/modules": {}
    },
    packages: []
};
