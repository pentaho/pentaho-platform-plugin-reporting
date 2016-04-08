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

import junit.framework.Assert;
import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
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
 * <p>
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

  IApplicationContext context = mock( IApplicationContext.class );
  Random bytes = new Random();

  final IFixedSizeStreamingContent input =
    new AsyncJobFileStagingHandler.FixedSizeStagingContent( new NullInputStream( 0 ), 0 );

  @Before public void before() throws Exception {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();

    when( handler.getStagingContent() ).thenReturn( input );
    when( report.getReportConfiguration() ).thenReturn( configuration );
    when( component.getReport() ).thenReturn( report );

    when( session1.getId() ).thenReturn( sessionUid1.toString() );
    when( session2.getId() ).thenReturn( sessionUid2.toString() );

    String tempFolder = System.getProperty( "java.io.tmpdir" );
    Path junitPrivate = Paths.get( tempFolder ).resolve( "JUNIT_" + UUID.randomUUID().toString() );
    junitPrivate.toFile().deleteOnExit();

    when( context.getSolutionPath( anyString() ) ).thenReturn( junitPrivate.toString() );
    PentahoSystem.setApplicationContext( context );
  }

  @After
  public void after() {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
  }

  @Test public void testCanCompleteTask() throws Exception {
    when( component.execute() ).thenReturn( true );
    //PentahoSessionHolder.setSession( session1 );

    PentahoAsyncReportExecution task1 = createMockCallable( session1 );

    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 10 );
    UUID id = exec.addTask( task1, session1 );
    Future<IFixedSizeStreamingContent> result = exec.getFuture( id, session1 );
    while ( result.isDone() ) {
      Thread.sleep( 150 );
    }
    IFixedSizeStreamingContent resultInput = result.get();
    assertEquals( input.getStream(), resultInput.getStream() );
  }

  @Test public void testCorrectFuturePerSessionRetrival() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );

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
    return new PentahoAsyncReportExecution( "junit-path", component, handler, session, null ) {
      @Override
      protected AsyncReportStatusListener createListener( final UUID id ) {
        final AsyncReportState state = new AsyncReportState( id, getReportPath() );
        final AsyncReportStatusListener retval = mock( AsyncReportStatusListener.class );
        when( retval.getState() ).thenReturn( state );
        return retval;
      }
    };
  }

  @Test public void testCorrectStatePerSessionRetrieval() {
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );

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
    PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );
    AsyncJobFileStagingHandler handler1 = new AsyncJobFileStagingHandler( session1 );
    AsyncJobFileStagingHandler handler2 = new AsyncJobFileStagingHandler( session2 );

    Path stagingFolder = AsyncJobFileStagingHandler.getStagingDirPath();
    // folders created on async file staging constructor call:
    assertTrue( stagingFolder.toFile().list().length == 2 );


    byte[] anyByte = new byte[ 1024 ];
    bytes.nextBytes( anyByte );
    handler1.getStagingOutputStream().write( anyByte );
    handler1.getStagingOutputStream().close();
    bytes.nextBytes( anyByte );
    handler2.getStagingOutputStream().write( anyByte );
    handler2.getStagingOutputStream().close();

    String[] folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 2 );

    exec.onLogout( session1 );

    folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 1 );
    assertTrue( folders[ 0 ].equals( session2.getId() ) );

    exec.onLogout( session2 );
    folders = stagingFolder.toFile().list();
    assertTrue( folders.length == 0 );
  }


  @Test public void testSchedule() {
    final PentahoAsyncExecutor exec = new PentahoAsyncExecutor( 1 );

    final TestListener testListener = new TestListener( "1", UUID.randomUUID(), "" );

    // must have two separate instances, as callable holds unique ID and listener for each addTask(..)
    final UUID id1 = exec.addTask(  new PentahoAsyncReportExecution( "junit-path",
            component, handler, PentahoSessionHolder.getSession(), null ) {
      @Override
      protected AsyncReportStatusListener createListener(final UUID id) {
        return testListener;
      }
    }, session1 );

    exec.schedule( id1, session1 );

    final IAsyncReportState state = exec.getReportState( id1, session1 );
    assertNotNull( state );
    assertEquals( AsyncExecutionStatus.SCHEDULED, testListener.getState().getStatus() );

  }
}
