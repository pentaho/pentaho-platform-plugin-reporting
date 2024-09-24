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
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.base.util.IOUtils;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class XLSOutput implements ReportOutputHandler {
  private byte[] templateData;
  private ProxyOutputStream proxyOutputStream;

  public XLSOutput() {
  }

  public void setTemplateDataFromStream( final InputStream templateInputStream ) throws IOException {
    if ( templateInputStream != null ) {
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try {
        IOUtils.getInstance().copyStreams( templateInputStream, bout );
      } finally {
        templateInputStream.close();
      }
      templateData = bout.toByteArray();
    } else {
      templateData = null;
    }
  }

  public byte[] getTemplateData() {
    return templateData;
  }

  public void setTemplateData( final byte[] templateData ) {
    this.templateData = templateData;
  }

  public Object getReportLock() {
    return this;
  }

  private FlowReportProcessor createProcessor( final MasterReport report, final int yieldRate )
    throws ReportProcessingException {
    proxyOutputStream = new ProxyOutputStream();
    final FlowExcelOutputProcessor target =
      new FlowExcelOutputProcessor( report.getConfiguration(), proxyOutputStream, report.getResourceManager() );
    target.setUseXlsxFormat( false );
    final FlowReportProcessor reportProcessor = new FlowReportProcessor( report, target );

    if ( yieldRate > 0 ) {
      reportProcessor.addReportProgressListener( getYieldListener( yieldRate ) );
    }
    return reportProcessor;
  }

  public int paginate( MasterReport report, int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException {
    return 0;
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException {
    final FlowReportProcessor reportProcessor = createProcessor( report, yieldRate );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    //Add async job listener
    if ( listener != null ) {
      reportProcessor.addReportProgressListener( listener );
    }
    try {
      proxyOutputStream.setParent( outputStream );
      if ( templateData != null ) {
        final FlowExcelOutputProcessor target = (FlowExcelOutputProcessor) reportProcessor.getOutputProcessor();
        target.setTemplateInputStream( new ByteArrayInputStream( templateData ) );
      }

      reportProcessor.processReport();
      if ( listener != null ) {
        listener.setIsQueryLimitReached( reportProcessor.isQueryLimitReached() );
      }
      outputStream.flush();
      return 0;
    } finally {
      if ( listener != null ) {
        reportProcessor.removeReportProgressListener( listener );
      }
      reportProcessor.close();
      proxyOutputStream.setParent( null );
      final FlowExcelOutputProcessor target = (FlowExcelOutputProcessor) reportProcessor.getOutputProcessor();
      target.setTemplateInputStream( null );
    }

  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {
  }
}
