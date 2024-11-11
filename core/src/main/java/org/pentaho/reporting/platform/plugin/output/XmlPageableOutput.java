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
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.AllPageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.xml.XmlPageOutputProcessor;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.IOException;
import java.io.OutputStream;

public class XmlPageableOutput implements ReportOutputHandler {
  private PageableReportProcessor proc;
  private ProxyOutputStream proxyOutputStream;

  public XmlPageableOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  private PageableReportProcessor createProcessor( final MasterReport report, final int yieldRate )
    throws ReportProcessingException {
    proxyOutputStream = new ProxyOutputStream();
    final XmlPageOutputProcessor outputProcessor =
      new XmlPageOutputProcessor( report.getConfiguration(), proxyOutputStream );

    final PageableReportProcessor proc = new PageableReportProcessor( report, outputProcessor );
    if ( yieldRate > 0 ) {
      proc.addReportProgressListener( getYieldListener( yieldRate ) );
    }
    return proc;
  }

  public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException, IOException {
    if ( proc == null ) {
      proc = createProcessor( report, yieldRate );
    }
    proc.paginate();
    return proc.getPhysicalPageCount();
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException {
    OutputUtils.overrideQueryLimit( report );
    if ( proc == null ) {
      proc = createProcessor( report, yieldRate );
    }

    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    //Add async job listener
    if ( listener != null ) {
      proc.addReportProgressListener( listener );
    }
    try {
      if ( acceptedPage >= 0 ) {
        final XmlPageOutputProcessor outputProcessor = (XmlPageOutputProcessor) proc.getOutputProcessor();
        outputProcessor.setFlowSelector( new SinglePageFlowSelector( acceptedPage, false ) );
      }
      proxyOutputStream.setParent( outputStream );
      proc.processReport();
      if ( listener != null ) {
        listener.setIsQueryLimitReached( proc.isQueryLimitReached() );
      }
      return proc.getPhysicalPageCount();
    } finally {
      if ( listener != null ) {
        proc.removeReportProgressListener( listener );
      }
      if ( acceptedPage >= 0 ) {
        final XmlPageOutputProcessor outputProcessor = (XmlPageOutputProcessor) proc.getOutputProcessor();
        outputProcessor.setFlowSelector( new AllPageFlowSelector() );
      }
      if ( proxyOutputStream != null ) {
        proxyOutputStream.setParent( null );
      }
    }
  }

  public boolean supportsPagination() {
    return true;
  }

  public void close() {
    if ( proc != null ) {
      proc.close();
      proc = null;
      proxyOutputStream = null;
    }

  }
}
