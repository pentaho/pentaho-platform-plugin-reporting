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

import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

import java.util.Collections;
import java.util.UUID;

public class TestListener extends AsyncReportStatusListener {

  private boolean onStart = false;
  private boolean onUpdate = false;
  private boolean onFinish = false;
  private boolean onFirstPage = false;

  public TestListener( final String path, final UUID uuid, final String mimeType ) {
    super( path, uuid, mimeType, Collections.<ReportProgressListener>emptyList() );
  }

  @Override public synchronized void reportProcessingStarted( final ReportProgressEvent event ) {
    onStart = true;
    super.reportProcessingStarted( event );
  }

  @Override public synchronized void reportProcessingUpdate( final ReportProgressEvent event ) {
    onUpdate = true;
    super.reportProcessingUpdate( event );
    if ( getState().getStatus().equals( AsyncExecutionStatus.CONTENT_AVAILABLE ) ) {
      onFirstPage = true;
    }
  }

  @Override public synchronized void reportProcessingFinished( final ReportProgressEvent event ) {
    onFinish = true;
    super.reportProcessingFinished( event );
  }

  public boolean isOnStart() {
    return onStart;
  }

  public boolean isOnUpdate() {
    return onUpdate;
  }

  public boolean isOnFinish() {
    return onFinish;
  }

  public boolean isOnFirstPage() {
    return onFirstPage;
  }
}

