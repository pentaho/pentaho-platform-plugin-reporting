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


package org.pentaho.reporting.platform.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JobManagerResponseCodeTest {


  @Test public void testEchoStatusCode() throws Exception {
    final JobManager jobManager = new JobManager();
    final Response response = jobManager.getConfig();

    assertNotNull( response );
    assertEquals( 200, response.getStatus() );

    final String json = response.readEntity( String.class );
    final ObjectMapper objectMapper = new ObjectMapper();
    final Map config = objectMapper.readValue( json, Map.class );
    assertEquals( Boolean.TRUE, config.get( "supportAsync" ) );
    assertTrue( 500 == (int) config.get( "pollingIntervalMilliseconds" ) );
  }

  @Test public void testAddJobIncorrectContentUUID() throws IOException {
    final JobManager jobManager = new JobManager();
    final Response response = jobManager.getContent( "notauuid" );
    assertNotNull( response );
    assertEquals( 404, response.getStatus() );
  }
}
