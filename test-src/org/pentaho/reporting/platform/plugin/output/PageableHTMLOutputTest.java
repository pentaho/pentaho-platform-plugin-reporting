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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.async.TestListener;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class PageableHTMLOutputTest {
  PageableHTMLOutput pageableHTMLOutput;
  private IAsyncReportListener listener;

  @Before public void setUp() {
    pageableHTMLOutput = new PageableHTMLOutput();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( pageableHTMLOutput, pageableHTMLOutput.getReportLock() );
  }

  @Test public void testSetContentHandlerPattern() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getContentHandlerPattern() );
    pageableHTMLOutput.setContentHandlerPattern( "pattern" ); //$NON-NLS-1$
    Assert.assertEquals( "pattern", pageableHTMLOutput.getContentHandlerPattern() ); //$NON-NLS-1$
  }

  @Test public void testSetProxyOutputStream() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getProxyOutputStream() );
    ProxyOutputStream mockStream = mock( ProxyOutputStream.class );
    pageableHTMLOutput.setProxyOutputStream( mockStream );
    Assert.assertEquals( mockStream, pageableHTMLOutput.getProxyOutputStream() );
  }

  @Test public void testSetPrinter() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getPrinter() );
    AllItemsHtmlPrinter mockPrinter = mock( AllItemsHtmlPrinter.class );
    pageableHTMLOutput.setPrinter( mockPrinter );
    Assert.assertEquals( mockPrinter, pageableHTMLOutput.getPrinter() );
  }

  @Test public void testSetReportProcessor() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getReportProcessor() );
    PageableReportProcessor mockProcessor = mock( PageableReportProcessor.class );
    pageableHTMLOutput.setReportProcessor( mockProcessor );
    Assert.assertEquals( mockProcessor, pageableHTMLOutput.getReportProcessor() );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( true, pageableHTMLOutput.supportsPagination() );
  }

  @Test public void testPaginate() throws Exception {
    PageableHTMLOutput output = mock( PageableHTMLOutput.class, CALLS_REAL_METHODS );
    PageableReportProcessor processor = mock( PageableReportProcessor.class );
    doNothing().when( output ).reinitOutputTarget();
    doReturn( true ).when( processor ).isPaginated();
    MasterReport report = mock( MasterReport.class );
    AllItemsHtmlPrinter printer = mock( AllItemsHtmlPrinter.class );
    doNothing().when( printer ).setContentWriter( null, null );
    doNothing().when( printer ).setDataWriter( null, null );

    output.setReportProcessor( processor );
    output.setPrinter( printer );
    Assert.assertEquals( 0, output.paginate( report, 0 ) );

    doReturn( false ).when( processor ).isPaginated();
    output.setReportProcessor( processor );
    Assert.assertEquals( 0, output.paginate( report, 0 ) );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    pageableHTMLOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerate() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    pageableHTMLOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerateListenerFlow() throws Exception {
    ClassicEngineBoot.getInstance().start();
    pageableHTMLOutput.generate( new MasterReport(), -1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerateFlow() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    pageableHTMLOutput.generate( new MasterReport(), -1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testPageNotInCacheSchedule() throws PlatformInitializationException, ContentIOException,
    ReportProcessingException, IOException,
    ResourceException {
    ClassicEngineBoot.getInstance().start();

    MicroPlatform microPlatform = MicroPlatformFactory.create();

    try {
      microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
      final IReportContentCache mockCache = mock( IReportContentCache.class );
      final IReportContent iReportContent = mock( IReportContent.class );
      when( iReportContent.getPageData( 3 ) ).thenReturn( null );

      final String key = "test";
      when( mockCache.get( key ) ).thenReturn( iReportContent );
      final IPluginCacheManager iPluginCacheManager =
        new PluginCacheManagerImpl( mockCache );
      microPlatform.define( "IPluginCacheManager", iPluginCacheManager );
      microPlatform.start();

      final IPentahoSession session = new StandaloneSession();
      PentahoSessionHolder.setSession( session );


      final CachingPageableHTMLOutput cachingPageableHTMLOutput = spy( new CachingPageableHTMLOutput() );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "resource/solution/test/reporting/report.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      r.setContentCacheKey( key );

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      listener.setStatus( AsyncExecutionStatus.SCHEDULED );
      ReportListenerThreadHolder.setListener( listener );


      try ( final java.io.ByteArrayOutputStream outputStream =
              new java.io.ByteArrayOutputStream() ) { //$NON-NLS-1$ //$NON-NLS-2$
        cachingPageableHTMLOutput.generate( r, 3, outputStream, 1 );
        final String content = new String( outputStream.toByteArray(), "UTF-8" );
        assertTrue( content.contains( "Scheduled paginated HTML report" ) );
      }


      verify( cachingPageableHTMLOutput, times( 1 ) ).regenerateCache( r, 1, key, 3 );


    } finally {
      ReportListenerThreadHolder.clear();
      microPlatform.stop();
      microPlatform = null;
    }
  }


  @Test
  public void testPageNotInCache() throws PlatformInitializationException, ContentIOException,
    ReportProcessingException, IOException,
    ResourceException {
    ClassicEngineBoot.getInstance().start();

    MicroPlatform microPlatform = MicroPlatformFactory.create();

    try {
      microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
      final IReportContentCache mockCache = mock( IReportContentCache.class );
      final IReportContent iReportContent = mock( IReportContent.class );
      when( iReportContent.getPageData( 3 ) ).thenReturn( null );

      final String key = "test";
      when( mockCache.get( key ) ).thenReturn( iReportContent );
      final IPluginCacheManager iPluginCacheManager =
        new PluginCacheManagerImpl( mockCache );
      microPlatform.define( "IPluginCacheManager", iPluginCacheManager );
      microPlatform.start();

      final IPentahoSession session = new StandaloneSession();
      PentahoSessionHolder.setSession( session );


      final CachingPageableHTMLOutput cachingPageableHTMLOutput = spy( new CachingPageableHTMLOutput() );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "resource/solution/test/reporting/report.prpt" );
      final MasterReport r = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      r.setContentCacheKey( key );

      cachingPageableHTMLOutput.generate( r, 3, mock( OutputStream.class ), 1 );

      verify( cachingPageableHTMLOutput, times( 1 ) ).regenerateCache( r, 1, key, 3 );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }
}
