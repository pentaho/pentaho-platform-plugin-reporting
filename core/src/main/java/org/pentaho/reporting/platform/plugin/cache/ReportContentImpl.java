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

import java.util.Map;

/**
 * Report representation for atomic cache operations
 */
public class ReportContentImpl implements IReportContent {

  public ReportContentImpl( final int pageCount, final Map<Integer, byte[]> reportData ) {

    this.pageCount = pageCount;
    this.reportData = reportData;
  }

  private final int pageCount;

  private final Map<Integer, byte[]> reportData;

  @Override public int getPageCount() {
    return pageCount;
  }

  @Override public int getStoredPageCount() {
    if ( reportData != null ) {
      return reportData.size();
    }
    return 0;
  }

  @Override public byte[] getPageData( final int page ) {
    return reportData.get( page );
  }

}
