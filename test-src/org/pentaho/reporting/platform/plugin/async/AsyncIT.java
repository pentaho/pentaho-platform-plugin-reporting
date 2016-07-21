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

package org.pentaho.reporting.platform.plugin.async;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.io.input.NullInputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.layout.output.ReportProcessorThreadHolder;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.JaxRsServerProvider;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.reporting.platform.plugin.output.FastExportReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncIT {

  private static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";
  private static final String MIME = "junit_mime";
  private static final String PATH = "junit_path";
  private static final NullInputStream NULL_INPUT_STREAM = new NullInputStream( 100 );
  private static int PAGE = 0;
  private static int PROGRESS = -113;
  private static AsyncExecutionStatus STATUS = AsyncExecutionStatus.FAILED;

  private static MicroPlatform microPlatform;
  private static FileSystemCacheBackend fileSystemCacheBackend;

  private static JaxRsServerProvider provider;
  private static IPentahoSession session;
  private static IUnifiedRepository repository;
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );


  @BeforeClass
  public static void setUp() throws Exception {
    provider = new JaxRsServerProvider();
    provider.startServer( new JobManager() );
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
    microPlatform
      .define( "IPluginCacheManager", new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) ) );
    final PentahoAsyncExecutor<AsyncReportState> executor = spy( new TestExecutor( 2, 100 ) );
    microPlatform.define( "IPentahoAsyncExecutor", executor );
    microPlatform.define( "ISchedulingDirectoryStrategy", new HomeSchedulingDirStrategy() );
    microPlatform.addLifecycleListener( new AsyncSystemListener() );

    repository = mock( IUnifiedRepository.class );

    microPlatform.define( "IUnifiedRepository", repository );

    microPlatform.start();

    session = new StandaloneSession();

  }


  @AfterClass
  public static void tearDown() throws PlatformInitializationException {
    PentahoSystem.shutdown();
    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
    microPlatform.stop();
    microPlatform = null;
    provider.stopServer();
    provider = null;
  }

  @After
  public void after() {
    reset( PentahoSystem.get( IPentahoAsyncExecutor.class ) );
  }

  @Before
  public void before() {
    PentahoSessionHolder.setSession( session );
  }

  @Test
  public void testDefaultConfig() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "config", "" );
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertEquals( json,
      "{\"pollingIntervalMilliseconds\":500,\"dialogThresholdMilliseconds\":1500,\"promptForLocation\":false,"
        + "\"supportAsync\":true}" );
  }

  @Test
  public void testCustomConfig() throws Exception {
    provider.stopServer();
    provider.startServer( new JobManager( false, 100L, 300L, true ) );
    final String config = String.format( URL_FORMAT, "config", "" );
    final WebClient client = provider.getFreshClient();
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertEquals( json,
      "{\"pollingIntervalMilliseconds\":100,\"dialogThresholdMilliseconds\":300,\"promptForLocation\":true,"
        + "\"supportAsync\":false}" );
    provider.stopServer();
    provider.startServer( new JobManager() );
  }

  @Test
  public void testStatusNoTask() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/status" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }

  @Test
  public void testStatusInvalidId() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "noidhereman", "/status" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testQueueReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/status" );
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertFalse( StringUtil.isEmpty( json ) );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();


    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/schedule" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    WebClient client1 = provider.getFreshClient();
    client1.path( String.format( URL_FORMAT, uuid, "/status" ) );
    Response response1 = client1.get();
    final String json = response1.readEntity( String.class );
    assertTrue( json.contains( "SCHEDULED" ) );

    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/schedule" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/schedule" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleTwice() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();


    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/schedule" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    WebClient client1 = provider.getFreshClient();
    client1.path( String.format( URL_FORMAT, uuid, "/status" ) );
    Response response1 = client1.get();
    final String json = response1.readEntity( String.class );
    assertTrue( json.contains( "SCHEDULED" ) );

    WebClient client2 = provider.getFreshClient();
    final String config2 = String.format( URL_FORMAT, uuid, "/schedule" );
    client2.path( config2 );
    Response response2 = client2.get();
    assertEquals( 404, response2.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testCancelReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<Void>() {
      @Override public Void answer( final InvocationOnMock invocation ) throws Throwable {
        STATUS = AsyncExecutionStatus.CANCELED;
        return null;
      }
    } ).when( listenableFuture ).cancel( true );

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/cancel" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/status" ) );
    response = client.get();
    final String json = response.readEntity( String.class );
    assertTrue( json.contains( "CANCELED" ) );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testCacelWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/cancel" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testCancelInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/cancel" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPage() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );


    final PentahoAsyncReportExecution execution = mockExec();


    final UUID uuid = executor.addTask( execution, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/requestPage/100" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    assertEquals( "100", response.readEntity( String.class ) );

    PAGE = 0;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPageWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/requestPage/100" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPageInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/requestPage/1" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContent() throws ExecutionException, InterruptedException {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<IFixedSizeStreamingContent>() {
      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
        return new IFixedSizeStreamingContent() {

          @Override public InputStream getStream() {
            return NULL_INPUT_STREAM;
          }

          @Override public long getContentSize() {
            return 0;
          }

          @Override public boolean cleanContent() {
            return false;
          }
        };

      }
    } ).when( listenableFuture ).get();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/content" );
    client.path( config );
    Response response = client.post( null );
    assertEquals( 202, response.getStatus() );
    STATUS = AsyncExecutionStatus.FINISHED;
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/content" ) );
    response = client.post( null );
    assertEquals( 200, response.getStatus() );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentGet() throws ExecutionException, InterruptedException {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<IFixedSizeStreamingContent>() {
      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
        return new IFixedSizeStreamingContent() {

          @Override public InputStream getStream() {
            return NULL_INPUT_STREAM;
          }

          @Override public long getContentSize() {
            return 0;
          }

          @Override public boolean cleanContent() {
            return false;
          }
        };

      }
    } ).when( listenableFuture ).get();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/content" );
    client.path( config );
    Response response = client.get();
    assertEquals( 202, response.getStatus() );
    STATUS = AsyncExecutionStatus.FINISHED;
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/content" ) );
    response = client.get();
    assertEquals( 200, response.getStatus() );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/content" );
    client.path( config );
    final Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/content" );
    client.path( config );
    final Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentFutureFailed() throws ExecutionException, InterruptedException {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    when( listenableFuture.get() ).thenThrow( new RuntimeException() );
    STATUS = AsyncExecutionStatus.FINISHED;
    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/content" );
    client.path( config );
    Response response = client.get();
    assertEquals( 500, response.getStatus() );
    STATUS = AsyncExecutionStatus.FAILED;
  }

  @Test public void testAutoScheduling() throws InterruptedException, ExecutionException {


    final List<Integer[]> objects = Arrays.asList( new Integer[][] {
      { 0, 0, 0 },
      { 0, Integer.MAX_VALUE, 0 },
      { 0, -1, 0 },
      { 1, 0, 0 },
      { 1, Integer.MAX_VALUE, 1 },
      { 1, -1, 0 }
    } );


    for ( final Integer[] params : objects ) {

      final PentahoAsyncExecutor exec = spy( new PentahoAsyncExecutor( 1, params[ 0 ] ) {
        @Override public boolean schedule( final UUID id, final IPentahoSession session ) {
          return true;
        }
      } );

      ReportProgressEvent event = null;
      if ( params[ 1 ] >= 0 ) {
        event = mock( ReportProgressEvent.class );
        when( event.getMaximumRow() ).thenReturn( params[ 1 ] );
      }

      final ReportProgressEvent finalEvent = event;

      final UUID id =
        exec.addTask( new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
          AuditWrapper.NULL ) {
          @Override
          protected AsyncReportStatusListener createListener( final UUID id,
                                                              final List<? extends ReportProgressListener>
                                                                callbackListener ) {
            return new AsyncReportStatusListener( getReportPath(), id, getMimeType(), callbackListener );
          }

          @Override public IFixedSizeStreamingContent call() throws Exception {
            getListener().reportProcessingStarted( finalEvent );
            getListener().reportProcessingUpdate( finalEvent );
            getListener().reportProcessingFinished( finalEvent );
            verify( exec, times( params[ 2 ] ) ).schedule( any(), any() );
            return null;
          }
        }, session );
    }

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocation() throws Exception {
    try {


      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
      final IAsyncReportExecution mock = mockExec();

      final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );

      WebClient client2 = provider.getFreshClient();
      final String config2 = String.format( URL_FORMAT, uuid, "/schedule" );
      client2.path( config2 );
      Response response2 = client2.get();
      assertEquals( 200, response2.getStatus() );

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule/location" );
      client.path( config );
      client.query( "folderId", folderId );
      client.query( "newName", "test" );

      Response response = client.post( null );
      assertEquals( 200, response.getStatus() );
      verify( executor, times( 1 ) )
        .updateSchedulingLocation( any(), any(), any(), any() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocationNotScheduled() throws Exception {
    try {


      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
      final IAsyncReportExecution mock = mockExec();

      final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule/location" );
      client.path( config );
      client.query( "folderId", folderId );

      Response response = client.post( null );
      assertEquals( 404, response.getStatus() );


      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocationDisabledPrompting() throws Exception {

    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();


    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final UUID folderId = UUID.randomUUID();
    final String config = String.format( URL_FORMAT, uuid, "/schedule/location" );
    client.path( config );
    client.query( "folderId", folderId );

    Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocationWrongID() {

    final UUID folderId = UUID.randomUUID();
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/sheduling/location?targetId=" + folderId );
    client.path( config );
    client.query( "folderId", folderId );

    Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testLogout() {

    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    PentahoSystem.invokeLogoutListeners( PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/status" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleTwiceNoJobManager() {

    final CountDownLatch countDownLatch = new CountDownLatch( 1 );
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock =
      new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null", AuditWrapper.NULL ) {

        @Override public IFixedSizeStreamingContent call() throws Exception {
          countDownLatch.await();
          return new NullSizeStreamingContent();
        }
      };


    final UUID uuid = executor.addTask( mock, session );
    assertTrue( executor.schedule( uuid, session ) );
    assertFalse( executor.schedule( uuid, session ) );
    countDownLatch.countDown();

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleNoSession() throws Exception {
    final boolean[] flag = { false };
    PentahoSessionHolder.removeSession();
    new WriteToJcrTask( mockExec(), mock( InputStream.class ) ) {
      @Override protected ReportContentRepository getReportContentRepository( RepositoryFile outputFolder ) {
        if ( outputFolder == null ) {
          flag[ 0 ] = true;
        }
        return null;
      }
    }.call();
    assertTrue( flag[ 0 ] );
    PentahoSessionHolder.setSession( session );

  }

  @Test public void testCancelNoListener() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, 0 );
    final PentahoAsyncReportExecution pentahoAsyncReportExecution = new PentahoAsyncReportExecution( "junit-path",
      component, handler, session, "not null", AuditWrapper.NULL ) {
      @Override
      public void notifyTaskQueued( final UUID id, final List<? extends ReportProgressListener> callbackListeners ) {

      }
    };

    exec.addTask( pentahoAsyncReportExecution, session );
    //Works ok without listener
    pentahoAsyncReportExecution.cancel();
  }


  @Test public void testCancelNotInterruptable() {

    try {

      final CountDownLatch latch = new CountDownLatch( 1 );

      final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, 0 );
      final PentahoAsyncReportExecution pentahoAsyncReportExecution = new PentahoAsyncReportExecution( "junit-path",
        component, handler, session, "not null", AuditWrapper.NULL ) {

        @Override public IFixedSizeStreamingContent call() throws Exception {
          latch.await();
          return new NullSizeStreamingContent();
        }
      };


      final UUID uuid = exec.addTask( pentahoAsyncReportExecution, session );

      final Future future = exec.getFuture( uuid, session );

      final AbstractReportProcessor processor = mock( AbstractReportProcessor.class );

      ReportProcessorThreadHolder.setProcessor( processor );

      future.cancel( false );

      pentahoAsyncReportExecution.getListener()
        .reportProcessingUpdate( new ReportProgressEvent( this, ReportProgressEvent.PAGINATING, 0, 0, 0, 0, 0, 0 ) );

      verify( processor, never() ).cancel();

      latch.countDown();
    } finally {
      ReportProcessorThreadHolder.clear();
    }
  }


  private PentahoAsyncReportExecution mockExec() {
    return new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
      AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener>
                                                            callbackListener ) {
        return new AsyncReportStatusListener( getReportPath(), id, getMimeType(), callbackListener );
      }

      @Override public IAsyncReportState getState() {
        return AsyncIT.getState();
      }

      @Override public boolean schedule() {
        STATUS = AsyncExecutionStatus.SCHEDULED;
        return true;
      }
    };
  }

  private static IAsyncReportState getState() {
    return new IAsyncReportState() {

      @Override public String getPath() {
        return PATH;
      }

      @Override public UUID getUuid() {
        return UUID.randomUUID();
      }

      @Override public AsyncExecutionStatus getStatus() {
        return STATUS;
      }

      @Override public int getProgress() {
        return PROGRESS;
      }

      @Override public int getPage() {
        return PAGE;
      }

      @Override public int getTotalPages() {
        return 0;
      }

      @Override public int getGeneratedPage() {
        return 0;
      }

      @Override public int getRow() {
        return 0;
      }

      @Override public int getTotalRows() {
        return 0;
      }

      @Override public String getActivity() {
        return null;
      }

      @Override public String getMimeType() {
        return MIME;
      }

      @Override public String getErrorMessage() {
        return null;
      }

      @Override public boolean getIsQueryLimitReached() {
        return false;
      }

    };
  }

  public static class TestExecutor extends PentahoAsyncExecutor<AsyncReportState> {

    public TestExecutor( final int capacity, final int autoSchedulerThreshold ) {
      super( capacity, autoSchedulerThreshold );
    }

    @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                                  final IAsyncReportExecution<? extends
                                                                    IAsyncReportState> runningTask ) {

      final FakeLocation fakeLocation = new FakeLocation();
      final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
      try {
        when( contentRepository.getRoot() ).thenReturn( fakeLocation );
      } catch ( final ContentIOException e ) {
        e.printStackTrace();
      }
      return new WriteToJcrTask( runningTask, result.getStream() ) {
        @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
          return contentRepository;
        }
      };

    }
  }

}
