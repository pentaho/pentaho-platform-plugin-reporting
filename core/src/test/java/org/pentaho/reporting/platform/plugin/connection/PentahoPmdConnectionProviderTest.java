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
package org.pentaho.reporting.platform.plugin.connection;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;

public class PentahoPmdConnectionProviderTest {

  @BeforeClass
  public static void beforeClass() throws Exception {
  }

  @AfterClass
  public static void afterClass() {
  }

  @Before
  public void before() {
  }

  @Test
  public void testCreateConnection() throws SQLException {
    PentahoJndiDatasourceConnectionProvider jndicp = Mockito.mock( PentahoJndiDatasourceConnectionProvider.class );
    PentahoPoolDataSourceConnectionProvider poolcp = Mockito.mock( PentahoPoolDataSourceConnectionProvider.class );
    PentahoPmdConnectionProvider ppcp = Mockito.spy( new PentahoPmdConnectionProvider() );
    when( ppcp.getJndiProvider() ).thenReturn( jndicp );
    when( ppcp.getPoolProvider() ).thenReturn( poolcp );
    when( jndicp.createConnection( any(), any() ) ).thenReturn( (Connection) Mockito.mock( Connection.class ) );
    when( poolcp.createConnection( any(), any() ) ).thenReturn( (Connection) Mockito.mock( Connection.class ) );
    String username = "user";
    String password = "pass";
    DatabaseMeta databaseMeta = mock( DatabaseMeta.class );
    when( databaseMeta.getAccessType() ).thenReturn( DatabaseMeta.TYPE_ACCESS_JNDI );
    when( databaseMeta.getDatabaseName() ).thenReturn( "test" );
    when( databaseMeta.isUsingConnectionPool() ).thenReturn( false );
    try {
      Connection conn = ppcp.createConnection( databaseMeta, username, password  );
      Assert.assertFalse( "JNDI connection", conn == null );
    } catch ( ReportDataFactoryException e ) {
      Assert.fail();
    }
    when( databaseMeta.getAccessType() ).thenReturn( DatabaseMeta.TYPE_ACCESS_NATIVE );
    when( databaseMeta.getName() ).thenReturn( "test" );
    when( databaseMeta.isUsingConnectionPool() ).thenReturn( true );
    try {
      Connection conn = ppcp.createConnection( databaseMeta, username, password  );
      Assert.assertFalse( "Pool connection", conn == null );
    } catch ( ReportDataFactoryException e ) {
      Assert.fail();
    }
  }
}
