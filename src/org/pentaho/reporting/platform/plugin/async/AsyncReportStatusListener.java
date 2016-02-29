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

import java.util.UUID;

/**
 * Simple synchronized bean with async execution status.
 * <p/>
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
public class AsyncReportStatusListener implements IAsyncReportListener, ReportProgressListener, IAsyncReportState {

  public static final String COMPUTING_LAYOUT = "async_computing_layout_title";
  public static final String PRECOMPUTING_VALUES = "async_precomputing_values_title";
  public static final String PAGINATING = "async_paginating_title";
  public static final String GENERATING_CONTENT = "async_generating_content_title";

  private volatile String path;
  private volatile UUID uuid;
  private volatile AsyncExecutionStatus status = AsyncExecutionStatus.QUEUED;
  private volatile int progress = 0;
  private volatile int page = 0;
  private volatile String activity;
  private volatile int row = 0;
  private volatile boolean firstPageMode = false;
  private volatile String mimeType;

  public AsyncReportStatusListener( final String path, final UUID uuid, final String mimeType ) {
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
  public synchronized void setStatus( final AsyncExecutionStatus status ) {
    this.status = status;
  }

  @Override
  public synchronized int getProgress() {
    return progress;
  }

  @Override
  public synchronized void setProgress( final int progress ) {
    this.progress = progress;
  }

  @Override
  public synchronized int getPage() {
    return page;
  }

  @Override
  public synchronized void setPage( final int page ) {
    this.page = page;
  }

  @Override
  public synchronized void setRow( final int row ) {
    this.row = row;
  }

  @Override
  public synchronized int getRow() {
    return row;
  }

  @Override
  public synchronized String getActivity() {
    return activity;
  }

  @Override
  public synchronized void setActivity( final String activity ) {
    this.activity = activity;
  }

  @Override
  public synchronized String getMimeType() {
    return this.mimeType;
  }

  @Override
  public synchronized void reportProcessingStarted( final ReportProgressEvent event ) {
    this.status = AsyncExecutionStatus.WORKING;
  }

  @Override
  public synchronized void reportProcessingUpdate( final ReportProgressEvent event ) {
    final int activity = event.getActivity();
    if ( firstPageMode && ReportProgressEvent.GENERATING_CONTENT == activity && page > 0 ) {
      //First page is ready here
      this.status = AsyncExecutionStatus.CONTENT_AVAILABLE;
    }
    this.activity = getActivityCode( activity );
    this.progress = (int) ReportProgressEvent.computePercentageComplete( event, true );
    this.page = event.getPage();
    this.row = event.getRow();
  }

  @Override
  public synchronized void reportProcessingFinished( final ReportProgressEvent event ) {
    final int activity = event.getActivity();
    this.activity = getActivityCode( activity );
    this.progress = (int) ReportProgressEvent.computePercentageComplete( event, true );
    this.page = event.getPage();
    this.row = event.getRow();
    this.status = AsyncExecutionStatus.FINISHED;
  }

  public synchronized void setFirstPageMode( final boolean firstPageMode ) {
    this.firstPageMode = firstPageMode;
  }

  @Override
  public synchronized IAsyncReportState clone() {
    final AsyncReportStatusListener clone =
      new AsyncReportStatusListener( path, UUID.fromString( this.uuid.toString() ), mimeType );
    clone.setStatus( this.status );
    clone.setProgress( this.progress );
    clone.setPage( this.page );
    clone.setRow( this.row );
    clone.setActivity( this.activity );
    return clone;
  }

  @Override public String toString() {
    return "AsyncReportStatusListener{"
      + "path='" + path + '\''
      + ", uuid=" + uuid
      + ", status=" + status
      + ", progress=" + progress
      + ", page=" + page
      + ", activity='" + activity + '\''
      + ", row=" + row
      + ", firstPageMode=" + firstPageMode
      + ", mimeType='" + mimeType + '\''
      + '}';
  }

  private static String getActivityCode( final int activity ) {
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
