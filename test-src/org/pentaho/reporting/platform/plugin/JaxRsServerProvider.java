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

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import java.util.UUID;


public final class JaxRsServerProvider {

  private static Server server;
  private final String ENDPOINT_ADDRESS = "local://" + UUID.randomUUID().toString();
  private static JAXRSServerFactoryBean sf;
  private WebClient client;

  public void startServer( final JobManager jobManager ) throws Exception {
    sf = new JAXRSServerFactoryBean();
    sf.setResourceClasses( JobManager.class );
    final SingletonResourceProvider factory = new SingletonResourceProvider( jobManager, false );
    sf.setResourceProvider( JobManager.class, factory );
    sf.setAddress( ENDPOINT_ADDRESS );
    server = sf.create();
  }


  public WebClient getFreshClient() {
    if ( client != null ) {
      try {
        client.close();
        client = null;
      } catch ( final Exception ignored ) {

      }
    }
    client = WebClient.create( ENDPOINT_ADDRESS );
    return client;
  }


  public void stopServer() {
    server.stop();
    server.destroy();
    sf = null;
  }
}
