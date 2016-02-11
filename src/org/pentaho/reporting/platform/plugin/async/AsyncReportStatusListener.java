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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple bean with async execution status
 *
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
public class AsyncReportStatusListener implements AsyncReportListener, ReportProgressListener, AsyncReportState {

  private String path;
  private UUID uuid;
  private AsyncExecutionStatus status = AsyncExecutionStatus.QUEUED;
  private int progress = 0;

  private Queue<ReportProgressEvent> events = new ConcurrentLinkedQueue<>();

  private String mimeType;

  public AsyncReportStatusListener( String path, UUID uuid, String mimeType ) {
    this.path = path;
    this.uuid = uuid;
    this.mimeType = mimeType;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  @Override
  public AsyncExecutionStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus( AsyncExecutionStatus status ) {
    this.status = status;
  }

  @Override
  public int getProgress() {
    return progress;
  }

  @Override
  public void setProgress( int progress ) {
    this.progress = progress;
  }

  @Override
  public String getMimeType() {
    return this.mimeType;
  }

  @Override
  public void reportProcessingStarted( ReportProgressEvent event ) {
    events.add( event );
  }

  @Override
  public void reportProcessingUpdate( ReportProgressEvent event ) {
    events.add( event );
  }

  @Override
  public void reportProcessingFinished( ReportProgressEvent event ) {
    events.add( event );
  }

  // is not thread safe but we don't need it
  @Override
  public AsyncReportState clone() {
    AsyncReportStatusListener clone = new AsyncReportStatusListener( path, UUID.fromString( this.uuid.toString() ), mimeType );
    clone.setStatus( this.status );
    clone.setProgress( this.progress );

    return clone;
  }

  @Override
  public String toString() {
    return "AsyncReportStatusListener{"
        + "path='" + path + '\''
        + ", uuid=" + uuid
        + ", status=" + status
        + ", progress=" + progress
        + '}';
  }
}
