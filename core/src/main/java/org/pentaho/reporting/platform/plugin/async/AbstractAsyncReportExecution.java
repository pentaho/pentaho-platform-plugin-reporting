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
 * Copyright 2006 - 2020 Hitachi Vantara.  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin.async;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.MDCUtil;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractAsyncReportExecution<TReportState extends IAsyncReportState>
  implements IAsyncReportExecution<TReportState>, IListenableFutureDelegator<IFixedSizeStreamingContent> {

  protected final SimpleReportingComponent reportComponent;
  protected final AsyncJobFileStagingHandler handler;
  protected final String url;
  protected final String auditId;
  protected final IPentahoSession safeSession;

  private AsyncReportStatusListener listener;

  private static final Log log = LogFactory.getLog( AbstractAsyncReportExecution.class );

  private AuditWrapper audit;
  
  protected final MDCUtil mdcUtil = new MDCUtil();

  public AbstractAsyncReportExecution( final String url,
                                       final SimpleReportingComponent reportComponent,
                                       final AsyncJobFileStagingHandler handler,
                                       final IPentahoSession safeSession,
                                       final String auditId ) {
    this( url, reportComponent, handler, safeSession, auditId, AuditWrapper.NULL );
  }

  /**
   * Creates callable execuiton task.
   *
   * @param url             for audit purposes
   * @param reportComponent component responsible for gererating report
   * @param handler         content staging handler between requests
   * @param safeSession     pentaho session used mostly for log/audit purposes
   * @param auditId         audit id per execution. Typically nested from http controller that do create this task
   * @param audit           audit record handler implementation
   */
  public AbstractAsyncReportExecution( final String url,
                                       final SimpleReportingComponent reportComponent,
                                       final AsyncJobFileStagingHandler handler,
                                       final IPentahoSession safeSession,
                                       final String auditId,
                                       final AuditWrapper audit ) {
    ArgumentNullException.validate( "url", url );
    ArgumentNullException.validate( "reportComponent", reportComponent );
    ArgumentNullException.validate( "handler", handler );
    ArgumentNullException.validate( "safeSession", safeSession );
    ArgumentNullException.validate( "auditId", auditId );
    ArgumentNullException.validate( "audit", audit );
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.auditId = auditId;
    this.safeSession = safeSession;
    this.audit = audit;
  }

  public void notifyTaskQueued( final UUID id, final List<? extends ReportProgressListener> callbackListeners ) {
    ArgumentNullException.validate( "id", id );

    if ( listener != null ) {
      throw new IllegalStateException( "This instance has already been scheduled." );
    }
    this.listener = createListener( id, callbackListeners );
  }

  protected AsyncReportStatusListener getListener() {
    return this.listener;
  }

  protected AuditWrapper getAudit() {
    return this.audit;
  }

  protected void fail() {
    // do not erase canceled status
    String messageType = MessageTypes.CANCELLED;
    if ( listener != null && !listener.getState().getStatus().equals( AsyncExecutionStatus.CANCELED ) ) {
      listener.setStatus( AsyncExecutionStatus.FAILED );
      messageType = MessageTypes.FAILED;
    }
    audit.audit( safeSession.getId(), safeSession.getName(), getAuditActionName(), getClass().getName(), getClass().getName(),
      messageType, auditId, "", 0, null );
    closeFile();
  }

  private void closeFile() {
    try {
      handler.getStagingContent().cleanContent();
    } catch ( final Exception e  ) {
      log.debug( "No content was created for this task" );
    }
  }

  protected String getAuditActionName() {
    return url;
  }

  protected AsyncReportStatusListener createListener( final UUID instanceId,
                                                      final List<? extends ReportProgressListener> callbackListeners ) {
    return new AsyncReportStatusListener( getReportPath(), instanceId, getMimeType(), callbackListeners );
  }

  // cancel can only be called from the future created by "newTask".
  protected void cancel() {
    String userName = safeSession == null ? "Unknown" : safeSession.getName();
    log.info( "Report execution canceled: " + url + " , requested by : " + userName );
    closeFile();
    if ( listener != null ) {
      listener.cancel();
    }
  }

  @Override
  public void requestPage( final int page ) {
    listener.setRequestedPage( page );
  }

  @Override
  public String toString() {
    return "PentahoAsyncReportExecution{" + "url='" + url + '\'' + ", instanceId='" + auditId + '\'' + ", listener="
      + listener + '}';
  }

  public String getMimeType() {
    return reportComponent.getMimeType();
  }

  public String getReportPath() {
    return this.url;
  }

  @Override public synchronized boolean schedule() {
    synchronized( listener ) {
      if ( listener.isScheduled() ) {
        return false;
      } else {
        listener.setStatus( AsyncExecutionStatus.SCHEDULED );
        return listener.isScheduled();
      }
    }
  }

  public static final IFixedSizeStreamingContent NULL = new NullSizeStreamingContent();

  public static final class NullSizeStreamingContent implements IFixedSizeStreamingContent {

    @Override public InputStream getStream() {
      return new NullInputStream( 0 );
    }

    @Override public long getContentSize() {
      return 0;
    }

    @Override public boolean cleanContent() {
      return true;
    }
  }


  @Override
  public ListenableFuture<IFixedSizeStreamingContent> delegate(
    final ListenableFuture<IFixedSizeStreamingContent> delegate ) {
    return new CancelableListenableFuture( delegate );
  }

  @Override public boolean preSchedule() {
    listener.setStatus( AsyncExecutionStatus.PRE_SCHEDULED );
    return AsyncExecutionStatus.PRE_SCHEDULED.equals( listener.getState().getStatus() );
  }

  /**
   * Implements cancel functionality
   */
  class CancelableListenableFuture extends SimpleDelegatedListenableFuture<IFixedSizeStreamingContent> {
    private CancelableListenableFuture( final ListenableFuture<IFixedSizeStreamingContent> delegate ) {
      super( delegate );
    }

    @Override
    public boolean cancel( final boolean mayInterruptIfRunning ) {
      final AsyncReportStatusListener listener = getListener();
      if ( !listener.isScheduled() ) {
        try {
          if ( mayInterruptIfRunning ) {
            AbstractAsyncReportExecution.this.cancel();
          }
        } catch ( final Exception e ) {
          // ignored.
        }
        return super.cancel( mayInterruptIfRunning );
      } else {
        return Boolean.FALSE;
      }
    }
  }
}
