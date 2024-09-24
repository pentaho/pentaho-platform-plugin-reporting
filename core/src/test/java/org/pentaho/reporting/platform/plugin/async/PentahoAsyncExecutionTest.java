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

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by dima.prokopenko@gmail.com on 2/17/2016.
 */
public class PentahoAsyncExecutionTest {

  @Rule public Timeout globalTimeout = new Timeout( 10000 );

  IPentahoSession userSession = mock( IPentahoSession.class );
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
  IFixedSizeStreamingContent input;

  MasterReport report = mock( MasterReport.class );
  ModifiableConfiguration configuration = mock( ModifiableConfiguration.class );
  private int autoSchedulerThreshold = 0;
  File temp;

  @Before
  public void before() throws Exception {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();

    PentahoSessionHolder.setSession( userSession );

    temp = File.createTempFile( "junit", "tmp" );
    input = new AsyncJobFileStagingHandler.FixedSizeStagingContent( temp );

    when( handler.getStagingContent() ).thenReturn( input );
    when( report.getReportConfiguration() ).thenReturn( configuration );
    when( component.getReport() ).thenReturn( report );
    when( userSession.getId() ).thenReturn( "junit" );
    when( userSession.getName() ).thenReturn( "junit" );
    final ISecurityHelper iSecurityHelper = mock( ISecurityHelper.class );
    when( iSecurityHelper.runAsUser( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocation ) throws Throwable {
        final Object call = ( (Callable) invocation.getArguments()[ 1 ] ).call();
        return call;
      }
    } );

    SecurityHelper.setMockInstance( iSecurityHelper );
  }

  @After
  public void after() {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
  }

  @Test
  public void testListenerSuccessExecution() throws Exception {
    when( component.execute() ).thenReturn( true );

    PentahoAsyncReportExecution exec = createMockCallable();

    // simulates queuing
    exec.notifyTaskQueued( UUID.randomUUID(), Collections.<ReportProgressListener>emptyList() );

    assertEquals( AsyncExecutionStatus.QUEUED, exec.getState().getStatus() );

    IFixedSizeStreamingContent returnStream = exec.call();

    assertEquals( AsyncExecutionStatus.FINISHED, exec.getState().getStatus() );
    assertEquals( input, returnStream );

    verify( handler, times( 1 ) ).getStagingContent();
  }

  private PentahoAsyncReportExecution createMockCallable() {
    return new PentahoAsyncReportExecution( "junit-path", component, handler, userSession, "not null",
      AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( UUID id,
                                                          List<? extends ReportProgressListener> callbackListener ) {
        return new AsyncReportStatusListener( "display_path", id, "text/html", callbackListener );
      }
    };
  }

  @Test
  public void testListenerFailExecution() throws Exception {
    when( component.execute() ).thenReturn( false );

    PentahoAsyncReportExecution exec = createMockCallable();
    // simulates queuing
    exec.notifyTaskQueued( UUID.randomUUID(), Collections.<ReportProgressListener>emptyList() );

    assertEquals( AsyncExecutionStatus.QUEUED, exec.getState().getStatus() );

    IFixedSizeStreamingContent returnStream = exec.call();

    assertEquals( AsyncExecutionStatus.FAILED, exec.getState().getStatus() );
    assertFalse( returnStream.equals( input ) );

    //For cleanup
    verify( handler, times( 1 ) ).getStagingContent();
    assertFalse( temp.exists() );
  }

  @Test
  public void testCancellationFeature() throws InterruptedException {
    final AtomicBoolean run = new AtomicBoolean( false );

    PentahoAsyncReportExecution spy = this.getSleepingSpy( run, this.userSession );

    PentahoAsyncExecutor executor = new PentahoAsyncExecutor( 13, autoSchedulerThreshold );
    UUID id = executor.addTask( spy, this.userSession );

    Thread.sleep( 100 );
    Future<IFixedSizeStreamingContent> fu = executor.getFuture( id, this.userSession );
    assertFalse( fu.isDone() );

    fu.cancel( true );
    verify( spy, times( 1 ) ).cancel();

    assertTrue( fu.isCancelled() );
  }

  @Test
  public void testOnLogout() {
    // mock 2 separate sessions
    IPentahoSession session1 = mock( IPentahoSession.class );
    IPentahoSession session2 = mock( IPentahoSession.class );
    UUID sessionUid1 = UUID.randomUUID();
    UUID sessionUid2 = UUID.randomUUID();
    UUID uuid1 = UUID.randomUUID();
    UUID uuid2 = UUID.randomUUID();
    when( session1.getId() ).thenReturn( sessionUid1.toString() );
    when( session2.getId() ).thenReturn( sessionUid2.toString() );

    PentahoSessionHolder.setSession( session1 );
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10, autoSchedulerThreshold );

    AtomicBoolean run = new AtomicBoolean( false );
    PentahoAsyncReportExecution napster = getSleepingSpy( run, session1 );
    assertEquals( "Task created for fist session", session1, napster.safeSession );

    // invoke never ending execution:
    UUID id1 = exec.addTask( napster, session1 );

    // invoke logout for incorrect session
    PentahoSystem.invokeLogoutListeners( session2 );

    Future<IFixedSizeStreamingContent> input1 = exec.getFuture( id1, session1 );
    assertNotNull( input1 );
    assertFalse( input1.isCancelled() );

    // invoke logout for correct session
    PentahoSystem.invokeLogoutListeners( session1 );
    Future<IFixedSizeStreamingContent> input11 = exec.getFuture( id1, session1 );

    // unable to get link to a canceled future
    assertNull( input11 );

    // but since we ALREADY has a link to it we can check cancel status:
    assertTrue( input1.isCancelled() );
  }

  @Test
  public void testRequestedPage() throws Exception {
    final AsyncReportStatusListener listener = mock( AsyncReportStatusListener.class );
    final PentahoAsyncReportExecution execution =
      new PentahoAsyncReportExecution( "junit-path", component, handler, userSession, "id", AuditWrapper.NULL ) {

        @Override
        protected AsyncReportStatusListener createListener( UUID instanceId,
                                                            List<? extends ReportProgressListener> callbackListeners ) {
          return listener;
        }
      };
    execution.notifyTaskQueued( UUID.randomUUID(), Collections.<ReportProgressListener>emptyList() );
    execution.requestPage( 500 );
    verify( listener, times( 1 ) ).setRequestedPage( 500 );
  }

  @Test
  public void testDefaultWrapper() {
    final AbstractAsyncReportExecution<IAsyncReportState> abstractAsyncReportExecution =
      new AbstractAsyncReportExecution<IAsyncReportState>( "junit-path", component, handler, userSession, "id" ) {
        @Override public IFixedSizeStreamingContent call() throws Exception {
          return null;
        }

        @Override public IAsyncReportState getState() {
          return null;
        }
      };
    assertEquals( abstractAsyncReportExecution.getAudit(), AuditWrapper.NULL );
  }

  @Test( expected = IllegalStateException.class )
  public void testAlreadyQueued() {
    final AbstractAsyncReportExecution<IAsyncReportState> abstractAsyncReportExecution =
      new AbstractAsyncReportExecution<IAsyncReportState>( "junit-path", component, handler, userSession, "id" ) {
        @Override public IFixedSizeStreamingContent call() throws Exception {
          return null;
        }

        @Override public IAsyncReportState getState() {
          return null;
        }
      };
    abstractAsyncReportExecution.notifyTaskQueued( UUID.randomUUID(), Collections.emptyList() );
    abstractAsyncReportExecution.notifyTaskQueued( UUID.randomUUID(), Collections.emptyList() );
  }

  @Test
  @SuppressWarnings( "unchecked" )
  public void testCancelScheduled() {
    final AbstractAsyncReportExecution<IAsyncReportState> abstractAsyncReportExecution =
      new AbstractAsyncReportExecution<IAsyncReportState>( "junit-path", component, handler, userSession, "id" ) {
        @Override public IFixedSizeStreamingContent call() throws Exception {
          return null;
        }

        @Override public IAsyncReportState getState() {
          return null;
        }
      };
    abstractAsyncReportExecution.notifyTaskQueued( UUID.randomUUID(), Collections.emptyList() );
    assertTrue( abstractAsyncReportExecution.schedule() );
    assertFalse( abstractAsyncReportExecution.delegate( mock( ListenableFuture.class ) ).cancel( true ) );
  }

  @Test
  public void testPreSchedule() {
    final PentahoAsyncReportExecution execution =
      new PentahoAsyncReportExecution( "junit-path", component, handler, userSession, "id", AuditWrapper.NULL );
    execution.notifyTaskQueued( UUID.randomUUID(), Collections.emptyList() );
    assertEquals( AsyncExecutionStatus.QUEUED, execution.getState().getStatus() );
    assertTrue( execution.preSchedule() );
    assertEquals( AsyncExecutionStatus.PRE_SCHEDULED, execution.getState().getStatus() );
    assertTrue( execution.schedule() );
    assertEquals( AsyncExecutionStatus.SCHEDULED, execution.getState().getStatus() );
    assertFalse( execution.preSchedule() );
  }

  @Test
  public void testCancelNoListener() {
    final AbstractAsyncReportExecution<IAsyncReportState> abstractAsyncReportExecution =
      new AbstractAsyncReportExecution<IAsyncReportState>( "junit-path", component, handler, userSession, "id" ) {
        @Override public IFixedSizeStreamingContent call() throws Exception {
          return null;
        }

        @Override public IAsyncReportState getState() {
          return null;
        }
      };
    abstractAsyncReportExecution.cancel();
  }

  private PentahoAsyncReportExecution getSleepingSpy( final AtomicBoolean run, IPentahoSession session ) {
    PentahoAsyncReportExecution exec =
      new PentahoAsyncReportExecution( "junit-path", component, handler, session, "junit", AuditWrapper.NULL ) {
        @Override
        public IFixedSizeStreamingContent call() throws Exception {
          while ( !run.get() && !Thread.currentThread().isInterrupted() ) {
            Thread.sleep( 10 );
          }
          return null;
        }
      };
    PentahoAsyncReportExecution spy = spy( exec );

    return spy;
  }


}
