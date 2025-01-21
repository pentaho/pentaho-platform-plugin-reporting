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
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xml.XmlTableOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.IOException;
import java.io.OutputStream;

public class XmlTableOutput implements ReportOutputHandler {
  private StreamReportProcessor proc;
  private ProxyOutputStream proxyOutputStream;

  public XmlTableOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  private StreamReportProcessor createProcessor( final MasterReport report, final int yieldRate )
    throws ReportProcessingException {
    proxyOutputStream = new ProxyOutputStream();
    final XmlTableOutputProcessor target = new XmlTableOutputProcessor( proxyOutputStream );
    final StreamReportProcessor proc = new StreamReportProcessor( report, target );

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
    if ( proc == null ) {
      proc = createProcessor( report, yieldRate );
    }

    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    //Add async job listener
    if ( listener != null ) {
      proc.addReportProgressListener( listener );
    }
    try {
      proxyOutputStream.setParent( outputStream );
      proc.processReport();
      if ( listener != null ) {
        listener.setIsQueryLimitReached( proc.isQueryLimitReached() );
      }
      return 0;
    } finally {
      if ( listener != null ) {
        proc.removeReportProgressListener( listener );
      }
      proxyOutputStream.setParent( null );
      outputStream.flush();
    }
  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {
    if ( proc != null ) {
      proc.close();
      proc = null;
      proxyOutputStream = null;
    }

  }
}
