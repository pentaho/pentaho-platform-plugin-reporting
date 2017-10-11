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
import java.util.Properties;
import java.util.concurrent.Callable;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISecurityHelper;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;


import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PentahoMondrianConnectionProviderTest {

  private static IPentahoSession session = mock( IPentahoSession.class );
  private static IUserRoleListService userRoleListService = mock( IUserRoleListService.class );
  private PentahoMondrianConnectionProvider provider = new PentahoMondrianConnectionProvider();

  @BeforeClass
  public static void beforeClass() throws Exception {
    PentahoSessionHolder.setSession( session );
    PentahoSystem.registerObject( userRoleListService );
    final ISecurityHelper iSecurityHelper = mock( ISecurityHelper.class );
    when( iSecurityHelper.runAsUser( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( InvocationOnMock invocation ) throws Throwable {
        final Object call = ( (Callable) invocation.getArguments()[ 1 ] ).call();
        return call;
      }
    } );

    SecurityHelper.setMockInstance( iSecurityHelper );
  }

  @AfterClass
  public static void afterClass() {
    PentahoSessionHolder.setSession( null );
  }

  @Before
  public void before() {
    when( session.getName() ).thenReturn( "karasik" );
  }

  @Test
  public void testGetConnectionHash() throws Exception {
    ArrayList result = (ArrayList) provider.getConnectionHash( mock( Properties.class ) );

    assertEquals( 3, result.size() );
    assertEquals( "org.pentaho.reporting.platform.plugin.connection.PentahoMondrianConnectionProvider",
      result.get( 0 ) ); //$NON-NLS-1$
  }
}
