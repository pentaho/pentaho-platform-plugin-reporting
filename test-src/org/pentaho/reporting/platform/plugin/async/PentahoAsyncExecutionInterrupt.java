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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/**
 * Created by dima.prokopenko@gmail.com on 2/24/2016.
 */
public class PentahoAsyncExecutionInterrupt {

  private MicroPlatform microPlatform;
  private String stagingDir = System.getProperty( "java.io.tmpdir" );
  private PentahoAsyncExecutor executor = new PentahoAsyncExecutor( 2 );

  @Before public void before() throws PlatformInitializationException {
    new File( "./resource/solution/system/tmp" ).mkdirs();
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @Test public void testInterrupt() throws IOException, InterruptedException {
    IPentahoSession session = PentahoSessionHolder.getSession();

    SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    // ...point to report
    reportComponent.setReportFileId( "resource/solution/test/reporting/100000_rows.prpt" );
    AsyncJobFileStagingHandler
        handler =
        new AsyncJobFileStagingHandler( session );
    PentahoAsyncReportExecution
        task = new PentahoAsyncReportExecution( "junit", reportComponent, handler );

    UUID id = executor.addTask( task, session );

    // ...asuming it is already started
    Thread.sleep( 100 );

    Future<InputStream> future = executor.getFuture( id, session );

    future.cancel( true );

    IAsyncReportState state = executor.getReportState( id, session );

    assertEquals( AsyncExecutionStatus.CANCELED, state.getStatus() );
  }

  @After public void tearDown() throws Exception {
    microPlatform.stop();
  }
}
