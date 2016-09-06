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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.validator.ReportStructureValidator;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.xls.FastExcelExportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.IOException;
import java.io.OutputStream;

public class FastXLSOutput implements ReportOutputHandler {
  private ProxyOutputStream proxyOutputStream;

  public FastXLSOutput() {
    proxyOutputStream = new ProxyOutputStream();
  }

  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    proxyOutputStream.setParent( outputStream );
    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    ReportStructureValidator validator = new ReportStructureValidator();
    if ( validator.isValidForFastProcessing( report ) == false ) {
      createXLS( report, outputStream, listener );
      return 0;
    }

    final FastExcelExportProcessor reportProcessor = new FastExcelExportProcessor( report, outputStream, false );
    if ( listener != null ) {
      reportProcessor.addReportProgressListener( listener );
    }

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

  public static void createXLS( final MasterReport report, final OutputStream outputStream,
                                final IAsyncReportListener listener )
          throws ReportProcessingException, IOException {
    if ( report == null ) {
      throw new NullPointerException();
    }
    if ( outputStream == null ) {
      throw new NullPointerException();
    }

    final FlowExcelOutputProcessor target =
            new FlowExcelOutputProcessor( report.getConfiguration(), outputStream, report.getResourceManager() );
    target.setUseXlsxFormat( false );
    final FlowReportProcessor reportProcessor = new FlowReportProcessor( report, target );
    if ( listener != null ) {
      reportProcessor.addReportProgressListener( listener );
    }
    doProcess( listener, reportProcessor );
    outputStream.flush();
  }

  private static void doProcess( final IAsyncReportListener listener, final AbstractReportProcessor reportProcessor )
          throws ReportProcessingException {
    try {
      reportProcessor.processReport();
      if ( listener != null ) {
        listener.setIsQueryLimitReached( reportProcessor.isQueryLimitReached() );
      }
    } finally {
      if ( listener != null ) {
        reportProcessor.removeReportProgressListener( listener );
      }
      reportProcessor.close();
    }
  }

}
