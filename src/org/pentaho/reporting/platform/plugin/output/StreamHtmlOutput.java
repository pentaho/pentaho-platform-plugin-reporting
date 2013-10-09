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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.URLRewriter;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.file.FileRepository;
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;

public class StreamHtmlOutput implements ReportOutputHandler {
  private String contentHandlerPattern;

  public StreamHtmlOutput( final String contentHandlerPattern ) {
    this.contentHandlerPattern = contentHandlerPattern;
  }

  public Object getReportLock() {
    return this;
  }

  public String getContentHandlerPattern() {
    return contentHandlerPattern;
  }

  public boolean supportsPagination() {
    return false;
  }

  public void close() {

  }

  protected boolean isSafeToDelete() {
    return "true".equals( ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty(
        "org.pentaho.reporting.platform.plugin.AlwaysDeleteHtmlDataFiles" ) );
  }

  public int paginate( MasterReport report, int yieldRate ) throws ReportProcessingException, IOException,
    ContentIOException {
    return 1;
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
      final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();

    final URLRewriter rewriter;
    final ContentLocation dataLocation;
    final PentahoNameGenerator dataNameGenerator;
    if ( ctx != null ) {
      File dataDirectory = new File( ctx.getFileOutputPath( "system/tmp/" ) ); //$NON-NLS-1$
      if ( dataDirectory.exists() && ( dataDirectory.isDirectory() == false ) ) {
        dataDirectory = dataDirectory.getParentFile();
        if ( dataDirectory.isDirectory() == false ) {
          throw new ReportProcessingException( "Dead " + dataDirectory.getPath() ); //$NON-NLS-1$
        }
      } else if ( dataDirectory.exists() == false ) {
        dataDirectory.mkdirs();
      }

      final FileRepository dataRepository = new FileRepository( dataDirectory );
      dataLocation = dataRepository.getRoot();
      dataNameGenerator = PentahoSystem.get( PentahoNameGenerator.class );
      if ( dataNameGenerator == null ) {
        throw new IllegalStateException( Messages.getInstance().getString(
            "ReportPlugin.errorNameGeneratorMissingConfiguration" ) );
      }
      dataNameGenerator.initialize( dataLocation, isSafeToDelete() );
      rewriter = new PentahoURLRewriter( contentHandlerPattern, false );
    } else {
      dataLocation = null;
      dataNameGenerator = null;
      rewriter = new PentahoURLRewriter( contentHandlerPattern, false );
    }

    final StreamRepository targetRepository = new StreamRepository( null, outputStream, "report" ); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor( report.getConfiguration() );
    final HtmlPrinter printer = new AllItemsHtmlPrinter( report.getResourceManager() );
    printer.setContentWriter( targetRoot,
       new DefaultNameGenerator( targetRoot, "index", "html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    printer.setDataWriter( dataLocation, dataNameGenerator );
    printer.setUrlRewriter( rewriter );
    outputProcessor.setPrinter( printer );

    final StreamReportProcessor sp = new StreamReportProcessor( report, outputProcessor );
    if ( yieldRate > 0 ) {
      sp.addReportProgressListener( new YieldReportListener( yieldRate ) );
    }

    try {
      sp.processReport();
    } finally {
      sp.close();
    }

    outputStream.flush();
    return 1;
  }

}
