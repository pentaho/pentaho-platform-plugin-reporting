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

package org.pentaho.reporting.platform.plugin;

import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.IPentahoSessionHolderStrategy;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dima.prokopenko@gmail.com on 2/22/2016.
 */
public class JobManagerTest extends JaxRsServerProvider {

  public static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";
  WebClient client = WebClient.create( ENDPOINT_ADDRESS );

  private static PentahoAsyncExecutor executor = null;
  private static IPentahoSession session = null;
  private static final UUID uuid = UUID.randomUUID();

  public static final String MIME = "junit_mime";
  public static final String PATH = "junit_path";
  public static int PROGRESS = -113;
  public static final AsyncExecutionStatus STATUS = AsyncExecutionStatus.FAILED;

  public static IAsyncReportState STATE;

  @Test public void testGetStatus() throws IOException {
    client.path( String.format( URL_FORMAT, UUID.randomUUID().toString(), "/status" ) );
    Response response = client.get();
    assertNotNull( response );
    assertTrue( response.hasEntity() );

    String json = response.readEntity( String.class );

    // currently no simple way to restore to AsyncReportState interface here
    // at least we get uuid in return.
    assertTrue( json.contains( uuid.toString() ) );
  }

  @BeforeClass public static void initialize() throws Exception {
    JaxRsServerProvider.initialize();

    STATE = getState();
    executor = mock( PentahoAsyncExecutor.class );
    when( executor.getReportState( any( UUID.class ), any( IPentahoSession.class ) ) ).thenReturn( STATE );

    PentahoSystem.init();
    IPentahoObjectFactory objFactory = mock( IPentahoObjectFactory.class );

    // return mock executor for any call to it's bean name.
    when( objFactory.objectDefined( anyString() ) ).thenReturn( true );
    when( objFactory.objectDefined( any( Class.class ) ) ).thenReturn( true );
    when( objFactory.get( any( Class.class ), eq( PentahoAsyncExecutor.BEAN_NAME ), any( IPentahoSession.class ) ) )
        .thenReturn( executor );

    PentahoSystem.registerObjectFactory( objFactory );
  }

  @AfterClass
  public static void afterClass() throws Exception {
    JaxRsServerProvider.destroy();
    PentahoSystem.shutdown();
  }

  public static IAsyncReportState getState() {
    return new IAsyncReportState() {

      @Override public String getPath() {
        return PATH;
      }

      @Override public UUID getUuid() {
        return uuid;
      }

      @Override public AsyncExecutionStatus getStatus() {
        return STATUS;
      }

      @Override public int getProgress() {
        return PROGRESS;
      }

      @Override public String getMimeType() {
        return MIME;
      }
    };
  }
}
