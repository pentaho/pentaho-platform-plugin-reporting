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
