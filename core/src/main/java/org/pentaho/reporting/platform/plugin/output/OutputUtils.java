package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;

public final class OutputUtils {

  private OutputUtils() {
  }

  static final Log log = LogFactory.getLog( PDFOutput.class );

  public static void overrideQueryLimit( MasterReport report ) {
    report.setQueryLimit( getPropertyValue( "export-query-limit", "-1" ) );
  }

  private static int getPropertyValue( String propName, String def ) {
    String limit = PentahoSystem.getSystemSetting( "pentaho-interactive-reporting/settings.xml", propName, def );
    try {
      int iLimit = Integer.parseInt( limit );
      if ( iLimit <= 0 ) {
        return -1;
      }
      return iLimit;
    } catch ( NumberFormatException e ) {
      log.error( "'" + propName + "' defined incorrectly in the 'settings.xml'. Using default value: " + def );
      return -1;
    }
  }
}
