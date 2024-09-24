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

package org.pentaho.reporting.platform.plugin.cache;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ReportCacheKeyTest extends TestCase {
  ReportCacheKey rck;
  Map<String, Object> parameter;

  @Before
  public void setUp() {
    parameter = new HashMap<String, Object>();
    rck = new ReportCacheKey( "", parameter );
  }

  @Test
  public void testHashCode() throws Exception {
    assertEquals( 0, rck.hashCode() );
    rck.addParameter( "param", "value" );
    assertEquals( 1403851013, rck.hashCode() );
  }

  @Test
  public void testGetSessionId() throws Exception {
    assertEquals( "", rck.getSessionId() );
    rck = new ReportCacheKey( "sessionId", parameter );
    assertEquals( "sessionId", rck.getSessionId() );
  }

  @Test
  public void testEquals() throws Exception {
    assertTrue( rck.equals( rck ) );
    assertFalse( rck.equals( "" ) );
    assertTrue( rck.equals( new ReportCacheKey( "", parameter ) ) );
    assertFalse( rck.equals( new ReportCacheKey( "sessionId", parameter ) ) );
  }
}
