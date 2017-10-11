/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.reporting.engine.classic.core.ReportInterruptedException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class PentahoAsyncExecutionAuditTest {

  private static final String url = "junit url";
  private static final String auditId = "auditId";
  private static final String sessionId = "sessionId";
  private static final String sessionName = "junitName";

  private static final UUID uuid = UUID.randomUUID();

  private SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  private AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );

  private IPentahoSession session = mock( IPentahoSession.class );
  private AuditWrapper wrapper = mock( AuditWrapper.class );

  private OutputStream outputStream = mock( OutputStream.class );

  @Before
  public void before() throws Exception {
    when( session.getId() ).thenReturn( sessionId );
    when( session.getName() ).thenReturn( sessionName );
    when( handler.getStagingContent() ).thenReturn( new AbstractAsyncReportExecution.NullSizeStreamingContent() );
    final ISecurityHelper iSecurityHelper = mock( ISecurityHelper.class );
    when( iSecurityHelper.runAsUser( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocation ) throws Throwable {
        final Object call = ( (Callable) invocation.getArguments()[ 1 ] ).call();
        return call;
      }
    } );

    SecurityHelper.setMockInstance( iSecurityHelper );
  }

  @Test
  public void testSuccessExecutionAudit() throws Exception {
    final PentahoAsyncReportExecution execution =
      new PentahoAsyncReportExecution( url, component, handler, session, auditId, wrapper );
    execution.notifyTaskQueued( uuid, Collections.emptyList() );

    //this is successful story
    when( component.execute() ).thenReturn( true );

    execution.call();

    verify( wrapper, Mockito.times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( execution.getClass().getName() ),
      startsWith( execution.getClass().getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( auditId ),
      eq( "" ),
      eq( (float) 0 ),
      any( ILogger.class )
    );

    verify( wrapper, Mockito.times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( execution.getClass().getName() ),
      startsWith( execution.getClass().getName() ),
      eq( MessageTypes.INSTANCE_END ),
      eq( auditId ),
      eq( "" ),
      anyFloat(), // hope more than 0
      any( ILogger.class )
    );
  }

  @Test
  public void testFailedExecutionAudit() throws Exception {
    final PentahoAsyncReportExecution execution =
      new PentahoAsyncReportExecution( url, component, handler, session, auditId, wrapper );
    execution.notifyTaskQueued( uuid, Collections.emptyList() );

    //this is sad story
    when( component.execute() ).thenReturn( false );

    execution.call();

    // we always log instance start for every execution attempt
    verify( wrapper, Mockito.times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( execution.getClass().getName() ),
      startsWith( execution.getClass().getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( auditId ),
      eq( "" ),
      eq( (float) 0 ),
      any( ILogger.class )
    );

    // no async reports for this case.
    verify( wrapper, Mockito.times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      eq( execution.getClass().getName() ),
      eq( execution.getClass().getName() ),
      eq( MessageTypes.FAILED ),
      eq( auditId ),
      eq( "" ),
      eq( (float) 0 ),
      any( ILogger.class )
    );
  }

  /**
   * We need a special wrapper that will be able to get id from one thread (created for report execution) and made this
   * value accessible in contest of this junit test thread.
   *
   * @throws Exception
   */
  @Test
  @SuppressWarnings( "unchecked" )
  public void testInstanceIdIsSet() throws Exception {

    final CountDownLatch latch = new CountDownLatch( 1 );
    final ThreadSpyAuditWrapper wrapper = new ThreadSpyAuditWrapper( latch );

    final String expected = UUID.randomUUID().toString();

    final PentahoAsyncReportExecution execution =
      new PentahoAsyncReportExecution( url, component, handler, session, expected, wrapper );

    final PentahoAsyncExecutor<IAsyncReportState> executor = new PentahoAsyncExecutor<>( 2, 0 );
    executor.addTask( execution, session );

    latch.await();

    synchronized ( latch ) {
      assertEquals( expected, wrapper.capturedId );
    }
  }

  /**
   * This is quite intricate test, but make attempt to inspect thread pool executor as much detailed as possible.
   *
   * @throws Exception
   */
  @Test
  public void testCancellationStatusTest() throws Exception {

    final int CAPACITY = 4;
    final int NUMBER_OF_THREADS = CAPACITY + 1;

    // controlled execution:
    final CountDownLatch startLatch = new CountDownLatch( 1 );
    final CountDownLatch waitSubmitted = new CountDownLatch( CAPACITY );
    final CountDownLatch finalBlock = new CountDownLatch( CAPACITY );

    // this is one more hack - we waiting on a handler's method that will be called in finally block
    // this way synchronizing on catch() block was executed.
    final Answer<OutputStream> handlerAnswer = invocation -> {
      finalBlock.countDown();
      return outputStream;
    };
    when( handler.getStagingOutputStream() ).thenAnswer( handlerAnswer );

    // hello java 8!
    final Answer answer = invocation -> {
      waitSubmitted.countDown();
      startLatch.await( 10, TimeUnit.SECONDS );
      throw new ReportInterruptedException( "Bang" );
    };

    final Stack<UUID> tasksIds = new Stack<>();
    final PentahoAsyncExecutor<IAsyncReportState> executor = new PentahoAsyncExecutor<>( CAPACITY, 0 );

    // add one more excessive execution
    UUID id;
    for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
      final SimpleReportingComponent component = mock( SimpleReportingComponent.class );
      // every time component.execute() called - we do await on answer object
      // we wait on a latch exactly number of submitted threads
      when( component.execute() ).thenAnswer( answer );

      final PentahoAsyncReportExecution execution =
        new PentahoAsyncReportExecution( url, component, handler, session, auditId, wrapper );
      id = executor.addTask( execution, session );

      // we save id's in order they would submitted
      assertNotNull( id );
      tasksIds.push( id );
    }

    // wait for all working tasks will hang on monitor 'startLatch'
    // at least we have to have 4 tasks waited for a startLatch
    waitSubmitted.await( 10, TimeUnit.SECONDS );

    final IAsyncReportState state5 = executor.getReportState( tasksIds.peek(), session );
    assertEquals( "4 running, one waiting", AsyncExecutionStatus.QUEUED, state5.getStatus() );

    // now collect all futures to a one place
    List<ListenableFuture<IFixedSizeStreamingContent>> futures = new ArrayList<>();
    final List<IAsyncReportState> states = new ArrayList<>();
    do {
      // this is a cheat: we know this futures really ListenableFuture.
      // if you have class cast here... will require additional code to
      // wait fot their completition, see code below...
      futures.add( (ListenableFuture<IFixedSizeStreamingContent>) executor.getFuture( tasksIds.peek(), session ) );
      states.add( executor.getReportState( tasksIds.pop(), session ) );
    } while ( !tasksIds.isEmpty() );

    assertEquals( "we have 5: 4 running and one waiting future", NUMBER_OF_THREADS, futures.size() );

    // we manually explore futures statuses
    int done = 0;
    int cancelled = 0;
    // we have 5 running and no cancelled (one running is queued)
    for ( final ListenableFuture<IFixedSizeStreamingContent> item : futures ) {
      if ( item.isCancelled() ) {
        cancelled++;
      } else if ( item.isDone() ) {
        done++;
      }
    }
    assertEquals( "none done since all waiting on a latch", 0, done );
    assertEquals( "still none cancelled", 0, cancelled );

    // simulate on-logout call - will attempt to cancel all tasks.
    executor.onLogout( session );

    // again - do a detailed insepction
    done = 0;
    cancelled = 0;
    // we have 5 running and no cancelled (one running is queued)
    for ( final ListenableFuture<IFixedSizeStreamingContent> item : futures ) {
      if ( item.isCancelled() ) {
        cancelled++;
      } else if ( item.isDone() ) {
        done++;
      }
    }
    assertEquals( "none done", 0, done );
    assertEquals( "cancelled all", NUMBER_OF_THREADS, cancelled );

    // after all futures seems to be cancelled - let try to complete them.
    final ListenableFuture<List<IFixedSizeStreamingContent>> all = Futures.successfulAsList( futures );
    // now - release all - if someone was not cancelled - it make attempt to execute successfully.
    startLatch.countDown();
    // wait for if any is still pending
    all.get();

    // wait here before ALL threads come into final block
    finalBlock.await( 10, TimeUnit.SECONDS );

    verify( wrapper, atLeast( CAPACITY ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      eq( PentahoAsyncReportExecution.class.getName() ),
      eq( PentahoAsyncReportExecution.class.getName() ),
      eq( MessageTypes.CANCELLED ),
      eq( auditId ),
      eq( "" ),
      anyFloat(),
      any( ILogger.class )
    );
  }

  /**
   * This is almost a copy of #testCancellationStatusTest above, but now we have one 'scheduled' execution.
   */
  @Test
  public void testScheduledExecutionsNotGetCancelled() throws Exception {
   /* SecurityHelper.setMockInstance( mock( ISecurityHelper.class ) );*/

    // we still have 4 threads capacity
    final int CAPACITY = 4;
    // but now one of the threads is scheduled
    final int NUMBER_OF_THREADS = CAPACITY + 2;

    // controlled execution:
    final CountDownLatch startLatch = new CountDownLatch( 1 );
    final CountDownLatch waitSubmitted = new CountDownLatch( CAPACITY );
    final CountDownLatch finalBlock = new CountDownLatch( NUMBER_OF_THREADS - 1 );

    final Answer<OutputStream> handlerAnswer = invocation -> {
      finalBlock.countDown();
      return outputStream;
    };
    when( handler.getStagingOutputStream() ).thenAnswer( handlerAnswer );

    final int[] doThrow = { 0 };

    // hello java 8!
    final Answer answer = invocation -> {

      waitSubmitted.countDown();
      startLatch.await( 10, TimeUnit.SECONDS );
      synchronized ( this ) {
        if ( doThrow[ 0 ] < NUMBER_OF_THREADS - 1 ) {
          doThrow[ 0 ]++;
          throw new ReportInterruptedException( "Bang" );
        } else {
          return true;
        }
      }

    };

    final Stack<UUID> tasksIds = new Stack<>();
    final PentahoAsyncExecutor<IAsyncReportState> executor = new PentahoAsyncExecutor<IAsyncReportState>( CAPACITY, 0 ) {
      @Override protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                                    final IAsyncReportExecution runningTask ) {
        return () -> null;
      }
    };

    UUID id;
    for ( int i = 0; i < NUMBER_OF_THREADS; i++ ) {
      final SimpleReportingComponent component = mock( SimpleReportingComponent.class );
      when( component.execute() ).thenAnswer( answer );
      final PentahoAsyncReportExecution execution =
        new PentahoAsyncReportExecution( url, component, handler, session, auditId, wrapper );
      id = executor.addTask( execution, session );
      assertNotNull( id );
      tasksIds.push( id );
    }
    waitSubmitted.await( 10, TimeUnit.SECONDS );

    // remember - retrieve but does not remove
    final IAsyncReportState state5 = executor.getReportState( tasksIds.peek(), session );
    assertEquals( "4 running, one waiting", AsyncExecutionStatus.QUEUED, state5.getStatus() );

    // what we doing now - is attempt to schedule a peek of queue.
    // this is creates 'queued + scheduled' execution
    executor.schedule( tasksIds.peek(), session );

    final List<ListenableFuture<IFixedSizeStreamingContent>> futures = new ArrayList<>();
    do {
      futures.add( (ListenableFuture<IFixedSizeStreamingContent>) executor.getFuture( tasksIds.pop(), session ) );
    } while ( !tasksIds.isEmpty() );

    assertEquals( "we have running and waiting futures", NUMBER_OF_THREADS, futures.size() );

    int done = 0;
    int cancelled = 0;
    for ( final ListenableFuture<IFixedSizeStreamingContent> item : futures ) {
      if ( item.isCancelled() ) {
        cancelled++;
      } else if ( item.isDone() ) {
        done++;
      }
    }
    assertEquals( "none done since all waiting on a latch", 0, done );
    assertEquals( "still none cancelled", 0, cancelled );

    // simulate on-logout call - will attempt to cancel all tasks.
    // remember - now we have one scheduled execution - so it should survive onLogout call
    executor.onLogout( session );

    final ListenableFuture<List<IFixedSizeStreamingContent>> all = Futures.successfulAsList( futures );
    startLatch.countDown();
    all.get();

    // again - do a detailed inspection
    done = 0;
    cancelled = 0;
    // we have 5 running and no cancelled (one running is queued)
    for ( final ListenableFuture<IFixedSizeStreamingContent> item : futures ) {
      if ( item.isCancelled() ) {
        cancelled++;
      } else if ( item.isDone() ) {
        done++;
      }
    }
    assertEquals( "scheduled is done:", 1, done );
    assertEquals( "have cancelled all, BUT SCHEDULED!", NUMBER_OF_THREADS - 1, cancelled );

    finalBlock.await( 10, TimeUnit.SECONDS );

    //All started
    verify( wrapper, times( 6 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      eq( MessageTypes.INSTANCE_START ),
      eq( auditId ),
      eq( "" ),
      anyFloat(),
      any( ILogger.class )
    );

    verify( wrapper, atLeast( CAPACITY ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      eq( MessageTypes.CANCELLED ),
      eq( auditId ),
      eq( "" ),
      anyFloat(),
      any( ILogger.class )
    );

    // this is ONE sucesfull execution for scheduled task since it was not cancelled!!!
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( url ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      startsWith( PentahoAsyncReportExecution.class.getName() ),
      eq( MessageTypes.INSTANCE_END ),
      eq( auditId ),
      eq( "" ),
      anyFloat(),
      any( ILogger.class )
    );
  }


  private static class ThreadSpyAuditWrapper extends AuditWrapper {

    volatile String capturedId;
    private final CountDownLatch latch;

    ThreadSpyAuditWrapper( final CountDownLatch latch ) {
      this.latch = latch;
    }

    @Override
    public void audit( final String instanceId, final String userId, String actionName, final String objectType,
                       final String processId, final String messageType, final String message, final String value,
                       final float duration,
                       final ILogger logger ) {
      synchronized ( latch ) {
        latch.countDown();
        capturedId = ReportListenerThreadHolder.getRequestId();
      }

    }
  }
}
