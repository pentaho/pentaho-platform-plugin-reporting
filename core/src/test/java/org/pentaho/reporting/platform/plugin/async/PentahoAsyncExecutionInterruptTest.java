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
 * Copyright 2006 - 2018 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dima.prokopenko@gmail.com on 2/24/2016.
 */
public class PentahoAsyncExecutionInterruptTest {

  private MicroPlatform microPlatform;
  private int autoSchedulerThreshold = 0;
  private PentahoAsyncExecutor executor = new PentahoAsyncExecutor( 2, autoSchedulerThreshold );
  private IPentahoSession session = mock( IPentahoSession.class );
  private File tmp;

  @Before
  public void before() throws PlatformInitializationException {
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
    when( session.getId() ).thenReturn( "junit" );
    PentahoSessionHolder.setSession( session );

    tmp = new File( "target/test/resource/solution/system/tmp" );
    tmp.mkdirs();
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }

  @After
  public void after() throws IOException {
    PentahoSystem.shutdown();
    PentahoSystem.clearObjectFactory();
    PentahoSessionHolder.removeSession();
    microPlatform.stop();
    microPlatform = null;
  }

  @Test public void testInterrupt() throws IOException, InterruptedException {
    SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    // ...point to report
    reportComponent.setReportFileId( "target/test/resource/solution/test/reporting/BigReport.prpt" );
    AsyncJobFileStagingHandler
      handler =
      new AsyncJobFileStagingHandler( session );
    PentahoAsyncReportExecution
      task =
      new PentahoAsyncReportExecution( "junit", reportComponent, handler, session, "not null", AuditWrapper.NULL );

    UUID id = executor.addTask( task, session );

    // ...asuming it is already started
    Thread.sleep( 100 );

    Future<IFixedSizeStreamingContent> future = executor.getFuture( id, session );

    future.cancel( true );

    IAsyncReportState state = executor.getReportState( id, session );

    assertEquals( AsyncExecutionStatus.CANCELED, state.getStatus() );
  }
}
