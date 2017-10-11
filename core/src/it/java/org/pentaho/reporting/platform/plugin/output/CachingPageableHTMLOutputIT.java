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

import junit.framework.Assert;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.async.TestListener;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class CachingPageableHTMLOutputIT {

  private IAsyncReportListener listener;
  SimpleReportingComponent rc;
  private static MicroPlatform microPlatform;
  private File tmp;
  private static FileSystemCacheBackend fileSystemCacheBackend;
  private static IPluginCacheManager iPluginCacheManager;

  @BeforeClass
  public static void setUpClass() throws PlatformInitializationException {
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
    iPluginCacheManager =
      spy( new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) ) );
    microPlatform.define( "IPluginCacheManager", iPluginCacheManager );
    microPlatform.start();

  }

  @AfterClass
  public static void tearDownClass() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );

    microPlatform.stop();
    microPlatform = null;
  }


  @Before public void setUp() {
    ClassicEngineBoot.getInstance().start();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
    // create an instance of the component
    rc = new SimpleReportingComponent();

    tmp = new File( "target/test/resource/solution/system/tmp" );
    tmp.mkdirs();


    final IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );

    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test
  public void testGenerate() throws Exception {
    ReportListenerThreadHolder.clear();
    CachingPageableHTMLOutput cachingPageableHTMLOutput = new CachingPageableHTMLOutput();
    cachingPageableHTMLOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testIsQueryLimitReachedFlagStoredInCacheMetadataTrue() throws Exception {
    ReportListenerThreadHolder.clear();
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final MasterReport report = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      report.setContentCacheKey( "some_key" );
      // set row limit for report
      report.setQueryLimit( 100 );

      execute( report );

      assertTrue( listener.isQueryLimitReached() );

      final IReportContentCache cache = iPluginCacheManager.getCache();

      Map<String, Serializable> metaData = cache.getMetaData( report.getContentCacheKey() );
      assertEquals( metaData.get( "IsQueryLimitReached" ), true );

      ReportListenerThreadHolder.clear();
      assertNull( ReportListenerThreadHolder.getListener() );
    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

  @Test
  public void testIsQueryLimitReachedFlagStoredInCacheMetadataFalse() throws Exception {
    ReportListenerThreadHolder.clear();
    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final MasterReport report = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      report.setContentCacheKey( "somekey" );
      // set unlimited rows for report
      report.setQueryLimit( -1 );

      execute( report );

      assertFalse( listener.isQueryLimitReached() );

      final IReportContentCache cache = iPluginCacheManager.getCache();

      Map<String, Serializable> metaData = cache.getMetaData( report.getContentCacheKey() );
      assertNull( metaData.get( "IsQueryLimitReached" ) );

      ReportListenerThreadHolder.clear();
      assertNull( ReportListenerThreadHolder.getListener() );
    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

  @Test
  public void testIsQueryLimitReachedForFirstPageModeDisabled() throws Exception {
    ReportListenerThreadHolder.clear();
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", "true" );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "false" );
    try {

      final TestListener listener = new TestListener( "1", UUID.randomUUID(), "" );
      ReportListenerThreadHolder.setListener( listener );
      final ResourceManager mgr = new ResourceManager();
      final File src = new File( "target/test/resource/solution/test/reporting/report.prpt" );
      final MasterReport report = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
      report.setContentCacheKey( "some_key" );
      // set row limit for report
      report.setQueryLimit( 20 );
      execute( report );

      assertTrue( listener.isQueryLimitReached() );
      final IReportContentCache cache = iPluginCacheManager.getCache();
      Map<String, Serializable> metaData = cache.getMetaData( report.getContentCacheKey() );
      assertEquals( metaData.get( "IsQueryLimitReached" ), true );

      ReportListenerThreadHolder.clear();
      assertNull( ReportListenerThreadHolder.getListener() );
    } finally {
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.CachePageableHtmlContent", null );
      edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", null );
    }
  }

  private void execute( final MasterReport r ) throws Exception {
    // create an instance of the component
    final SimpleReportingComponent rc = new SimpleReportingComponent();
    // create/set the InputStream
    rc.setReport( r );
    rc.setOutputType( "text/html" ); //$NON-NLS-1$

    // turn on pagination, by way of input (typical mode for xaction)
    final HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "content-handler-pattern", "test" );
    rc.setInputs( inputs );

    final FileOutputStream outputStream =
      new FileOutputStream( new File( tmp, System.currentTimeMillis() + ".html" ) ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setOutputStream( outputStream );

    // execute the component
    assertTrue( rc.execute() );
  }

  @Test
  public void testScheduledReportContainsAllPages() throws Exception {

    ReportListenerThreadHolder.clear();
    final CountDownLatch latch1 = new CountDownLatch( 1 );
    final CountDownLatch latch2 = new CountDownLatch( 1 );
    final ExecutorService executorService = Executors.newFixedThreadPool( 2 );

    final SimpleReportingComponent rc = new SimpleReportingComponent();
    final ResourceManager mgr = new ResourceManager();
    final File src = new File( "target/test/resource/solution/test/reporting/BigReport.prpt" );
    final MasterReport masterReport = (MasterReport) mgr.createDirectly( src, MasterReport.class ).getResource();
    final String key = "test";
    masterReport.setContentCacheKey( key );
    masterReport.setQueryLimit( 500 );
    rc.setReport( masterReport );

    rc.setOutputType( "text/html" ); //$NON-NLS-1$
    // turn on pagination, by way of input (typical mode for xaction)
    final HashMap<String, Object> inputs = new HashMap<String, Object>();
    inputs.put( "paginate", "true" ); //$NON-NLS-1$ //$NON-NLS-2$
    inputs.put( "accepted-page", "0" ); //$NON-NLS-1$ //$NON-NLS-2$
    rc.setInputs( inputs );

    final TestListener future1Listener = new TestListener( "1", UUID.randomUUID(), "" );
    future1Listener.setStatus( AsyncExecutionStatus.SCHEDULED );
    final TestListener future2Listener = new TestListener( "1", UUID.randomUUID(), "" );

    final Future<byte[]> future1 =
      executorService.submit( new CachingPageableHTMLOutputIT.TestTask( latch1, rc, future1Listener ) );
    final Future<byte[]> future2 =
      executorService.submit( new CachingPageableHTMLOutputIT.TestTask( latch2, rc, future2Listener ) );

    latch2.countDown();
    while ( !future2.isDone() ) {
      if ( future2Listener.isOnFirstPage() ) {
        latch1.countDown();
        break;
      }
      Thread.sleep( 10 );
    }

    final String content2 = new String( future2.get(), "UTF-8" );
    assertFalse( content2.contains( "Scheduled paginated HTML report" ) );

    final String content1 = new String( future1.get(), "UTF-8" );
    assertTrue( content1.contains( "Scheduled paginated HTML report" ) );

    assertEquals( future2Listener.getState().getPage(), future1Listener.getState().getTotalPages() );
    assertEquals( future1Listener.getState().getPage(), future1Listener.getState().getTotalPages() );
  }

  private class TestTask implements Callable<byte[]> {

    private final CountDownLatch latch;
    private final SimpleReportingComponent rc;
    private final TestListener listener;

    private TestTask( final CountDownLatch latch, final SimpleReportingComponent rc, final TestListener listener ) {
      this.latch = latch;
      this.rc = rc;
      this.listener = listener;
    }

    @Override public byte[] call() throws Exception {
      latch.await();
      try ( final java.io.ByteArrayOutputStream outputStream =
              new java.io.ByteArrayOutputStream() ) { //$NON-NLS-1$ //$NON-NLS-2$
        rc.setOutputStream( outputStream );
        ReportListenerThreadHolder.setListener( listener );
        rc.execute();
        return outputStream.toByteArray();
      }
    }
  }

}
