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
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.IPentahoSessionHolderStrategy;

/**
 * Created by dima.prokopenko@gmail.com on 2/23/2016.
 */
public class JaxRsServerProvider{

  protected static Server server;
  protected final static String ENDPOINT_ADDRESS = "local://junit";

  public static final void startServer() throws Exception {
    JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
    sf.setResourceClasses( JobManager.class );
    SingletonResourceProvider factory = new SingletonResourceProvider( new JobManager(), false );
    sf.setResourceProvider( JobManager.class, factory );
    sf.setAddress( ENDPOINT_ADDRESS );
    server = sf.create();
  }

  @BeforeClass public static void initialize() throws Exception {
    startServer();
  }

  @AfterClass public static void destroy() throws Exception {
    server.stop();
    server.destroy();
  }
}
