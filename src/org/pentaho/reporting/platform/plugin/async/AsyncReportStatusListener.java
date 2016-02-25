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

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple synchronized bean with async execution status.
 *
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
class AsyncReportStatusListener implements IAsyncReportListener, ReportProgressListener, IAsyncReportState {

  private volatile String path;
  private volatile UUID uuid;
  private volatile AsyncExecutionStatus status = AsyncExecutionStatus.QUEUED;
  private volatile int progress = 0;
  private volatile int page = 0;
  private volatile String activity;

  public static final String COMPUTING_LAYOUT = "async_computing_layout_title";
  public static final String PRECOMPUTING_VALUES = "async_precomputing_values_title";
  public static final String PAGINATING = "async_paginating_title";
  public static final String GENERATING_CONTENT = "async_generating_content_title";



  private Queue<ReportProgressEvent> events = new ConcurrentLinkedQueue<>();

  private String mimeType;

  public AsyncReportStatusListener( String path, UUID uuid, String mimeType ) {
    this.path = path;
    this.uuid = uuid;
    this.mimeType = mimeType;
  }

  @Override
  public synchronized String getPath() {
    return path;
  }

  @Override
  public synchronized UUID getUuid() {
    return uuid;
  }

  @Override
  public synchronized AsyncExecutionStatus getStatus() {
    return status;
  }

  @Override
  public synchronized void setStatus( AsyncExecutionStatus status ) {
    this.status = status;
  }

  @Override
  public synchronized int getProgress() {
    return progress;
  }

  @Override
  public synchronized void setProgress( int progress ) {
    this.progress = progress;
  }

  @Override  public synchronized int getPage() {
    return page;
  }

  @Override public synchronized void setPage( int page ) {
    this.page = page;
  }

  @Override public synchronized String getActivity() {
    return activity;
  }

  @Override public synchronized void setActivity( String activity ) {
    this.activity = activity;
  }

  @Override
  public synchronized String getMimeType() {
    return this.mimeType;
  }

  @Override
  public synchronized void reportProcessingStarted( ReportProgressEvent event ) {
    // will be implemented in another commit
  }

  @Override
  public synchronized void reportProcessingUpdate( ReportProgressEvent event ) {
    // will be implemented in another commit
    this.activity = getActivityCode( event.getActivity() );
    this.progress = (int) ReportProgressEvent.computePercentageComplete( event, true );
    this.page = event.getPage();
  }

  @Override
  public synchronized void reportProcessingFinished( ReportProgressEvent event ) {
    // will be implemented in another commit
  }

  // is not thread safe but we don't need it
  @Override
  public AsyncReportState clone() {
    AsyncReportStatusListener clone =
      new AsyncReportStatusListener( path, UUID.fromString( this.uuid.toString() ), mimeType );
    clone.setStatus( this.status );
    clone.setProgress( this.progress );
    clone.setPage( this.page );
    clone.setActivity( this.activity );

    return clone;
  }

  @Override
  public String toString() {
    return "AsyncReportStatusListener{"
      + "path='" + path + '\''
      + ", uuid=" + uuid
      + ", status=" + status
      + ", progress=" + progress
      + ", page=" + page
      + ", activity=" + activity
      + '}';
  }

  private String getActivityCode( int activity ) {
    String result = "";

    switch ( activity ) {
      case ReportProgressEvent.COMPUTING_LAYOUT: {
        result = COMPUTING_LAYOUT;
        break;
      }
      case ReportProgressEvent.PRECOMPUTING_VALUES: {
        result = PRECOMPUTING_VALUES;
        break;
      }
      case ReportProgressEvent.PAGINATING: {
        result = PAGINATING;
        break;
      }
      case ReportProgressEvent.GENERATING_CONTENT: {
        result = GENERATING_CONTENT;
        break;
      }
    }

    return result;
  }
}
