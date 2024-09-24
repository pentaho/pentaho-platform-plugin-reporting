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

import junit.framework.TestCase;

import java.util.ArrayList;

public class PentahoOlap4JJndiConnectionProviderTest extends TestCase {
  PentahoOlap4JJndiConnectionProvider provider;

  protected void setUp() {
    provider = new PentahoOlap4JJndiConnectionProvider();
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
    assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoOlap4JJndiConnectionProvider", result.get( 0 ) );
    assertEquals( "jndiname", result.get( 1 ) ); //$NON-NLS-1$
    assertEquals( "username", result.get( 2 ) ); //$NON-NLS-1$
  }
}
