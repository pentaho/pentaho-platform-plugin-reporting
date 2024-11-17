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
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.validator.ReportStructureValidator;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.xls.FastExcelExportProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.IOException;
import java.io.OutputStream;

public class FastXLSOutput extends XLSOutput {
  private ProxyOutputStream proxyOutputStream;

  public FastXLSOutput() {
    proxyOutputStream = new ProxyOutputStream();
  }

  @Override
  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException {
    proxyOutputStream.setParent( outputStream );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    ReportStructureValidator validator = new ReportStructureValidator();
    if ( validator.isValidForFastProcessing( report ) == false ) {
      return super.generate( report, acceptedPage, outputStream, yieldRate );
    }

    final FastExcelExportProcessor reportProcessor = new FastExcelExportProcessor( report, outputStream, false );

    doProcess( listener, reportProcessor );
    outputStream.flush();

    return 0;
  }

  public int paginate( final MasterReport report,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    return 0;
  }

  public void close() {

  }

  public boolean supportsPagination() {
    return false;
  }

  public Object getReportLock() {
    return this;
  }
}
