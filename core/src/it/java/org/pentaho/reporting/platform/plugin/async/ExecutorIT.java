/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.SpringIT;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ExecutorIT extends SpringIT {

  @Autowired
  IPentahoAsyncExecutor<IAsyncReportState> executor;

  @Autowired
  SimpleReportingComponent simpleReportingComponent;

  @Autowired
  JobManager jobManager;

  private MicroPlatform microPlatform = MicroPlatformFactory.create();

  @Before
  public void start() throws PlatformInitializationException {
    microPlatform.start();
  }

  @After
  public void stop() throws PlatformInitializationException {
    microPlatform.stop();
  }


  @Test( expected = IllegalStateException.class )
  public void testRecalcNoTask() {
    executor.recalculate( UUID.randomUUID(), new StandaloneSession() );
  }


  @Test
  public void testRecalc() throws IOException, ExecutionException, InterruptedException {
    try {
      final IPluginManager pluginManager = mock( IPluginManager.class );
      PentahoSystem.registerObject( pluginManager, IPluginManager.class );
      when( pluginManager.getPluginSetting( "reporting", "settings/query-limit", "0" ) ).thenReturn( "50" );
      final StandaloneSession session = new StandaloneSession( "test" );
      PentahoSessionHolder.setSession( session );
      final UUID uuid = UUID.randomUUID();
      final AsyncJobFileStagingHandler asyncJobFileStagingHandler =
        new AsyncJobFileStagingHandler( session );
      simpleReportingComponent.setReportDefinitionPath( "target/test/resource/solution/test/reporting/100rows.prpt" );
      simpleReportingComponent.setInputs( Collections.singletonMap( "query-limit", 500 ) );

      executor
        .addTask(
          new PentahoAsyncReportExecution( "target/test/resource/solution/test/reporting/100rows.prpt", simpleReportingComponent,
            asyncJobFileStagingHandler, session, UUID.randomUUID().toString(), new AuditWrapper() ), session, uuid );
      IAsyncReportState reportState = executor.getReportState( uuid, session );
      while ( reportState.getTotalRows() < 1 ) {
        reportState = executor.getReportState( uuid, session );
      }
      assertEquals( 100, reportState.getTotalRows() );
      final UUID recalculate = executor
        .recalculate( uuid, session );

      assertFalse( uuid.equals( recalculate ) );

      IAsyncReportState recalcState = executor.getReportState( recalculate, session );
      while ( recalcState.getTotalRows() < 1 ) {
        recalcState = executor.getReportState( recalculate, session );
      }

      assertEquals( 100, recalcState.getTotalRows() );


    } finally {
      PentahoSessionHolder.removeSession();
    }
  }


  @Test
  public void testNeedRecalc()
    throws IOException, ExecutionException, InterruptedException, JobManager.ContextFailedException {
    try {
      final IPluginManager pluginManager = mock( IPluginManager.class );
      PentahoSystem.registerObject( pluginManager, IPluginManager.class );
      when( pluginManager.getPluginSetting( "reporting", "settings/query-limit", "0" ) ).thenReturn( "50" );
      final StandaloneSession session = new StandaloneSession( "test" );
      PentahoSessionHolder.setSession( session );
      final UUID uuid = UUID.randomUUID();
      final AsyncJobFileStagingHandler asyncJobFileStagingHandler =
        new AsyncJobFileStagingHandler( session );
      simpleReportingComponent.setReportDefinitionPath( "target/test/resource/solution/test/reporting/100rows.prpt" );
      simpleReportingComponent.setInputs( Collections.singletonMap( "query-limit", 500 ) );

      executor
        .addTask(
          new PentahoAsyncReportExecution( "target/test/resource/solution/test/reporting/100rows.prpt", simpleReportingComponent,
            asyncJobFileStagingHandler, session, UUID.randomUUID().toString(), new AuditWrapper() ), session, uuid );
      IAsyncReportState reportState = executor.getReportState( uuid, session );
      while ( reportState.getTotalRows() < 1 ) {
        reportState = executor.getReportState( uuid, session );
      }
      assertEquals( 100, reportState.getTotalRows() );

      assertTrue( jobManager.getContext( uuid.toString() ).needRecalculation( false ) );


    } finally {
      PentahoSessionHolder.removeSession();
    }
  }


}
