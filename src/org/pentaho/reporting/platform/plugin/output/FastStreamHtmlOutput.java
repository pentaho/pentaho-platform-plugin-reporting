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
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlContentItems;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.html.FastHtmlExportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.validator.ReportStructureValidator;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

public class FastStreamHtmlOutput extends StreamHtmlOutput {
  public FastStreamHtmlOutput( final String contentHandlerPattern ) {
    super( contentHandlerPattern );
  }

  public int generate( final MasterReport report,
                       final int acceptedPage,
                       final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {

    ReportStructureValidator validator = new ReportStructureValidator();
    if ( validator.isValidForFastProcessing( report ) == false ) {
      return super.generate( report, acceptedPage, outputStream, yieldRate );
    }

    FastHtmlContentItems contentItems = computeContentItems( outputStream );
    final FastHtmlExportProcessor reportProcessor = new FastHtmlExportProcessor( report, contentItems );
    final ReportProgressListener listener = ReportListenerThreadHolder.getListener();
    //Add async job listener
    if ( listener != null ) {
      reportProcessor.addReportProgressListener( listener );
    }
    try {
      reportProcessor.processReport();
    } finally {
      if ( listener != null ) {
        reportProcessor.removeReportProgressListener( listener );
      }
      reportProcessor.close();
    }

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
