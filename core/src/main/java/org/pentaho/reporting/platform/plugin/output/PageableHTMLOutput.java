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
 * Copyright (c) 2002-2018 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import mondrian.util.Pair;
import org.apache.commons.io.IOUtils;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.DisplayAllFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.PageableHtmlOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.PageSequenceNameGenerator;
import org.pentaho.reporting.libraries.repository.Repository;
import org.pentaho.reporting.libraries.repository.file.FileRepository;
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.libraries.repository.zip.ZipRepository;
import org.pentaho.reporting.platform.plugin.PentahoPlatformModule;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;
import org.pentaho.reporting.platform.plugin.cache.ReportContentImpl;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.PentahoURLRewriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class PageableHTMLOutput extends AbstractHtmlOutput {

  private ProxyOutputStream proxyOutputStream;
  private PageableReportProcessor proc;
  private AllItemsHtmlPrinter printer;

  public PageableHTMLOutput() {
  }

  public Object getReportLock() {
    return this;
  }

  public ProxyOutputStream getProxyOutputStream() {
    return proxyOutputStream;
  }

  public void setProxyOutputStream( final ProxyOutputStream proxyOutputStream ) {
    this.proxyOutputStream = proxyOutputStream;
  }

  public HtmlPrinter getPrinter() {
    return printer;
  }

  public void setPrinter( final AllItemsHtmlPrinter printer ) {
    this.printer = printer;
  }

  public PageableReportProcessor getReportProcessor() {
    return proc;
  }

  public void setReportProcessor( final PageableReportProcessor proc ) {
    this.proc = proc;
  }

  protected PageableReportProcessor createReportProcessor( final MasterReport report, final int yieldRate )
          throws ReportProcessingException {

    proxyOutputStream = new ProxyOutputStream();

    printer = new AllItemsHtmlPrinter( report.getResourceManager() );
    printer.setUrlRewriter( new PentahoURLRewriter( getContentHandlerPattern(), shouldUseContentIdAsName() ) );

    final PageableHtmlOutputProcessor outputProcessor = new PageableHtmlOutputProcessor( report.getConfiguration() );
    outputProcessor.setPrinter( printer );
    proc = new PageableReportProcessor( report, outputProcessor );

    if ( yieldRate > 0 ) {
      proc.addReportProgressListener( getYieldListener( yieldRate ) );
    }

    return proc;
  }

  protected boolean shouldUseContentIdAsName() {
    return false;
  }

  protected void reinitOutputTarget() throws ReportProcessingException, ContentIOException {

    final Pair<ContentLocation, PentahoNameGenerator> pair = reinitLocationAndNameGenerator();

    final StreamRepository targetRepository = new StreamRepository( null, proxyOutputStream, "report" ); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    printer.setContentWriter( targetRoot,
            new DefaultNameGenerator( targetRoot, "index", "html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    printer.setDataWriter( pair.getKey(), pair.getValue() );
  }

  protected Pair<ContentLocation, PentahoNameGenerator> reinitLocationAndNameGenerator()
          throws ReportProcessingException, ContentIOException {
    final IApplicationContext ctx = PentahoSystem.getApplicationContext();

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
      dataNameGenerator.initialize( dataLocation, true );
    } else {
      dataLocation = null;
      dataNameGenerator = null;
    }
    return Pair.of( dataLocation, dataNameGenerator );
  }

  public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException, IOException,
          ContentIOException {
    if ( proc == null ) {
      proc = createReportProcessor( report, yieldRate );
    }
    reinitOutputTarget();
    try {
      if ( proc.isPaginated() == false ) {
        proc.paginate();
      }
    } finally {
      printer.setContentWriter( null, null );
      printer.setDataWriter( null, null );
    }

    return proc.getLogicalPageCount();
  }

  public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
                       final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
    if ( proc == null ) {
      proc = createReportProcessor( report, yieldRate );
    }

    final boolean singlePageRequested = acceptedPage >= 0;

    final IAsyncReportListener listener = ReportListenerThreadHolder.getListener();
    final boolean asyncMode = listener != null;

    //Add async job listener
    if ( asyncMode ) {
      proc.addReportProgressListener( listener );
    }

    final PageableHtmlOutputProcessor outputProcessor = (PageableHtmlOutputProcessor) proc.getOutputProcessor();

    //Async mode also needs all pages
    if ( singlePageRequested && !asyncMode ) {
      outputProcessor.setFlowSelector( new SinglePageFlowSelector( acceptedPage ) );
    } else {
      outputProcessor.setFlowSelector( new DisplayAllFlowSelector() );
    }

    try {
      //Not in async or in flow mode - fallback to old logic
      if ( !asyncMode || !singlePageRequested ) {
        proxyOutputStream.setParent( outputStream );
        reinitOutputTarget();
        proc.processReport();
        if ( listener != null ) {
          listener.setIsQueryLimitReached( proc.isQueryLimitReached() );
        }
        return proc.getLogicalPageCount();
      } else {
        final Repository repository = reinitOutputTargetRepo();
        proc.processReport();
        //Get whole report and work with it
        final IReportContent completeReport = produceReportContent( proc, repository );
        if ( completeReport == null ) {
          throw new ReportProcessingException( "Can't generate report" );
        }

        if ( listener != null ) {
          listener.setIsQueryLimitReached( proc.isQueryLimitReached() );
        }

        //Write all if scheduled
        final boolean forceAllPages = isForceAllPages( report );
        if ( forceAllPages || listener.isScheduled() ) {
          PaginationControlWrapper.write( outputStream, completeReport );
        } else {
          final byte[] pageData = completeReport.getPageData( acceptedPage );
          if ( pageData != null ) {
            outputStream.write( pageData );
          }
        }
        return completeReport.getPageCount();
      }
    } finally {
      if ( asyncMode ) {
        proc.removeReportProgressListener( listener );
      }
      outputStream.flush();
      printer.setContentWriter( null, null );
      printer.setDataWriter( null, null );
    }
  }

  protected boolean isForceAllPages( final MasterReport report ) {
    return "true".equals( report.getConfiguration().getConfigProperty( PentahoPlatformModule.FORCE_ALL_PAGES ) );
  }

  public boolean supportsPagination() {
    return true;
  }

  public void close() {
    if ( proc != null ) {
      proc.close();
      proxyOutputStream = null;
    }

  }

  protected Repository reinitOutputTargetRepo() throws ReportProcessingException, ContentIOException {

    final Pair<ContentLocation, PentahoNameGenerator> pair = reinitLocationAndNameGenerator();

    final ZipRepository targetRepository = new ZipRepository(); //$NON-NLS-1$
    final ContentLocation targetRoot = targetRepository.getRoot();

    final HtmlPrinter printer = getPrinter();
    // generates predictable file names like "page-0.html", "page-1.html" and so on.
    printer.setContentWriter( targetRoot,
            new PageSequenceNameGenerator( targetRoot, "page", "html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    printer.setDataWriter( pair.getKey(), pair.getValue() );

    return targetRepository;
  }

  private int extractPageFromName( final String name ) {
    if ( name.startsWith( "page-" ) && name.endsWith( ".html" ) ) {
      try {
        final String number = name.substring( "page-".length(), name.length() - ".html".length() );
        return Integer.parseInt( number );
      } catch ( final Exception e ) {
        // ignore invalid names. Outside of a PoC it is probably a good idea to check errors properly.
        return -1;
      }
    }
    return -1;
  }

  protected IReportContent produceReportContent( final PageableReportProcessor proc,
                                                 final Repository targetRepository )
          throws ContentIOException, IOException {
    final int pageCount = proc.getLogicalPageCount();
    final ContentLocation root = targetRepository.getRoot();
    final Map<Integer, byte[]> pages = new HashMap<>();

    for ( final ContentEntity contentEntities : root.listContents() ) {
      if ( contentEntities instanceof ContentItem ) {
        final ContentItem ci = (ContentItem) contentEntities;
        final String name = ci.getName();
        final int pageNumber = extractPageFromName( name );
        if ( pageNumber >= 0 ) {
          pages.put( pageNumber, read( ci.getInputStream() ) );
        }
      }
    }
    return new ReportContentImpl( pageCount, pages );
  }

  private byte[] read( final InputStream in ) throws IOException {
    try {
      return IOUtils.toByteArray( in );
    } finally {
      in.close();
    }
  }
}
