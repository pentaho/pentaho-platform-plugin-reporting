/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.libraries.base.util.PerformanceLoggingStopWatch;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dima.prokopenko@gmail.com on 4/26/2016.
 */
public class PluginPerfomanceMonitorContextTest {

  private static String TAG = "junit_tag";
  private static String sessionId = "any_junit_session id";
  private static String sessionName = "Jhon Doe";
  private static String requestId = "any_iddqd";
  private static String perfomanceMetric = "[very slow]";

  private IPentahoSession session = mock( IPentahoSession.class );

  @Before
  public void before() {
    when( session.getId() ).thenReturn( sessionId );
    when( session.getName() ).thenReturn( sessionName );

    PentahoSessionHolder.setSession( session );
    ReportListenerThreadHolder.setRequestId( requestId );
  }

  @Test
  public void createStopWatchNoMessageTest() {
    PluginPerfomanceMonitorContext context = new PluginPerfomanceMonitorContext();
    PerformanceLoggingStopWatch sw = context.createStopWatch( TAG );
    String actual = String.valueOf( sw.getMessage() );

    assertEquals( "null sessionid[any_junit_session id] userid[Jhon Doe] requestid[any_iddqd]", actual );
  }

  @Test
  public void createStopWatchMessageTest() {
    PluginPerfomanceMonitorContext context = new PluginPerfomanceMonitorContext();
    PerformanceLoggingStopWatch sw = context.createStopWatch( TAG, perfomanceMetric );
    String actual = String.valueOf( sw.getMessage() );

    assertEquals( "[very slow] sessionid[any_junit_session id] userid[Jhon Doe] requestid[any_iddqd]", actual );
  }

  /**
   * Test for null session and null request Id we will not fall into NPE for logging.
   */
  @Test
  public void createStopWatchNoSessionNoIdTest() {
    PentahoSessionHolder.setSession( null );
    ReportListenerThreadHolder.setRequestId( null );

    PluginPerfomanceMonitorContext context = new PluginPerfomanceMonitorContext();
    PerformanceLoggingStopWatch sw = context.createStopWatch( TAG, perfomanceMetric );
    String actual = String.valueOf( sw.getMessage() );

    assertEquals( "[very slow] sessionid[null] userid[null] requestid[null]", actual );
  }

  @AfterClass
  public static void afterClass() {
    PentahoSessionHolder.removeSession();
    ReportListenerThreadHolder.clear();
  }
}
