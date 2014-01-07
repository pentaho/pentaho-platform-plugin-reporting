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

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
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
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;

public class StreamJcrHtmlOutput implements ReportOutputHandler {

  private String jcrOutputPath;
  private String contentHandlerPattern;

  public StreamJcrHtmlOutput()
  {
  }

  public void setContentHandlerPattern(final String pattern)
  {
    this.contentHandlerPattern = pattern;
  }

  public String getContentHandlerPattern()
  {
    return contentHandlerPattern;
  }

  public String getJcrOutputPath() {
    return jcrOutputPath;
  }

  public void setJcrOutputPath( String jcrOutputPath ) {
    this.jcrOutputPath = jcrOutputPath;
  }

  public Object getReportLock() {
    return this;
  }

  public boolean isSafeToDelete()
  {
    return "true".equals( ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty(
        "org.pentaho.reporting.platform.plugin.AlwaysDeleteHtmlDataFiles" ) );
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
      final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    final RepositoryFile outputFolder = repo.getFile( jcrOutputPath );

    final ReportContentRepository repository = new ReportContentRepository( outputFolder );
    final ContentLocation dataLocation = repository.getRoot();
    final PentahoNameGenerator dataNameGenerator = PentahoSystem.get( PentahoNameGenerator.class );
    if ( dataNameGenerator == null ) {
      throw new IllegalStateException( Messages.getInstance().getString(
          "ReportPlugin.errorNameGeneratorMissingConfiguration" ) );
    }
    dataNameGenerator.initialize( dataLocation, isSafeToDelete() );
    final URLRewriter rewriter = new PentahoURLRewriter( getContentHandlerPattern(), true );

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
    return 0;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    return 0;
  }

  public void close()
  {

  }

  public boolean supportsPagination()
  {
    return false;
  }
}
