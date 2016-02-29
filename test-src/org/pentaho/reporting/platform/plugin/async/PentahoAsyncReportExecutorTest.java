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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
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

  @Rule public Timeout globalTimeout = new Timeout( 10000 );

  IPentahoSession session1 = mock( IPentahoSession.class );
  IPentahoSession session2 = mock( IPentahoSession.class );
  UUID sessionUid1 = UUID.randomUUID();
  UUID sessionUid2 = UUID.randomUUID();
  UUID uuid1 = UUID.randomUUID();
  UUID uuid2 = UUID.randomUUID();

  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
  MasterReport report = mock( MasterReport.class );
  ModifiableConfiguration configuration = mock( ModifiableConfiguration.class );

  final InputStream input = new NullInputStream( 0 );

  PentahoAsyncReportExecution
      task1 = new PentahoAsyncReportExecution( "junit-path", component, handler );

  @Before public void before() throws Exception {
    when( handler.getStagingContent() ).thenReturn( input );
    when( report.getReportConfiguration() ).thenReturn( configuration );
    when( component.getReport() ).thenReturn( report );

    when( session1.getId() ).thenReturn( sessionUid1.toString() );
    when( session2.getId() ).thenReturn( sessionUid2.toString() );

    task1.setListener( mock( AsyncReportStatusListener.class ) );
  }

  @Test public void testCanCompleteTask() throws Exception {
    when( component.execute() ).thenReturn( true );

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
}
