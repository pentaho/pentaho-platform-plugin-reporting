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


package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

import java.util.UUID;

class AutoScheduleListener implements ReportProgressListener {

  private boolean scheduled = false;
  private int threshold;
  private UUID id;
  private IPentahoSession session;
  private IPentahoAsyncExecutor pentahoAsyncExecutor;

  protected AutoScheduleListener( final UUID id, final IPentahoSession session, final int threshold,
                                  final IPentahoAsyncExecutor pentahoAsyncExecutor ) {
    this.id = id;
    this.threshold = threshold;
    this.session = session;

    this.pentahoAsyncExecutor = pentahoAsyncExecutor;
  }

  private synchronized void autoSchedule( final ReportProgressEvent reportProgressEvent ) {
    if ( !scheduled && threshold > 0 && reportProgressEvent != null
      && reportProgressEvent.getMaximumRow() > threshold ) {
      //Auto scheduling always needs confirmation
      pentahoAsyncExecutor.preSchedule( id, session );
      scheduled = true;
    }
  }

  @Override public void reportProcessingStarted( final ReportProgressEvent reportProgressEvent ) {
    autoSchedule( reportProgressEvent );
  }

  @Override public void reportProcessingUpdate( final ReportProgressEvent reportProgressEvent ) {
    autoSchedule( reportProgressEvent );
  }

  @Override public void reportProcessingFinished( final ReportProgressEvent reportProgressEvent ) {

  }
}
