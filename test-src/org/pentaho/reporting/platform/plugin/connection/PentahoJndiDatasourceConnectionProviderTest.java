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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.connection;

import junit.framework.TestCase;

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
    Object list = provider.getConnectionHash();
  }
}
