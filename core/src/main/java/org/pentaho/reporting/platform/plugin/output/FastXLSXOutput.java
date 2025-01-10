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
import org.pentaho.reporting.engine.classic.core.modules.output.fast.validator.ReportStructureValidator;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.xls.FastExcelExportProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;


import java.io.IOException;
import java.io.OutputStream;

public class FastXLSXOutput extends XLSXOutput {
  private ProxyOutputStream proxyOutputStream;



  public FastXLSXOutput() {
    proxyOutputStream = new ProxyOutputStream();
  }

  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException {
    proxyOutputStream.setParent( outputStream );
    OutputUtils.enforceQueryLimit( report );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    ReportStructureValidator validator = new ReportStructureValidator();
    if ( validator.isValidForFastProcessing( report ) == false ) {
      return super.generate( report, acceptedPage, outputStream, yieldRate );
    }

    final FastExcelExportProcessor reportProcessor = new FastExcelExportProcessor( report, outputStream, true );

    doProcess( listener, reportProcessor );
    outputStream.flush();
    return 0;
  }

  // Functionality requested by BISERVER-14865
  private void overrideQueryLimit( MasterReport report ) {
    report.setQueryLimit( getPropertyValue( "excel-query-limit", "-1" ) );
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
