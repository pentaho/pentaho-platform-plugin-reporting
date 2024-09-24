/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/
package org.pentaho.reporting.platform.plugin.connection;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

public class PentahoPoolDataSourceConnectionProviderTest {
  PentahoPoolDataSourceConnectionProvider provider;

  @Before
  public void setUp() {
    provider = new PentahoPoolDataSourceConnectionProvider();
  }

  @Test
  public void testSetName() throws Exception {
    Assert.assertNull( provider.getName() );

    provider.setName( "mysql" ); //$NON-NLS-1$
    Assert.assertEquals( "mysql", provider.getName() ); //$NON-NLS-1$
  }

  @Test
  public void testSetUsername() throws Exception {
    Assert.assertNull( provider.getUsername() );

    provider.setUsername( "username" ); //$NON-NLS-1$
    Assert.assertEquals( "username", provider.getUsername() ); //$NON-NLS-1$
  }

  @Test
  public void testSetPassword() throws Exception {
    Assert.assertNull( provider.getPassword() );

    provider.setPassword( "password" ); //$NON-NLS-1$
    Assert.assertEquals( "password", provider.getPassword() ); //$NON-NLS-1$
  }

  @Test
  public void testGetConnectionHash() throws Exception {
    provider.setName( "mysql" ); //$NON-NLS-1$
    provider.setUsername( "username" ); //$NON-NLS-1$
    ArrayList result = (ArrayList) provider.getConnectionHash();
    Assert.assertEquals( 3, result.size() );
    Assert.assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoPoolDataSourceConnectionProvider", result.get( 0 ) );
    Assert.assertEquals( "mysql", result.get( 1 ) ); //$NON-NLS-1$
    Assert.assertEquals( "username", result.get( 2 ) ); //$NON-NLS-1$
  }
}
