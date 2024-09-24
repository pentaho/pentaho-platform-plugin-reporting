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

