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
