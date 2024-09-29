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


package org.pentaho.reporting.platform.plugin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( Parameterized.class )
public class SimpleReportingComponentTest {


  public SimpleReportingComponentTest( final int reportLimit, final int systemLimit, final int userLimit,
                                       final int result, final boolean isLimitEnabled ) {
    this.reportLimit = reportLimit;
    this.systemLimit = systemLimit;
    this.userLimit = userLimit;
    this.result = result;
    this.isLimitEnabled = isLimitEnabled;
  }

  private boolean isLimitEnabled;
  private int reportLimit;
  private int systemLimit;
  private int userLimit;
  private int result;


  @Parameterized.Parameters
  public static Collection params() {
    //  { reportLimit, systemLimit, userLimit, result },
    return Arrays.asList( new Object[][] {
      //Report limit is set - we should return either report limit or system
      //System limit is set - return min (reportLimit, systemLimit)
      { 100, 200, -1, 100, true },
      { 100, 50, -1, 50, true },
      //System limit is not set - return report limit
      { 100, 0, -1, 100, true },
      //Report limit is not set - we should return either user limit or system
      //System limit and user limit are set - return min (reportLimit, systemLimit)
      { -1, 200, 100, 100, true },
      { -1, 50, 100, 50, true },
      //System limit is set but no user limit - return system limit
      { -1, 50, -1, 50, true },
      //System limit is not set - return user limit or -1 by default
      { -1, 0, 100, 100, true },
      //When limit is disabled
      { 100, 200, -1, 100, false },
      { 100, 50, -1, 100, false },
      { 100, 0, -1, 100, false },
      { -1, 200, 100, 100, false },
      { -1, 50, 100, 100, false },
      { -1, 50, -1, -1, false },
      { -1, 0, 100, 100, false }
    } );
  }

  @Before
  public void before() {
    final IPluginManager pluginManager = mock( IPluginManager.class );
    PentahoSystem.registerObject( pluginManager, IPluginManager.class );
    when( pluginManager.getPluginSetting( "reporting", "settings/query-limit", "0" ) )
      .thenReturn( String.valueOf( systemLimit ) );
    when( pluginManager.getPluginSetting( "reporting", "settings/query-limit-ui-enabled", "false" ) )
      .thenReturn( String.valueOf( isLimitEnabled ) );
  }

  @After
  public void after() {
    PentahoSystem.clearObjectFactory();
  }


  @Test
  public void checkAndGetUserInputQueryLimitTest() {
    assertEquals( result,
      new SimpleReportingComponent().checkAndGetUserInputQueryLimit( String.valueOf( userLimit ), reportLimit ) );
  }

}
