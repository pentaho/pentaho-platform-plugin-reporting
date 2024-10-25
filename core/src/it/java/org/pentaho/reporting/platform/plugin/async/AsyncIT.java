///*! ******************************************************************************
// *
// * Pentaho
// *
// * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
// *
// * Use of this software is governed by the Business Source License included
// * in the LICENSE.TXT file.
// *
// * Change Date: 2029-07-20
// ******************************************************************************/
//
//
//package org.pentaho.reporting.platform.plugin.async;
//
//import com.google.common.util.concurrent.ListenableFuture;
//import org.apache.commons.io.input.NullInputStream;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.pentaho.platform.api.engine.IApplicationContext;
//import org.pentaho.platform.api.engine.IPentahoSession;
//import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
//import org.pentaho.platform.api.repository2.unified.RepositoryFile;
//import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
//import org.pentaho.platform.engine.core.system.PentahoSystem;
//import org.pentaho.platform.engine.core.system.StandaloneSession;
//import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
//import org.pentaho.platform.util.StringUtil;
//import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
//import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
//import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
//import org.pentaho.reporting.engine.classic.core.layout.output.ReportProcessorThreadHolder;
//import org.pentaho.reporting.libraries.repository.ContentIOException;
//import org.pentaho.reporting.platform.plugin.AuditWrapper;
//import org.pentaho.reporting.platform.plugin.JobManager;
//import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
//import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
//import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
//import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
//import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
//import org.pentaho.reporting.platform.plugin.output.FastExportReportOutputHandlerFactory;
//import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
//import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
//import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
//import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
//import org.pentaho.test.platform.engine.core.MicroPlatform;
//
//import javax.ws.rs.core.Response;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.Serializable;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.UUID;
//import java.util.concurrent.Callable;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutionException;
//import java.util.concurrent.Future;
//
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;
//
//public class AsyncIT {
//
//
//  private static final String MIME = "junit_mime";
//  private static final String PATH = "junit_path";
//  private static final NullInputStream NULL_INPUT_STREAM = new NullInputStream( 100 );
//  private static int PAGE = 0;
//  private static int PROGRESS = -113;
//  private static AsyncExecutionStatus STATUS = AsyncExecutionStatus.FAILED;
//
//  private MicroPlatform microPlatform;
//  private FileSystemCacheBackend fileSystemCacheBackend;
//
//
//  private IPentahoSession session;
//  private SimpleReportingComponent component = mock( SimpleReportingComponent.class );
//  private AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
//
//
//  @Before
//  public void setUp() throws Exception {
//    fileSystemCacheBackend = new FileSystemCacheBackend();
//    fileSystemCacheBackend.setCachePath( "/test-cache/" );
//    microPlatform = MicroPlatformFactory.create();
//    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
//    microPlatform
//      .define( "IPluginCacheManager", new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) ) );
//    final PentahoAsyncExecutor<AsyncReportState> executor = spy( new TestExecutor( 2, 100 ) );
//    microPlatform.define( "IPentahoAsyncExecutor", executor );
//    microPlatform.define( "ISchedulingDirectoryStrategy", new HomeSchedulingDirStrategy() );
//    microPlatform.define( "IJobIdGenerator", new JobIdGenerator() );
//    microPlatform.addLifecycleListener( new AsyncSystemListener() );
//
//    IUnifiedRepository repository = mock( IUnifiedRepository.class );
//    final RepositoryFile repositoryFile = mock( RepositoryFile.class );
//    when( repositoryFile.getPath() ).thenReturn( "/home/unknown" );
//    when( repositoryFile.getId() ).thenReturn( "/home/unknown" );
//    when( repository.getFile( "/home/unknown" ) ).thenReturn( repositoryFile );
//
//    microPlatform.define( "IUnifiedRepository", repository );
//
//    microPlatform.start();
//
//    session = new StandaloneSession();
//
//    PentahoSessionHolder.setSession( session );
//  }
//
//
//  @After
//  public void tearDown() throws PlatformInitializationException {
//    PentahoSystem.shutdown();
//    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
//    microPlatform.stop();
//    microPlatform = null;
//    PentahoSessionHolder.removeSession();
//  }
//
//  @Test
//  public void testDefaultConfig() {
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getConfig();
//    final String json = response.readEntity( String.class );
//    assertEquals( json,
//      "{\"defaultOutputPath\":\"/home/unknown\",\"dialogThresholdMilliseconds\":1500,"
//        + "\"pollingIntervalMilliseconds\":500,\"promptForLocation\":false,\"supportAsync\":true}" );
//  }
//
//  @Test
//  public void testCustomConfig() throws Exception {
//    final JobManager jobManager = new JobManager( false, 100L, 300L, true );
//    final Response response = jobManager.getConfig();
//    final String json = response.readEntity( String.class );
//    assertEquals( json,
//      "{\"defaultOutputPath\":\"/home/unknown\",\"dialogThresholdMilliseconds\":300,"
//        + "\"pollingIntervalMilliseconds\":100,\"promptForLocation\":true,\"supportAsync\":false}" );
//  }
//
//  @Test
//  public void testStatusNoTask() {
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getStatus( UUID.randomUUID().toString() );
//    assertEquals( 404, response.getStatus() );
//  }
//
//  @Test
//  public void testStatusInvalidId() {
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getStatus( "notauuid" );
//    assertEquals( 404, response.getStatus() );
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testQueueReport() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getStatus( uuid.toString() );
//    final String json = response.readEntity( String.class );
//    assertFalse( StringUtil.isEmpty( json ) );
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleReport() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//    final JobManager jobManager = new JobManager();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final Response response = jobManager.schedule( uuid.toString(), true );
//    assertEquals( 200, response.getStatus() );
//    final Response response1 = jobManager.getStatus( uuid.toString() );
//    final String json = response1.readEntity( String.class );
//    assertTrue( json.contains( "SCHEDULED" ) );
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleWrongID() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.schedule( UUID.randomUUID().toString(), true );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleInvalid() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.schedule( "notauuid", true );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleTwice() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final JobManager jobManager = new JobManager();
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//    final Response response = jobManager.schedule( uuid.toString(), true );
//    assertEquals( 200, response.getStatus() );
//
//    final Response response1 = jobManager.getStatus( uuid.toString() );
//    final String json = response1.readEntity( String.class );
//    assertTrue( json.contains( "SCHEDULED" ) );
//
//
//    final Response response2 = jobManager.schedule( uuid.toString(), true );
//    assertEquals( 404, response2.getStatus() );
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testCancelReport() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
//    final IAsyncReportState state = getState();
//    when( mock.getState() ).thenReturn( state );
//    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
//    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
//    doAnswer( new Answer<Void>() {
//      @Override public Void answer( final InvocationOnMock invocation ) throws Throwable {
//        STATUS = AsyncExecutionStatus.CANCELED;
//        return null;
//      }
//    } ).when( listenableFuture ).cancel( true );
//    final JobManager jobManager = new JobManager();
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//    Response response = jobManager.cancel( uuid.toString() );
//    assertEquals( 200, response.getStatus() );
//
//    response = jobManager.getStatus( uuid.toString() );
//    final String json = response.readEntity( String.class );
//    assertTrue( json.contains( "CANCELED" ) );
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testCacelWrongID() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.cancel( UUID.randomUUID().toString() );
//    assertEquals( 200, response.getStatus() );
//
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testCancelInvalid() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.cancel( "notauuid" );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testRequestPage() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//
//
//    final PentahoAsyncReportExecution execution = mockExec();
//
//
//    final UUID uuid = executor.addTask( execution, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.requestPage( uuid.toString(), 100 );
//    assertEquals( 200, response.getStatus() );
//    assertEquals( "100", response.readEntity( String.class ) );
//
//    PAGE = 0;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testRequestPageWrongID() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.requestPage( UUID.randomUUID().toString(), 100 );
//    assertEquals( 404, response.getStatus() );
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testRequestPageInvalid() {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.requestPage( "notauuid", 100 );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testContent() throws ExecutionException, InterruptedException, IOException {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
//    final IAsyncReportState state = getState();
//    when( mock.getState() ).thenReturn( state );
//    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
//    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
//    doAnswer( new Answer<IFixedSizeStreamingContent>() {
//      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
//        return new IFixedSizeStreamingContent() {
//
//          @Override public InputStream getStream() {
//            return NULL_INPUT_STREAM;
//          }
//
//          @Override public long getContentSize() {
//            return 0;
//          }
//
//          @Override public boolean cleanContent() {
//            return false;
//          }
//        };
//
//      }
//    } ).when( listenableFuture ).get();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//
//    Response response = jobManager.getContent( uuid.toString() );
//    assertEquals( 202, response.getStatus() );
//    STATUS = AsyncExecutionStatus.FINISHED;
//    response = jobManager.getContent( uuid.toString() );
//    assertEquals( 200, response.getStatus() );
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testContentGet() throws ExecutionException, InterruptedException, IOException {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
//    final IAsyncReportState state = getState();
//    when( mock.getState() ).thenReturn( state );
//    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
//    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
//    doAnswer( new Answer<IFixedSizeStreamingContent>() {
//      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
//        return new IFixedSizeStreamingContent() {
//
//          @Override public InputStream getStream() {
//            return NULL_INPUT_STREAM;
//          }
//
//          @Override public long getContentSize() {
//            return 0;
//          }
//
//          @Override public boolean cleanContent() {
//            return false;
//          }
//        };
//
//      }
//    } ).when( listenableFuture ).get();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    Response response = jobManager.getPDFContent( uuid.toString() );
//    assertEquals( 202, response.getStatus() );
//    STATUS = AsyncExecutionStatus.FINISHED;
//    response = jobManager.getPDFContent( uuid.toString() );
//    assertEquals( 200, response.getStatus() );
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testContentWrongID() throws IOException {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//
//    final Response response = jobManager.getContent( UUID.randomUUID().toString() );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testContentInvalid() throws IOException {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
//
//    executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getContent( "notauuid" );
//    assertEquals( 404, response.getStatus() );
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testContentFutureFailed() throws ExecutionException, InterruptedException, IOException {
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
//    final IAsyncReportState state = getState();
//    when( mock.getState() ).thenReturn( state );
//    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
//    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
//    when( listenableFuture.get() ).thenThrow( new RuntimeException() );
//    STATUS = AsyncExecutionStatus.FINISHED;
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getContent( uuid.toString() );
//    assertEquals( 500, response.getStatus() );
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//  @Test public void testAutoScheduling() throws InterruptedException, ExecutionException {
//
//
//    final List<Integer[]> objects = Arrays.asList( new Integer[][] {
//      { 0, 0, 0 },
//      { 0, Integer.MAX_VALUE, 0 },
//      { 0, -1, 0 },
//      { 1, 0, 0 },
//      { 1, Integer.MAX_VALUE, 1 },
//      { 1, -1, 0 }
//    } );
//
//
//    for ( final Integer[] params : objects ) {
//
//      final PentahoAsyncExecutor exec = spy( new PentahoAsyncExecutor( 1, params[ 0 ] ) {
//        @Override public boolean schedule( final UUID id, final IPentahoSession session ) {
//          return true;
//        }
//      } );
//
//      ReportProgressEvent event = null;
//      if ( params[ 1 ] >= 0 ) {
//        event = mock( ReportProgressEvent.class );
//        when( event.getMaximumRow() ).thenReturn( params[ 1 ] );
//      }
//
//      final ReportProgressEvent finalEvent = event;
//
//      final UUID id =
//        exec.addTask( new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
//          AuditWrapper.NULL ) {
//          @Override
//          protected AsyncReportStatusListener createListener( final UUID id,
//                                                              final List<? extends ReportProgressListener>
//                                                                callbackListener ) {
//            return new AsyncReportStatusListener( getReportPath(), id, getMimeType(), callbackListener );
//          }
//
//          @Override public IFixedSizeStreamingContent call() throws Exception {
//            getListener().reportProcessingStarted( finalEvent );
//            getListener().reportProcessingUpdate( finalEvent );
//            getListener().reportProcessingFinished( finalEvent );
//            verify( exec, times( params[ 2 ] ) ).schedule( any(), any() );
//            return null;
//          }
//        }, session );
//    }
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testUpdateScheduleLocation() throws Exception {
//
//
//    final JobManager jobManager = new JobManager( true, 1000, 1000, true );
//
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//
//    final Response response2 = jobManager.schedule( uuid.toString(), false );
//    assertEquals( 200, response2.getStatus() );
//
//
//    final UUID folderId = UUID.randomUUID();
//
//
//    final Response response = jobManager.confirmSchedule( uuid.toString(), true, false, folderId.toString(), "test" );
//    assertEquals( 200, response.getStatus() );
//    verify( executor, times( 1 ) )
//      .updateSchedulingLocation( any(), any(), any(), any() );
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testUpdateScheduleLocationNotScheduled() throws Exception {
//
//    final JobManager jobManager = new JobManager( true, 1000, 1000, true );
//
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//
//    final UUID folderId = UUID.randomUUID();
//
//
//    final Response response = jobManager.confirmSchedule( uuid.toString(), false, false, folderId.toString(), "test" );
//    assertEquals( 404, response.getStatus() );
//
//
//    STATUS = AsyncExecutionStatus.FAILED;
//
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testUpdateScheduleLocationDisabledPrompting() throws Exception {
//
//    final JobManager jobManager = new JobManager();
//
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//    final UUID folderId = UUID.randomUUID();
//
//    final Response response = jobManager.confirmSchedule( uuid.toString(), true, false, folderId.toString(), "test" );
//    assertEquals( 404, response.getStatus() );
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testUpdateScheduleLocationWrongID() {
//
//    final UUID folderId = UUID.randomUUID();
//    final JobManager jobManager = new JobManager( true, 1000, 1000, true );
//
//    Response response =
//      jobManager.confirmSchedule( UUID.randomUUID().toString(), true, false, folderId.toString(), "test" );
//    assertEquals( 404, response.getStatus() );
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testLogout() {
//
//    final TestExecutor executor = (TestExecutor) PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    executor.onLogout( session );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getStatus( uuid.toString() );
//    assertEquals( 404, response.getStatus() );
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testLogoutAnotherSession() {
//
//    final TestExecutor executor = (TestExecutor) PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//    executor.onLogout( new StandaloneSession( "another" ) );
//    final JobManager jobManager = new JobManager();
//    final Response response = jobManager.getStatus( uuid.toString() );
//    assertEquals( 200, response.getStatus() );
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleTwiceNoJobManager() {
//
//    final CountDownLatch countDownLatch = new CountDownLatch( 1 );
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock =
//      new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null", AuditWrapper.NULL ) {
//
//        @Override public IFixedSizeStreamingContent call() throws Exception {
//          countDownLatch.await();
//          return new NullSizeStreamingContent();
//        }
//      };
//
//
//    final UUID uuid = executor.addTask( mock, session );
//    assertTrue( executor.schedule( uuid, session ) );
//    assertFalse( executor.schedule( uuid, session ) );
//    countDownLatch.countDown();
//
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleNoSession() throws Exception {
//    final boolean[] flag = { false };
//    PentahoSessionHolder.removeSession();
//    new WriteToJcrTask( mockExec(), mock( InputStream.class ) ) {
//      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
//        if ( outputFolder == null ) {
//          flag[ 0 ] = true;
//        }
//        return null;
//      }
//    }.call();
//    assertTrue( flag[ 0 ] );
//  }
//
//  @Test
//  public void testRecalcFinished() throws Exception {
//    final JobManager jobManager = new JobManager( true, 1000, 1000, true );
//
//    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
//    final IAsyncReportExecution mock = mockExec();
//
//    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
//
//
//    STATUS = AsyncExecutionStatus.FINISHED;
//
//    final JobManager.ExecutionContext context = jobManager.getContext( uuid.toString() );
//    assertTrue( context.needRecalculation( true ) );
//
//
//    STATUS = AsyncExecutionStatus.FAILED;
//  }
//
//  @Test public void testCancelNoListener() {
//
//    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, 0 );
//    final PentahoAsyncReportExecution pentahoAsyncReportExecution = new PentahoAsyncReportExecution( "junit-path",
//      component, handler, session, "not null", AuditWrapper.NULL ) {
//      @Override
//      public void notifyTaskQueued( final UUID id, final List<? extends ReportProgressListener> callbackListeners ) {
//
//      }
//    };
//
//    exec.addTask( pentahoAsyncReportExecution, session );
//    //Works ok without listener
//    pentahoAsyncReportExecution.cancel();
//  }
//
//
//  @Test public void testCancelNotInterruptable() {
//
//    try {
//
//      final CountDownLatch latch = new CountDownLatch( 1 );
//
//      final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, 0 );
//      final PentahoAsyncReportExecution pentahoAsyncReportExecution = new PentahoAsyncReportExecution( "junit-path",
//        component, handler, session, "not null", AuditWrapper.NULL ) {
//
//        @Override public IFixedSizeStreamingContent call() throws Exception {
//          latch.await();
//          return new NullSizeStreamingContent();
//        }
//      };
//
//      final UUID uuid = UUID.randomUUID();
//
//      exec.addTask( pentahoAsyncReportExecution, session, uuid );
//
//      final Future future = exec.getFuture( uuid, session );
//
//      final AbstractReportProcessor processor = mock( AbstractReportProcessor.class );
//
//      ReportProcessorThreadHolder.setProcessor( processor );
//
//      future.cancel( false );
//
//      pentahoAsyncReportExecution.getListener()
//        .reportProcessingUpdate( new ReportProgressEvent( this, ReportProgressEvent.PAGINATING, 0, 0, 0, 0, 0, 0 ) );
//
//      verify( processor, never() ).cancel();
//
//      latch.countDown();
//    } finally {
//      ReportProcessorThreadHolder.clear();
//    }
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleConfirmNoId() throws Exception {
//    final JobManager jobManager = new JobManager();
//    final Response post =
//      jobManager.confirmSchedule( UUID.randomUUID().toString(), true, false, UUID.randomUUID().toString(), "test" );
//    assertEquals( post.getStatus(), 404 );
//  }
//
//
//  @SuppressWarnings( "unchecked" )
//  @Test
//  public void testScheduleConfirmNoNewName() throws Exception {
//    final JobManager jobManager = new JobManager();
//    final Response post = jobManager.confirmSchedule( UUID.randomUUID().toString(), true, false, null, "test" );
//    assertEquals( post.getStatus(), 404 );
//  }
//
//  private PentahoAsyncReportExecution mockExec() {
//    return new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
//      AuditWrapper.NULL ) {
//      @Override
//      protected AsyncReportStatusListener createListener( final UUID id,
//                                                          final List<? extends ReportProgressListener>
//                                                            callbackListener ) {
//        return new AsyncReportStatusListener( getReportPath(), id, getMimeType(), callbackListener );
//      }
//
//      @Override public IAsyncReportState getState() {
//        return AsyncIT.getState();
//      }
//
//      @Override public boolean schedule() {
//        STATUS = AsyncExecutionStatus.SCHEDULED;
//        return true;
//      }
//    };
//  }
//
//  private static IAsyncReportState getState() {
//    return new IAsyncReportState() {
//
//      @Override public String getPath() {
//        return PATH;
//      }
//
//      @Override public UUID getUuid() {
//        return UUID.randomUUID();
//      }
//
//      @Override public AsyncExecutionStatus getStatus() {
//        return STATUS;
//      }
//
//      @Override public int getProgress() {
//        return PROGRESS;
//      }
//
//      @Override public int getPage() {
//        return PAGE;
//      }
//
//      @Override public int getTotalPages() {
//        return 0;
//      }
//
//      @Override public int getGeneratedPage() {
//        return 0;
//      }
//
//      @Override public int getRow() {
//        return 0;
//      }
//
//      @Override public int getTotalRows() {
//        return 0;
//      }
//
//      @Override public String getActivity() {
//        return null;
//      }
//
//      @Override public String getMimeType() {
//        return MIME;
//      }
//
//      @Override public String getErrorMessage() {
//        return null;
//      }
//
//      @Override public boolean getIsQueryLimitReached() {
//        return false;
//      }
//
//    };
//  }
//
//  public static class TestExecutor extends PentahoAsyncExecutor<AsyncReportState> {
//
//    public TestExecutor( final int capacity, final int autoSchedulerThreshold ) {
//      super( capacity, autoSchedulerThreshold );
//    }
//
//    @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
//                                                                  final IAsyncReportExecution<? extends
//                                                                    IAsyncReportState> runningTask ) {
//
//      final FakeLocation fakeLocation = new FakeLocation();
//      final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
//      try {
//        when( contentRepository.getRoot() ).thenReturn( fakeLocation );
//      } catch ( final ContentIOException e ) {
//        e.printStackTrace();
//      }
//      return new WriteToJcrTask( runningTask, result.getStream() ) {
//        @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
//          return contentRepository;
//        }
//      };
//
//    }
//  }
//
//}
