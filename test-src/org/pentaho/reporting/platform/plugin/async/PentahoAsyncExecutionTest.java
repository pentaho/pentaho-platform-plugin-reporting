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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.pentaho.di.core.util.Assert.assertFalse;

/**
 * Created by dima.prokopenko@gmail.com on 2/17/2016.
 */
public class PentahoAsyncExecutionTest {

  @Rule public Timeout globalTimeout = new Timeout( 10000 );

  IPentahoSession userSession = mock( IPentahoSession.class );
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
  InputStream input = new NullInputStream( 0 );

  MasterReport report = mock( MasterReport.class );
  ModifiableConfiguration configuration = mock( ModifiableConfiguration.class );

  @Before
  public void before() throws Exception {
    when( handler.getStagingContent() ).thenReturn( input );
    when( report.getReportConfiguration() ).thenReturn( configuration );
    when( component.getReport() ).thenReturn( report );
  }

  @Test
  public void testListenerSuccessExecution() throws Exception {
    when( component.execute() ).thenReturn( true );

    PentahoAsyncReportExecution
        exec = new PentahoAsyncReportExecution( "junit-path", component, handler );
    AsyncReportStatusListener listner = new AsyncReportStatusListener( "display_path", UUID.randomUUID(), "text/html" );

    exec.setListener( listner );

    assertEquals( AsyncExecutionStatus.QUEUED, exec.getState().getStatus() );

    InputStream returnStream = exec.call();

    assertEquals( AsyncExecutionStatus.FINISHED, exec.getState().getStatus() );
    assertEquals( input, returnStream );

    verify( handler, times(1) ).getStagingContent();
  }

  @Test
  public void testListenerFailExecution() throws Exception {
    when( component.execute() ).thenReturn( false );

    PentahoAsyncReportExecution
        exec = new PentahoAsyncReportExecution( "junit-path", component, handler );
    AsyncReportStatusListener listener = new AsyncReportStatusListener( "display_path", UUID.randomUUID(), "text/html" );

    exec.setListener( listener );
    assertEquals( AsyncExecutionStatus.QUEUED, exec.getState().getStatus() );

    InputStream returnStream = exec.call();

    assertEquals( AsyncExecutionStatus.FAILED, exec.getState().getStatus() );
    assertFalse( returnStream.equals( input ) );

    verify( handler, times(0) ).getStagingContent();
  }

  @Test
  public void testCancellationFeature() throws InterruptedException {
    final AtomicBoolean run = new AtomicBoolean( false );

    PentahoAsyncReportExecution spy = this.getSleepingSpy( run );

    PentahoAsyncExecutor executor = new PentahoAsyncExecutor( 13 );
    UUID id = executor.addTask( spy, this.userSession );

    Thread.sleep( 100 );
    Future<InputStream> fu = executor.getFuture( id, this.userSession );
    assertFalse( fu.isDone() );

    fu.cancel( true );
    verify( spy, times( 1 )).cancel();

    assertTrue( fu.isCancelled() );
  }

  private PentahoAsyncReportExecution getSleepingSpy( final AtomicBoolean run ) {
    PentahoAsyncReportExecution
        exec = new PentahoAsyncReportExecution( "junit-path", component, handler ) {
      @Override
      public InputStream call() throws Exception {
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
