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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.AllPageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.xml.XmlPageOutputProcessor;

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
      proc.addReportProgressListener( new YieldReportListener( yieldRate ) );
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
    if ( proc == null ) {
      proc = createProcessor( report, yieldRate );
    }

    try {
      if ( acceptedPage >= 0 ) {
        final XmlPageOutputProcessor outputProcessor = (XmlPageOutputProcessor) proc.getOutputProcessor();
        outputProcessor.setFlowSelector( new SinglePageFlowSelector( acceptedPage, false ) );
      }
      proxyOutputStream.setParent( outputStream );
      proc.processReport();
      return proc.getPhysicalPageCount();
    } finally {
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
