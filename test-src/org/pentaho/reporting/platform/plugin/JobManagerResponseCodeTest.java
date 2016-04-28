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
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Map;

import static org.junit.Assert.*;

public class JobManagerResponseCodeTest  {

  private static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";

  private static JaxRsServerProvider provider;

  @BeforeClass
  public static void init() throws Exception {
    provider = new JaxRsServerProvider();
    provider.startServer( new JobManager(  ) );
  }

  @AfterClass
  public static void destroy() throws Exception {
    provider.stopServer();
  }


  @Test public void testEchoStatusCode() throws Exception {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, "config", "" ) );
    final Response response = client.get();

    assertNotNull( response );
    assertEquals( 200, response.getStatus() );

    final String json = response.readEntity( String.class );
    final ObjectMapper objectMapper = new ObjectMapper();
    final Map config = objectMapper.readValue( json, Map.class );
    assertEquals( Boolean.TRUE, config.get( "supportAsync" ) );
    assertTrue( 500 == (int) config.get( "pollingIntervalMilliseconds" ) );
  }

  @Test public void testAddJobIncorrectContentUUID() {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, "123", "/content" ) );
    final Response response = client.post( null );
    assertNotNull( response );
    assertEquals( 404, response.getStatus() );
  }
}
