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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith( PowerMockRunner.class )
@PowerMockIgnore( "jdk.internal.reflect.*" )
@PrepareForTest( PentahoSessionHolder.class )
public class ReservedIdIT {


  private MicroPlatform microPlatform;
  private static final IPentahoSession session = Mockito.mock( IPentahoSession.class );


  @Before
  public synchronized void setUp() throws Exception {
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( "IJobIdGenerator", new JobIdGenerator() );
    microPlatform.start();
  }

  @After
  public synchronized void tearDown() throws PlatformInitializationException {
    PentahoSystem.shutdown();
    microPlatform.stop();
    microPlatform = null;
  }

  @Test
  public void testReserveId() throws Exception {

    PowerMockito.mockStatic( PentahoSessionHolder.class );

    when( PentahoSessionHolder.getSession() ).thenReturn( session );

    assertEquals( session, PentahoSessionHolder.getSession() );

    final JobManager jobManager = new JobManager();


    final Response response = jobManager.reserveId();

    assertEquals( 200, response.getStatus() );

    assertNotNull( String.valueOf( response.getEntity() ).contains( "reservedId" ) );

  }


  @Test
  public void testReserveIdNoSession() throws Exception {

    PowerMockito.mockStatic( PentahoSessionHolder.class );

    when( PentahoSessionHolder.getSession() ).thenReturn( null );
    final JobManager jobManager = new JobManager();
    assertNull( PentahoSessionHolder.getSession() );

    final Response response = jobManager.reserveId();
    assertEquals( 404, response.getStatus() );
  }


  @Test
  public void testReserveIdNoGenerator() throws Exception {

    tearDown();

    microPlatform = MicroPlatformFactory.create();

    microPlatform.start();

    PowerMockito.mockStatic( PentahoSessionHolder.class );

    when( PentahoSessionHolder.getSession() ).thenReturn( session );

    assertEquals( session, PentahoSessionHolder.getSession() );

    final JobManager jobManager = new JobManager();


    final Response response = jobManager.reserveId();

    assertEquals( 404, response.getStatus() );
  }

}
