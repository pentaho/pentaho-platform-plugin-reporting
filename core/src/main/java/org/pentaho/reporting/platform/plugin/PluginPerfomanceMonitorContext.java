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

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.engine.classic.core.states.PerformanceMonitorContext;
import org.pentaho.reporting.libraries.base.util.LoggingStopWatch;
import org.pentaho.reporting.libraries.base.util.PerformanceLoggingStopWatch;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import javax.swing.event.ChangeListener;

/**
 * Created by dima.prokopenko@gmail.com on 4/19/2016.
 */
public class PluginPerfomanceMonitorContext implements PerformanceMonitorContext {

  public static final String FORMAT = "%1s sessionid[%2s] userid[%3s] requestid[%4s]";

  @Override public PerformanceLoggingStopWatch createStopWatch( String tag ) {
    return new LoggingStopWatch( tag, getMessage( null ) );
  }

  @Override public PerformanceLoggingStopWatch createStopWatch( String tag, Object message ) {
    return new LoggingStopWatch( tag, getMessage( String.valueOf( message ) ) );
  }

  private String getMessage( String message ) {
    IPentahoSession session = PentahoSessionHolder.getSession();
    String sessionId = null;
    String name = null;

    if ( session != null ) {
      sessionId = session.getId();
      name = session.getName();
    }

    String requestId = ReportListenerThreadHolder.getRequestId();
    message = String.format( FORMAT, message, sessionId, name, requestId );

    return message;
  }

  @Override public void addChangeListener( ChangeListener listener ) {
    // sorry, no.
  }

  @Override public void removeChangeListener( ChangeListener listener ) {
    // sorry, no.
  }

  @Override public void close() {
    // have not idea what to do with that.
  }
}
