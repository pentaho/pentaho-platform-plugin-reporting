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

package org.pentaho.reporting.platform.plugin.connection;

import static org.mockito.ArgumentMatchers.any;
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
