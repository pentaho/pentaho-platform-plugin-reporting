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
