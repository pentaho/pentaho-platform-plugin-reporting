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
import java.util.Properties;

import static org.mockito.Mockito.mock;

public class PentahoMondrianDataSourceProviderTest extends TestCase {
  PentahoMondrianDataSourceProvider provider;

  protected void setUp() {
    provider = new PentahoMondrianDataSourceProvider( "data-source" ); //$NON-NLS-1$
  }

  public void testGetConnectionHash() throws Exception {
    ArrayList result = (ArrayList) provider.getConnectionHash(  );

    assertEquals( 2, result.size() );
    assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoMondrianDataSourceProvider",
      result.get( 0 ) ); //$NON-NLS-1$
    assertEquals( "data-source", result.get(1) );
  }
}
