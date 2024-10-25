/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import com.google.common.io.CharStreams;
import junit.framework.Assert;
import net.jcip.annotations.NotThreadSafe;
import org.junit.*;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.pentaho.commons.util.repository.exception.RuntimeException;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.engine.classic.core.event.async.AsyncExecutionStatus;
import org.pentaho.reporting.engine.classic.core.event.async.AsyncReportState;
import org.pentaho.reporting.engine.classic.core.event.async.AsyncReportStatusListener;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportState;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor.CompositeKey;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Tests uses concurrency and potentially can hang. See test timeout rule (globalTimeout).
 * <p>
 * Created by dima.prokopenko@gmail.com on 2/17/2016.
 */
@RunWith( MockitoJUnitRunner.class )
@NotThreadSafe
public class PentahoAsyncReportExecutorTest {

  @Rule public Timeout globalTimeout = new Timeout( 10000 );

  public static final int MILLIS = 1500;

  IPentahoSession session1 = mock( IPentahoSession.class );
  IPentahoSession session2 = mock( IPentahoSession.class );
  UUID sessionUid1 = UUID.randomUUID();
  UUID sessionUid2 = UUID.randomUUID();
  UUID uuid1 = UUID.randomUUID();
  UUID uuid2 = UUID.randomUUID();

  static final String MAGIC = "13";

  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
  MasterReport report = mock( MasterReport.class );
  ModifiableConfiguration configuration = mock( ModifiableConfiguration.class );

  IApplicationContext context = mock( IApplicationContext.class );
  Random bytes = new Random();

  IFixedSizeStreamingContent input;
  private int autoSchedulerThreshold = 0;
  private IUnifiedRepository repository;
  File temp;

  private static IApplicationContext backupContext;

  @BeforeClass
  public static void backup() {
    backupContext = PentahoSystem.getApplicationContext();
  }

  @AfterClass
  public static void restore() {
    PentahoSystem.setApplicationContext( backupContext );
  }

  @Before
  public void before() throws Exception {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();

    temp = File.createTempFile( "junit", "tmp" );
    FileOutputStream fout = new FileOutputStream( temp );
    fout.write( MAGIC.getBytes() );
    fout.flush();
    fout.close();

    input = new AsyncJobFileStagingHandler.FixedSizeStagingContent( temp );

    when( handler.getStagingContent() ).thenReturn( input );
    lenient().when( report.getReportConfiguration() ).thenReturn( configuration );
    lenient().when( component.getReport() ).thenReturn( report );

    when( session1.getId() ).thenReturn( sessionUid1.toString() );
    when( session2.getId() ).thenReturn( sessionUid2.toString() );
    when( session1.getName() ).thenReturn( "test" );
    when( session2.getName() ).thenReturn( "test" );

    String tempFolder = System.getProperty( "java.io.tmpdir" );
    Path junitPrivate = Paths.get( tempFolder ).resolve( "JUNIT_" + UUID.randomUUID().toString() );
    junitPrivate.toFile().deleteOnExit();

    when( context.getSolutionPath( any() ) ).thenReturn( junitPrivate.toString() );

    PentahoSystem.setApplicationContext( context );
    final ISecurityHelper iSecurityHelper = mock( ISecurityHelper.class );
    when( iSecurityHelper.runAsUser( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocation ) throws Throwable {
        final Object call = ( (Callable) invocation.getArguments()[ 1 ] ).call();
        return call;
      }
    } );

    SecurityHelper.setMockInstance( iSecurityHelper );

    repository = mock( IUnifiedRepository.class );
    final ISchedulingDirectoryStrategy strategy = mock( ISchedulingDirectoryStrategy.class );
    final RepositoryFile targetDir = mock( RepositoryFile.class );
    when( strategy.getSchedulingDir( repository ) ).thenReturn( targetDir );
    when( targetDir.getPath() ).thenReturn( "/test" );
    final RepositoryFile file = mock( RepositoryFile.class );
    when( repository.getFile( startsWith( "/test" ) ) ).thenReturn( file );
    when( file.getId() ).thenReturn( "test_id" );

    PentahoSystem.registerObject( repository, IUnifiedRepository.class );
    PentahoSystem.registerObject( strategy, ISchedulingDirectoryStrategy.class );
  }

  @After
  public void after() {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
  }

  @Test public void testCanCompleteTask() throws Exception {
    when( component.execute() ).thenReturn( true );

    PentahoAsyncReportExecution task1 = createMockCallable( session1 );


    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );
    UUID id = exec.addTask( task1, session1 );
    Future<IFixedSizeStreamingContent> result = exec.getFuture( id, session1 );
    while ( result.isDone() ) {
      Thread.sleep( MILLIS );
    }
    IFixedSizeStreamingContent resultInput = result.get();

    String actual = new String(CharStreams.toString( new InputStreamReader( input.getStream()) )) ;

    assertEquals( MAGIC, actual );
  }

  @Test public void testCorrectFuturePerSessionRetrival() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    UUID id1 = exec.addTask( createMockCallable( session1 ), session1 );
    UUID id2 = exec.addTask( createMockCallable( session2 ), session2 );
    Assert.assertFalse( id1.equals( id2 ) );

    Future<IFixedSizeStreamingContent> r1 = exec.getFuture( id1, session1 );
    assertNotNull( r1 );

    // incorrect session
    Future<IFixedSizeStreamingContent> r2 = exec.getFuture( id2, session1 );
    assertNull( r2 );

    // incorrect id
    r2 = exec.getFuture( id1, session2 );
    assertNull( r2 );

    // should be ok now
    r2 = exec.getFuture( id2, session2 );
    assertNotNull( r2 );
  }

  private PentahoAsyncReportExecution createMockCallable( IPentahoSession session ) {
    return new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener(final UUID id,
                                                         List<? extends ReportProgressListener> listenerList ) {
        final AsyncReportState state = new AsyncReportState( id, getReportPath() );
        final AsyncReportStatusListener retval = mock( AsyncReportStatusListener.class );
        when( retval.getState() ).thenReturn( state );
        return retval;
      }
    };
  }

  @Test public void testCorrectStatePerSessionRetrieval() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    UUID id1 = exec.addTask( createMockCallable( session1 ), session1 );
    UUID id2 = exec.addTask( createMockCallable( session2 ), session2 );

    IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );

    // incorrect session
    state = exec.getReportState( id2, session1 );
    assertNull( state );

    // incorrect id
    state = exec.getReportState( id1, session2 );
    assertNull( state );

    // should be ok now
    state = exec.getReportState( id2, session2 );
    assertNotNull( state );
  }

  @Test public void compositeKeyEqualsHashCodeTest() {
    CompositeKey one = new CompositeKey( session1, uuid1 );
    CompositeKey two = new CompositeKey( session2, uuid2 );
    CompositeKey three = new CompositeKey( session2, uuid2 );

    assertEquals( three, two );
    // in 4,294,967,295 * 2 + 0 probability it will fail
    assertEquals( three.hashCode(), two.hashCode() );

    assertTrue( two.equals( three ) );
    assertTrue( three.equals( two ) );
    assertEquals( two.hashCode(), three.hashCode() );

    assertFalse( one.equals( two ) );
    assertFalse( two.equals( one ) );
    assertFalse( one.hashCode() == two.hashCode() );
  }

  @Test
  public void onLogoutTest() throws IOException {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );
    final AsyncJobFileStagingHandler handler1 = new AsyncJobFileStagingHandler( session1 );
    final AsyncJobFileStagingHandler handler2 = new AsyncJobFileStagingHandler( session2 );

    final Path stagingFolder = AsyncJobFileStagingHandler.getStagingDirPath();
    // folders created on async file staging constructor call:
    assertTrue( stagingFolder.toFile().list().length == 2 );


    final byte[] anyByte = new byte[ 1024 ];
    bytes.nextBytes( anyByte );
    try ( final OutputStream stagingOutputStream = handler1.getStagingOutputStream() ) {
      stagingOutputStream.write( anyByte );
    } finally {
      handler1.getStagingContent().cleanContent();
    }

    bytes.nextBytes( anyByte );
    try ( final OutputStream stagingOutputStream = handler2.getStagingOutputStream() ) {
      stagingOutputStream.write( anyByte );
    }

    String[] folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 2 );

    exec.onLogout( session1 );

    folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 1 );
    assertTrue( folders[ 0 ].equals( session2.getId() ) );

    exec.onLogout( session2 );
    folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 1 );

    handler2.getStagingContent().cleanContent();

    exec.onLogout( session2 );
    folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 0 );
  }


  @Test public void testSchedule() {

    final CountDownLatch countDownLatch = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public IFixedSizeStreamingContent call() throws Exception {
        countDownLatch.await();
        return new NullSizeStreamingContent();
      }
    }, session1 );

    assertTrue( exec.schedule( id1, session1 ) );
    countDownLatch.countDown();

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );

  }

  @Test public void testShutdown() throws Exception {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.shutdown();

    assertNull( exec.getFuture( id1, session1 ) );
    assertNull( exec.getReportState( id1, session1 ) );
  }

  @Test public void testCleanFuture() throws Exception {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.cleanFuture( id1, session1 );

    assertNull( exec.getFuture( id1, session1 ) );
    assertNull( exec.getReportState( id1, session1 ) );
  }


  @Test public void testRequestPage() {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.requestPage( id1, session1, 100 );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( 100, testListener.getRequestedPage() );

  }

  @Test public void testRequestPageNoTask() {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.requestPage( UUID.randomUUID(), session1, 100 );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( 0, testListener.getRequestedPage() );

  }

  @Test( expected = IllegalStateException.class ) public void testScheduleAfterCallback() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold ) {


      @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                                    final IAsyncReportExecution runningTask ) {
        final FakeLocation fakeLocation = new FakeLocation();
        final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
        try {
          when( contentRepository.getRoot() ).thenReturn( fakeLocation );
        } catch ( final ContentIOException e ) {
          e.printStackTrace();
        }
        return new WriteToJcrTask( runningTask, result.getStream() ) {
          @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
            latch.countDown();
            return contentRepository;
          }
        };
      }
    };

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {
        final IFixedSizeStreamingContent call = super.call();

        return call;
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    assertTrue( exec.schedule( id1, session1 ) );


    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );


    latch.await();
    Thread.sleep( MILLIS );

    exec.schedule( id1, session1 );
  }

  @Test public void testScheduleBeforeCallback() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch( 1 );

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final PentahoAsyncReportExecution task = spy( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {
        final IFixedSizeStreamingContent call = super.call();
        latch.await();
        return call;
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    } );

    final UUID id1 = exec.addTask( task, session1 );

    assertTrue( exec.schedule( id1, session1 ) );

    Thread.sleep( MILLIS );

    latch.countDown();

    assertFalse( exec.schedule( id1, session1 ) );

  }


  @Test public void testScheduleEmptySessionName() {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final StandaloneSession namelessSession = new StandaloneSession( "" );

    final PentahoAsyncReportExecution task = spy( new PentahoAsyncReportExecution( "junit-path",
      component, handler, namelessSession, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    } );
    final UUID id1 = exec.addTask( task, namelessSession );

    assertFalse( exec.schedule( id1, namelessSession ) );

  }

  @Test( expected = IllegalStateException.class ) public void testScheduleNoTask() {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)

    final PentahoAsyncReportExecution task = new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    };
    final UUID id1 = exec.addTask( task, session1 );

    exec.schedule( UUID.randomUUID(), session1 );

  }


  @Test public void testUpdateSchedulingLocation() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public synchronized boolean schedule() {
        super.schedule();
        return true;
      }

    }, session1 );

    assertTrue( exec.schedule( id1, session1 ) );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );
    exec.updateSchedulingLocation( id1, session1, "/target", "test" );

  }

  @Test( expected = IllegalStateException.class ) public void testUpdateSchedulingLocationNoTask() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.updateSchedulingLocation( UUID.randomUUID(), session1, "/target", "test" );

  }


  @Test public void testUpdateSchedulingLocationRaceCondition() throws InterruptedException {

    final CountDownLatch latch = new CountDownLatch( 2 );

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final PentahoAsyncReportExecution task = spy( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public IFixedSizeStreamingContent call() throws Exception {
        latch.countDown();
        return super.call();
      }
    } );
    final UUID id1 = exec.addTask( task, session1 );


    exec.updateSchedulingLocation( id1, session1, "/target", "test" );


  }


  @Test public void testNotifyListeners() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch( 1 );

    final UUID uuid = UUID.randomUUID();

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold ) {
      @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                                    final IAsyncReportExecution runningTask ) {
        return () -> {
          latch.countDown();
          return uuid;
        };
      }
    };

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public synchronized boolean schedule() {
        super.schedule();
        return true;
      }
    }, session1 );

    assertTrue( exec.schedule( id1, session1 ) );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );


    latch.await();

    Thread.sleep( MILLIS );

    exec.updateSchedulingLocation( id1, session1, "/target", "test" );

    verify( repository, times( 1 ) ).getFileById( uuid );

  }

  @Ignore
  @Test public void testRequestLocationAfterCallback() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch( 1 );

    final ExecutorService executorService = Executors.newSingleThreadExecutor();

    executorService.submit( () -> {
      final CountDownLatch latch1 = new CountDownLatch( 1 );
      final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold ) {


        @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                                      final IAsyncReportExecution runningTask ) {
          final FakeLocation fakeLocation = new FakeLocation();
          final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
          try {
            when( contentRepository.getRoot() ).thenReturn( fakeLocation );
          } catch ( final ContentIOException e ) {
            e.printStackTrace();
          }
          return new WriteToJcrTask( runningTask, result.getStream() ) {
            @Override
            protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
              return contentRepository;
            }
          };
        }
      };

      final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

      // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
      final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
        component, handler, session1, "not null", AuditWrapper.NULL ) {
        @Override public IFixedSizeStreamingContent call() throws Exception {
          final IFixedSizeStreamingContent call = super.call();
          latch1.countDown();
          return call;
        }

        @Override
        protected AsyncReportStatusListener createListener( final UUID id,
                                                            List<? extends ReportProgressListener> listeners ) {
          return testListener;
        }
      }, session1 );

      assertTrue( exec.schedule( id1, session1 ) );


      final IAsyncReportState state = exec.getReportState( id1, session1 );
      assertNotNull( state );
      assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );


      exec.updateSchedulingLocation( id1, session1, "test", "test" );
      try {
        Thread.sleep( MILLIS );
      } catch ( InterruptedException e ) {
        e.printStackTrace();
      }
      latch.countDown();
    } );
    latch.await();


    verify( repository, times( 1 ) ).getFileById( "test_id" );
  }

  @Test
  public void testOnFailure() throws InterruptedException {


    final CountDownLatch latch = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );
    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {

        latch.await();
        throw new RuntimeException();
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.schedule( id1, session1 );
    assertNotNull( exec.getFuture( id1, session1 ) );
    latch.countDown();
    Thread.sleep( MILLIS );
    assertNull( exec.getFuture( id1, session1 ) );


  }


  @Test
  public void testCleanupOnFailure() throws InterruptedException {

    assertTrue( temp.exists() );
    final CountDownLatch latch = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );
    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {

        assertNotNull( handler.getStagingContent() );

        fail();
        latch.countDown();
        return null;
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    latch.await();

    assertFalse( temp.exists() );


  }


  @Test
  public void testCleanupOnLogout() throws InterruptedException {

    assertTrue( temp.exists() );
    final CountDownLatch latch = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );
    PentahoSystem.addLogoutListener( exec );
    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {
        latch.await();
        return null;
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );


    PentahoSystem.invokeLogoutListeners( session1 );
    latch.countDown();

    assertFalse( temp.exists() );


  }


  @Test
  public void testCleanupScheduled() throws InterruptedException {

    assertTrue( temp.exists() );
    final CountDownLatch latch1 = new CountDownLatch( 2 );
    final CountDownLatch latch2 = new CountDownLatch( 1 );
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 ) {
      @Override protected Callable<Serializable> getWriteToJcrTask( IFixedSizeStreamingContent result,
                                                                    IAsyncReportExecution runningTask ) {
        return () -> "test";
      }
    };
    PentahoSystem.addLogoutListener( exec );
    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override public IFixedSizeStreamingContent call() throws Exception {
        latch1.await();
        latch1.await();
        final IFixedSizeStreamingContent mock = mock( IFixedSizeStreamingContent.class );
        when( mock.cleanContent() ).thenAnswer( invocation -> {
          handler.getStagingContent().cleanContent();
          latch2.countDown();
          return null;
        } );
        return mock;
      }

      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          final List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }
    }, session1 );

    exec.schedule( id1, session1 );

    PentahoSystem.invokeLogoutListeners( session1 );

    latch1.countDown();

    assertTrue( temp.exists() );

    latch1.countDown();

    //Wait for callback
    latch2.await();

    assertFalse( temp.exists() );


  }


  @Test public void testPreSchedule() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final IAsyncReportExecution execution = mock( IAsyncReportExecution.class );
    when( execution.preSchedule() ).thenAnswer( new Answer<Boolean>() {
      @Override public Boolean answer( final InvocationOnMock invocation ) throws Throwable {
        testListener.setStatus( AsyncExecutionStatus.PRE_SCHEDULED );
        return true;
      }
    } );

    when( execution.getState() ).thenReturn( testListener.getState() );

    final UUID id1 = exec.addTask( execution, session1 );

    assertTrue( exec.preSchedule( id1, session1 ) );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.PRE_SCHEDULED, testListener.getState().getStatus() );
  }

  @Test public void testPreScheduleNoTask() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final UUID id1 = UUID.randomUUID();

    assertFalse( exec.preSchedule( id1, session1 ) );
  }


  @Test public synchronized void testRecalculate() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      component, handler, session1, "not null", AuditWrapper.NULL ) {


      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public synchronized boolean schedule() {
        super.schedule();
        return true;
      }

      @Override public IFixedSizeStreamingContent call() throws Exception {
        return new NullSizeStreamingContent();
      }


    }, session1 );

    assertTrue( exec.schedule( id1, session1 ) );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );
    final UUID recalculate = exec.recalculate( id1, session1 );
    assertNotNull( session1.getName() );
    assertTrue( exec.schedule( recalculate, session1 ) );
    final IAsyncReportState recalculateState = exec.getReportState( recalculate, session1 );
    assertNotNull( recalculateState );
    assertEquals( AsyncExecutionStatus.SCHEDULED, recalculateState.getStatus() );

  }

  @Test( expected = IllegalStateException.class ) public void testRecalculateNoTask() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = UUID.randomUUID();

    exec.recalculate( id1, session1 );

  }

  @Test public void testRecalculateJobFails() throws ResourceException, IOException {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );


    final SimpleReportingComponent reportComponent = mock( SimpleReportingComponent.class );
    when( reportComponent.getReport() ).thenReturn( report );

    final UUID id1 = exec.addTask( new PentahoAsyncReportExecution( "junit-path",
      reportComponent, handler, session1, "not null", AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id,
                                                          List<? extends ReportProgressListener> listeners ) {
        return testListener;
      }

      @Override public synchronized boolean schedule() {
        super.schedule();
        return true;
      }

      @Override public IFixedSizeStreamingContent call() throws Exception {
        return new NullSizeStreamingContent();
      }
    }, session1 );

    doAnswer( invocation -> {
      throw new Exception( "Oops!" );
    } ).when( reportComponent ).setOutputStream( any() );

    assertNull( exec.recalculate( id1, session1 ) );

  }


  @Test
  public void testProvidedUuid() {

    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1, autoSchedulerThreshold );

    final UUID uuid = UUID.randomUUID();

    final UUID task = exec.addTask( mock( IAsyncReportExecution.class ), session1, uuid );

    assertEquals( uuid, task );
  }

}

