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

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.IOException;
import java.io.OutputStream;

public interface ReportOutputHandler {
  /**
   * Returns the number of pages in the report.
   *
   * @param report    the report to handle.
   * @param yieldRate the yield rate.
   * @return the number of pages generated. This is ignored if {@link #supportsPagination()} returns false.
   * @throws ReportProcessingException if the report processing fails.
   * @throws IOException               if there is an IO error.
   * @throws ContentIOException        if there is an IO error.
   */
  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException;

  public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException;

  public void close();

  /**
   * Does the output target support pagination?
   */
  public boolean supportsPagination();

  public Object getReportLock();

  /**
   * Default report processing code
   */
  default void doProcess(final IAsyncReportListener listener, final AbstractReportProcessor reportProcessor )
    throws ReportProcessingException {
    if ( listener != null ) {
      try {
        reportProcessor.addReportProgressListener( listener );
        reportProcessor.processReport();
        listener.setIsQueryLimitReached( reportProcessor.isQueryLimitReached() );
      } finally {
        reportProcessor.removeReportProgressListener( listener );
        reportProcessor.close();
      }
    } else {
      try {
        reportProcessor.processReport();
      } finally {
        reportProcessor.close();
      }
    }
  }

  default YieldReportListener getYieldListener( final int yieldRate ) {
    return new YieldReportListener( yieldRate );
  }
}
