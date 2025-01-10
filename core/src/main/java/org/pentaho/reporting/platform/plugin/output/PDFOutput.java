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

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.IOException;
import java.io.OutputStream;

public class PDFOutput implements ReportOutputHandler {

  public PDFOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  private PageableReportProcessor createProcessor( final MasterReport report, final int yieldRate,
                                                   final OutputStream outputStream ) throws ReportProcessingException {
    final PdfOutputProcessor outputProcessor = new PdfOutputProcessor( report.getConfiguration(), outputStream );
    final PageableReportProcessor proc = new PageableReportProcessor( report, outputProcessor );
    if ( yieldRate > 0 ) {
      proc.addReportProgressListener( getYieldListener( yieldRate ) );
    }
    return proc;
  }

  public int paginate( MasterReport report, int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException {
    return 0;
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException {
    OutputUtils.enforceQueryLimit( report );
    final PageableReportProcessor proc = createProcessor( report, yieldRate, outputStream );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    doProcess( listener, proc );
    return 0;
  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {
  }
}
