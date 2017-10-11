/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xml.XmlTableOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

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
