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



load("${project.build.directory}/requireCfg.js");
requireCfg.paths["reportviewer"] = "reportviewer";
requireCfg.paths["pentaho/reportviewer"] = "reportviewer/dojo/pentaho/reportviewer";
var json = JSON.stringify(requireCfg);
var output = "require.config(" + json + ");\nrequire(requireCfg);";
out = new java.io.FileWriter("${project.build.directory}/requireCfg.js");

// Write the code to the file
out.write(output, 0, output.length);
out.flush();
out.close();
