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
