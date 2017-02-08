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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import org.pentaho.reporting.libraries.base.config.DefaultConfiguration;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PentahoReportEnvironmentTest {

  @Test( expected = NullPointerException.class )
  public void getNullProp() {
    new PentahoReportEnvironment( new DefaultConfiguration() ).getEnvironmentProperty( null );
  }

  @Test
  public void getCl() {
    final String clText = UUID.randomUUID().toString();
    assertEquals( clText, new PentahoReportEnvironment( new DefaultConfiguration(), clText )
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
      assertNull( new PentahoReportEnvironment( new DefaultConfiguration() )
        .getEnvironmentProperty( prop ) );
    }

  }


}
