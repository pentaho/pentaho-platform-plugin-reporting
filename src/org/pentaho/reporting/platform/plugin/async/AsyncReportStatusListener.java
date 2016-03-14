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

import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.libraries.base.config.ExtendedConfiguration;

import java.util.UUID;

/**
 * Simple synchronized bean with async execution status.
 * <p>
 * Created by dima.prokopenko@gmail.com on 2/12/2016.
 */
class AsyncReportStatusListener implements IAsyncReportListener, IAsyncReportState {

  public static final String COMPUTING_LAYOUT = "AsyncComputingLayoutTitle";
  public static final String PRECOMPUTING_VALUES = "AsyncPrecomputingValuesTitle";
  public static final String PAGINATING = "AsyncPaginatingTitle";
  public static final String GENERATING_CONTENT = "AsyncGeneratingContentTitle";

  private final String path;
  private final UUID uuid;
  private final String mimeType;
  private AsyncExecutionStatus status = AsyncExecutionStatus.QUEUED;
  private int progress = 0;
  private int page = 0;
  private int row = 0;
  private int totalPages = 0;
  private int totalRows = 0;
  private String activity;
  private boolean firstPageMode = false;


  public AsyncReportStatusListener( final String path,
                                    final UUID uuid,
                                    final String mimeType ) {
    this.path = path;
    this.uuid = uuid;
    this.mimeType = mimeType;

    final ExtendedConfiguration config = ClassicEngineBoot.getInstance().getExtendedConfig();
    firstPageMode = config.getBoolProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode" );

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

  @Override public synchronized void setTotalPages( final int totalPages ) {
    this.totalPages = totalPages;
  }

  @Override
  public boolean isFirstPageMode() {
    return firstPageMode;
  }

  @Override
  public synchronized int getProgress() {
    return progress;
  }

  @Override
  public synchronized int getPage() {
    return page;
  }

  @Override public synchronized int getTotalPages() {
    return totalPages;
  }

  @Override
  public synchronized int getRow() {
    return row;
  }

  @Override public synchronized int getTotalRows() {
    return totalRows;
  }

  @Override
  public synchronized String getActivity() {
    return activity;
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
    updateState( event, activity );
  }

  @Override
  public synchronized void reportProcessingFinished( final ReportProgressEvent event ) {
    final int activity = event.getActivity();
    updateState( event, activity );
    this.status = AsyncExecutionStatus.FINISHED;
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

  private void updateState( final ReportProgressEvent event, final int activity ) {
    this.activity = getActivityCode( activity );
    this.progress = (int) ReportProgressEvent.computePercentageComplete( event, true );
    this.page = event.getPage();
    this.row = event.getRow();
    this.totalRows = event.getMaximumRow();
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
