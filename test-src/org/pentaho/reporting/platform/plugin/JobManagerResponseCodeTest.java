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
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by dima.prokopenko@gmail.com on 2/23/2016.
 */
public class JobManagerResponseCodeTest extends JaxRsServerProvider {

  public static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";
  WebClient client = WebClient.create( ENDPOINT_ADDRESS );

  @Test public void testEchoStatusCode() throws Exception {
    client.path( String.format( URL_FORMAT, "isasync", "" ) );
    Response response = client.get();

    assertNotNull( response );
    assertEquals( 200, response.getStatus() );

    assertTrue( response.readEntity( Boolean.class ) );
  }

  @Test public void testAddJobIncorrectContentUUID() {
    client.path( String.format( URL_FORMAT, "123", "/content" ) );
    Response response = client.post( null );
    assertNotNull( response );
    assertEquals( 404, response.getStatus() );
  }
}
