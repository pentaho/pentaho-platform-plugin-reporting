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
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlContentItems;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlExportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.validator.ReportStructureValidator;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.IOException;
import java.io.OutputStream;

public class FastStreamHtmlOutput extends StreamHtmlOutput {
  public FastStreamHtmlOutput( final String contentHandlerPattern ) {
    super( contentHandlerPattern );
  }

  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {

    OutputUtils.overrideQueryLimit( report );
    ReportStructureValidator validator = new ReportStructureValidator();
    if ( validator.isValidForFastProcessing( report ) == false ) {
      return super.generate( report, acceptedPage, outputStream, yieldRate );
    }

    FastHtmlContentItems contentItems = computeContentItems( outputStream );
    final FastHtmlExportProcessor reportProcessor = new FastHtmlExportProcessor( report, contentItems );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    doProcess( listener, reportProcessor );
    outputStream.flush();
    return 1;
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
