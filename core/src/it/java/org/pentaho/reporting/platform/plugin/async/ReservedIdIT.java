/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

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
    try( MockedStatic<PentahoSessionHolder> mockedClient = mockStatic( PentahoSessionHolder.class) ) {
      mockedClient.when(PentahoSessionHolder::getSession).thenReturn( session );
      when( PentahoSessionHolder.getSession() ).thenReturn( session );
      assertEquals( session, PentahoSessionHolder.getSession() );
    }

    final JobManager jobManager = new JobManager();


    final Response response = jobManager.reserveId();

    assertEquals( 200, response.getStatus() );

    assertNotNull( String.valueOf( response.getEntity() ).contains( "reservedId" ) );

  }


  @Test
  public void testReserveIdNoSession() throws Exception {
    try( MockedStatic<PentahoSessionHolder> mockedClient = mockStatic( PentahoSessionHolder.class) ) {
      mockedClient.when(PentahoSessionHolder::getSession).thenReturn( null );
      when( PentahoSessionHolder.getSession() ).thenReturn( null );

      final JobManager jobManager = new JobManager();
      assertNull( PentahoSessionHolder.getSession() );
      final Response response = jobManager.reserveId();
      assertEquals( 404, response.getStatus() );
    }
  }


  @Test
  public void testReserveIdNoGenerator() throws Exception {

    tearDown();

    microPlatform = MicroPlatformFactory.create();

    microPlatform.start();

    try( MockedStatic<PentahoSessionHolder> mockedClient = mockStatic( PentahoSessionHolder.class) ) {
      mockedClient.when(PentahoSessionHolder::getSession).thenReturn( session );

      when( PentahoSessionHolder.getSession() ).thenReturn( session );
      assertEquals( session, PentahoSessionHolder.getSession() );
      final JobManager jobManager = new JobManager();
      final Response response = jobManager.reserveId();
      assertEquals( 404, response.getStatus() );
    }
  }

}
