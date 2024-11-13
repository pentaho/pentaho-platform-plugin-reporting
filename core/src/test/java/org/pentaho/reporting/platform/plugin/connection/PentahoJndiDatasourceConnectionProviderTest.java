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

import junit.framework.TestCase;

import java.util.ArrayList;

public class PentahoJndiDatasourceConnectionProviderTest extends TestCase {
  PentahoJndiDatasourceConnectionProvider provider;

  protected void setUp() {
    provider = new PentahoJndiDatasourceConnectionProvider();
  }

  public void testSetJndiName() throws Exception {
    assertNull( provider.getJndiName() );

    provider.setJndiName( "jndi" ); //$NON-NLS-1$
    assertEquals( "jndi", provider.getJndiName() ); //$NON-NLS-1$
  }

  public void testSetUsername() throws Exception {
    assertNull( provider.getUsername() );

    provider.setUsername( "username" ); //$NON-NLS-1$
    assertEquals( "username", provider.getUsername() ); //$NON-NLS-1$
  }

  public void testSetPassword() throws Exception {
    assertNull( provider.getPassword() );

    provider.setPassword( "password" ); //$NON-NLS-1$
    assertEquals( "password", provider.getPassword() ); //$NON-NLS-1$
  }

  public void testGetConnectionHash() throws Exception {
    provider.setJndiName( "jndiname" ); //$NON-NLS-1$
    provider.setUsername( "username" ); //$NON-NLS-1$
    ArrayList result = (ArrayList) provider.getConnectionHash();
    assertEquals( 3, result.size() );
    assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoJndiDatasourceConnectionProvider", result.get( 0 ) );
    assertEquals( "jndiname", result.get( 1 ) ); //$NON-NLS-1$
    assertEquals( "username", result.get( 2 ) ); //$NON-NLS-1$
  }
}
