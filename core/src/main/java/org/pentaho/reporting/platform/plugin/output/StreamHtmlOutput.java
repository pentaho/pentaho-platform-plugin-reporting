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
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlContentItems;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.IOException;
import java.io.OutputStream;

public class StreamHtmlOutput extends AbstractHtmlOutput {
  public StreamHtmlOutput() {
  }

  public StreamHtmlOutput( final String contentHandlerPattern ) {
    super( contentHandlerPattern );
  }

  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate )
    throws ReportProcessingException, IOException, ContentIOException {
    FastHtmlContentItems contentItems = computeContentItems( outputStream );
    final HtmlPrinter printer = new AllItemsHtmlPrinter( report.getResourceManager() );
    printer.setContentWriter( contentItems.getContentLocation(), contentItems.getContentNameGenerator() );
    printer.setDataWriter( contentItems.getDataLocation(), contentItems.getDataNameGenerator() );
    printer.setUrlRewriter( contentItems.getUrlRewriter() );

    final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor( report.getConfiguration() );
    outputProcessor.setPrinter( printer );
    final StreamReportProcessor sp = new StreamReportProcessor( report, outputProcessor );

    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    //Add async job listener
    if ( listener != null ) {
      sp.addReportProgressListener( listener );
    }
    doProcess( listener, sp );

    outputStream.flush();
    return 1;
  }

}
