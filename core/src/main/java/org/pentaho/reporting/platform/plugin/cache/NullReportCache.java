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


package org.pentaho.reporting.platform.plugin.cache;

import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

public class NullReportCache implements ReportCache {
  public NullReportCache() {
  }

  public ReportOutputHandler get( final ReportCacheKey key ) {
    return null;
  }

  public ReportOutputHandler put( final ReportCacheKey key, final ReportOutputHandler report ) {
    return report;
  }
}
