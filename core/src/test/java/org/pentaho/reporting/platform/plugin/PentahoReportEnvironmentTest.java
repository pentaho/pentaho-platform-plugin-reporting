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
 * Copyright (c) 2002-2017 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.Assert;
import org.junit.Test;
import org.pentaho.reporting.libraries.base.config.DefaultConfiguration;

import java.lang.reflect.Method;
import java.util.UUID;

public class PentahoReportEnvironmentTest {

  @Test( expected = NullPointerException.class )
  public void getNullProp() {
    new PentahoReportEnvironment( new DefaultConfiguration() ).getEnvironmentProperty( null );
  }

  @Test
  public void getCl() {
    final String clText = UUID.randomUUID().toString();
    Assert.assertEquals( clText, new PentahoReportEnvironment( new DefaultConfiguration(), clText )
      .getEnvironmentProperty( "contentLink" ) );
  }

  @Test
  public void testNotAppContext() {
    final String[] props = new String[] { "serverBaseURL",
      "pentahoBaseURL",
      "solutionRoot",
      "hostColonPort",
      "requestContextPath" };

    for ( final String prop : props ) {
      Assert.assertNull( new PentahoReportEnvironment( new DefaultConfiguration() )
        .getEnvironmentProperty( prop ) );
    }
  }

  @Test( expected = NullPointerException.class )
  public void testNull_1() {
    new PentahoReportEnvironment( null, "", "" );
  }

  @Test
  public void testNull_2() {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), null, "" );
    Assert.assertNotNull( pre );
    Assert.assertEquals( "", pre.getRepositoryPath() );
    Assert.assertNull( pre.getEnvironmentProperty( "contentLink" ) );
  }

  @Test
  public void testNull_3() {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", null );
    Assert.assertNotNull( pre );
    Assert.assertEquals( "", pre.getEnvironmentProperty( "contentLink" ) );
    Assert.assertNull( pre.getRepositoryPath() );
  }

  @Test
  public void testRepoPath_1() throws Exception {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", "/123/qwe/123.prpt" );
    Method m = PentahoReportEnvironment.class.getDeclaredMethod( "getRepositoryPath", new Class[] { boolean.class } );
    m.setAccessible( true );
    String result = String.valueOf( m.invoke( pre, new Object[] { false } ) );
    Assert.assertEquals( result, ":123:qwe:123.prpt" );
  }

  @Test
  public void testRepoPath_2() throws Exception {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", "/123/qwe/123.prpt" );
    Method m = PentahoReportEnvironment.class.getDeclaredMethod( "getRepositoryPath", new Class[] { boolean.class } );
    m.setAccessible( true );
    String result = String.valueOf( m.invoke( pre, new Object[] { true } ) );
    Assert.assertEquals( result, "%3A123%3Aqwe%3A123.prpt" );
  }
}
