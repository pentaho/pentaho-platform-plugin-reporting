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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoSession;

import javax.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.pentaho.di.core.util.Assert.assertFalse;
import static org.pentaho.di.core.util.Assert.assertNotNull;
import static org.pentaho.di.core.util.Assert.assertTrue;
import static org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor.CompositeKey;

/**
 * Tests uses concurrency and potentially can hang. See test timeout rule (globalTimeout).
 *
 * Created by dima.prokopenko@gmail.com on 2/17/2016.
 */
public class PentahoAsyncReportExecutorTest {

  //@Rule public Timeout globalTimeout = new Timeout( 10000 );

  IPentahoSession session1 = mock( IPentahoSession.class );
  IPentahoSession session2 = mock( IPentahoSession.class );
  UUID sessionUid1 = UUID.randomUUID();
  UUID sessionUid2 = UUID.randomUUID();
  UUID uuid1 = UUID.randomUUID();
  UUID uuid2 = UUID.randomUUID();

  final InputStream input = new NullInputStream( 0 );

  PentahoAsyncReportExecution task1 = mock( PentahoAsyncReportExecution.class );

  @Before public void before() throws Exception {
    when( session1.getId() ).thenReturn( sessionUid1.toString() );
    when( session2.getId() ).thenReturn( sessionUid2.toString() );
    when( task1.call() ).thenReturn( input );
  }

  @Test public void testCanCompleteTask() throws InterruptedException, ExecutionException {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10 );
    UUID id = exec.addTask( task1, session1 );
    Future<InputStream> result = exec.getFuture( id, session1 );
    while ( result.isDone() ) {
      Thread.sleep( 150 );
    }
    InputStream resultInput = result.get();
    assertEquals( input, resultInput );
  }

  @Test public void testCorrectFuturePerSessionRetrival() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );

    UUID id1 = exec.addTask( task1, session1 );
    UUID id2 = exec.addTask( task1, session2 );

    Future<InputStream> r1 = exec.getFuture( id1, session1 );
    assertNotNull( r1 );

    // incorrect session
    Future<InputStream> r2 = exec.getFuture( id2, session1 );
    assertNull( r2 );

    // incorrect id
    r2 = exec.getFuture( id1, session2 );
    assertNull( r2 );

    // should be ok now
    r2 = exec.getFuture( id2, session2 );
    assertNotNull( r2 );
  }

  @Test public void testCorrectStatePerSessionRetrieval() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );

    UUID id1 = exec.addTask( task1, session1 );
    UUID id2 = exec.addTask( task1, session2 );

    AsyncReportState state = exec.getReportState( id1, session1 );
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

  /**
   * Waiting for Future cancel implementation.
   *
   * @throws Exception
   */
  @Test @Ignore public void testOnLogoutListner() throws Exception {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10 );

    AtomicBoolean switch1 = new AtomicBoolean( false );
    AtomicBoolean switch2 = new AtomicBoolean( false );
    AtomicBoolean switch3 = new AtomicBoolean( false );
    AtomicBoolean switch4 = new AtomicBoolean( false );

    PentahoAsyncReportExecution task1 = getControlledStub( switch1 );
    PentahoAsyncReportExecution task2 = getControlledStub( switch2 );
    PentahoAsyncReportExecution task3 = getControlledStub( switch3 );
    PentahoAsyncReportExecution task4 = getControlledStub( switch4 );

    // started for session 1
    UUID id1 = exec.addTask( task1, session1 );
    UUID id2 = exec.addTask( task2, session1 );

    // started for session 2
    UUID id3 = exec.addTask( task3, session2 );
    UUID id4 = exec.addTask( task4, session2 );

    // session is ended:
    exec.onLogout( session2 );

    fail( "Future cancel not implemented yet." );
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

  public PentahoAsyncReportExecution getControlledStub( final AtomicBoolean run ) throws Exception {
    PentahoAsyncReportExecution execution = mock( PentahoAsyncReportExecution.class );

    when( execution.call() ).thenAnswer( new Answer() {
      @Override public InputStream answer( InvocationOnMock invocationOnMock ) throws Throwable {
        while ( !run.get() ) {
          Thread.sleep( 100 );
        }
        return input;
      }
    } );


    return execution;
  }
}
