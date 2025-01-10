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
package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;

public final class OutputUtils {

  private OutputUtils() {
  }

  static final Log log = LogFactory.getLog( PDFOutput.class );

  public static void enforceQueryLimit( MasterReport report ) {
    report.setQueryLimit( getPropertyValue( "export-query-limit", report.getQueryLimit() ) );
  }

  private static int getPropertyValue( String propName, int reportLimit ) {
    String setting = PentahoSystem.getSystemSetting( "pentaho-interactive-reporting/settings.xml", propName, "-1" );

    int settingsLimit = -1;
    try {
      settingsLimit = Integer.parseInt( setting );
    } catch ( NumberFormatException e ) {
      log.error( "'" + propName + "' defined incorrectly in the 'settings.xml'");
    }

    return getMinLimit(reportLimit, settingsLimit);
  }

  private static int getMinLimit ( int a, int b ) {
    // Return -1 when both are "no limit"
    if (a <= 0 && b <= 0) {
      return -1;
    }

    // Return one if the other is "no limit"
    if (a <= 0) {
      return b;
    }
    if (b <= 0) {
      return a;
    }

    // Return the minimum when both are positive
    return Math.min(a, b);
  }
}
