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

package org.pentaho.reporting.platform.plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.ws.rs.core.Response;
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
