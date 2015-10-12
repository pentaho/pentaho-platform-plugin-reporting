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

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Properties;

import static org.mockito.Mockito.mock;

public class PentahoMondrianConnectionProviderTest extends TestCase {
  PentahoMondrianConnectionProvider provider;

  protected void setUp() {
    provider = new PentahoMondrianConnectionProvider();
  }

  public void testGetConnectionHash() throws Exception {
    ArrayList result = (ArrayList) provider.getConnectionHash( mock( Properties.class ) );

    assertEquals( 3, result.size() );
    assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoMondrianConnectionProvider",
      result.get( 0 ) ); //$NON-NLS-1$
  }
}
