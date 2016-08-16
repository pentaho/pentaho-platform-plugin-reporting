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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.JaxRsServerProvider;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.core.Response;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith( PowerMockRunner.class )
@PrepareForTest( PentahoSessionHolder.class )
public class LocationPromptingIT {

  private JaxRsServerProvider provider;
  private AsyncExecutionStatus STATUS;
  private MicroPlatform microPlatform;

  @Before
  public void setUp() throws Exception {
    STATUS = AsyncExecutionStatus.QUEUED;
    provider = new JaxRsServerProvider();
    provider.startServer( new JobManager( true, 1000, 1000, true ) );
    final PentahoAsyncExecutor<AsyncReportState> executor = spy( new AsyncIT.TestExecutor( 2, 100 ) );
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( "IPentahoAsyncExecutor", executor );

  }

  @After
  public void tearDown() throws Exception {
    microPlatform.stop();
    provider.stopServer();
    provider = null;
  }

  @Test
  public void testRecalcFinished() throws Exception {
    PowerMockito.mockStatic( PentahoSessionHolder.class );

    final IPentahoSession session = mock( IPentahoSession.class );
    when( session.getId() ).thenReturn( "test" );
    when( session.getName() ).thenReturn( "test" );
    when( PentahoSessionHolder.getSession() ).thenReturn( session );


    final CountDownLatch latch = new CountDownLatch( 1 );


    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportState reportState = mock( IAsyncReportState.class );
    when( reportState.getStatus() ).thenAnswer( a -> getStatus() );
    final IAsyncReportExecution execution =
      new PentahoAsyncReportExecution( "test", mock( SimpleReportingComponent.class ),
        mock( AsyncJobFileStagingHandler.class ), session, "test", AuditWrapper.NULL ) {
        @Override public IAsyncReportState getState() {
          return reportState;
        }

        @Override public IFixedSizeStreamingContent call() throws Exception {
          return new NullSizeStreamingContent();
        }
      };


    final UUID uuid = executor.addTask( execution, PentahoSessionHolder.getSession() );

    final WebClient client = provider.getFreshClient();
    final UUID folderId = UUID.randomUUID();
    final String config = String.format( AsyncIT.URL_FORMAT, uuid, "/schedule" );
    client.path( config );
    client.query( "confirm", true );
    client.query( "folderId", folderId );
    client.query( "newName", "test" );
    client.query( "recalculateFinished", "true" );

    STATUS = AsyncExecutionStatus.FINISHED;

    latch.countDown();


    final Response response = client.post( null );
    assertEquals( 200, response.getStatus() );
    verify( executor, times( 1 ) ).recalculate( any(), any() );
    verify( executor, times( 1 ) ).schedule( any(), any() );
    verify( executor, times( 1 ) )
      .updateSchedulingLocation( any(), any(), any(), any() );


  }

  public AsyncExecutionStatus getStatus() {
    return STATUS;
  }
}
