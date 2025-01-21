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
    report.setQueryLimit( getLimit( report.getQueryLimit() ) );
  }

  private static int getLimit( int reportLimit ) {
    if ( reportLimit > 0 ) {
      return reportLimit;
    } else {
      String exportQueryLimit = PentahoSystem.getSystemSetting( "pentaho-interactive-reporting/settings.xml", "export-query-limit", "-1" );

      int settingsLimit = -1;
      try {
        settingsLimit = Integer.parseInt( exportQueryLimit );
      } catch ( NumberFormatException e ) {
        log.error( "'export-query-limit' defined incorrectly in the 'settings.xml'");
      }

      return settingsLimit;
    }
  }
}
