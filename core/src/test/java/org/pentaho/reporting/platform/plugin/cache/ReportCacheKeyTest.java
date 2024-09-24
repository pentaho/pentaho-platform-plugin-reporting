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
