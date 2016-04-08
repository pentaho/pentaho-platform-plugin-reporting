package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public abstract class AbstractAsyncReportExecution<TReportState extends IAsyncReportState>
  implements IAsyncReportExecution<TReportState> {

  protected final SimpleReportingComponent reportComponent;
  protected final AsyncJobFileStagingHandler handler;
  protected final String url;
  protected final String auditId;
  protected final IPentahoSession safeSession;

  private AsyncReportStatusListener listener;

  private static final Log log = LogFactory.getLog( AbstractAsyncReportExecution.class );

  /**
   * Creates callable execuiton task.
   *
   * @param url             for audit purposes
   * @param reportComponent component responsible for gererating report
   * @param handler         content staging handler between requests
   */
  public AbstractAsyncReportExecution( final String url,
                                       final SimpleReportingComponent reportComponent,
                                       final AsyncJobFileStagingHandler handler,
                                       final IPentahoSession safeSession,
                                       final String auditId ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.auditId = auditId;
    this.safeSession = safeSession;
  }

  public void notifyTaskQueued( final UUID id ) {
    ArgumentNullException.validate( "id", id );

    if ( listener != null ) {
      throw new IllegalStateException( "This instance has already been scheduled." );
    }
    this.listener = createListener( id );
  }

  protected AsyncReportStatusListener getListener() {
    return this.listener;
  }

  protected void fail() {
    // do not erase canceled status
    if ( listener != null && !listener.getState().getStatus().equals( AsyncExecutionStatus.CANCELED ) ) {
      listener.setStatus( AsyncExecutionStatus.FAILED );
    }
    AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
      MessageTypes.FAILED, auditId, "", 0, null );
  }

  protected AsyncReportStatusListener createListener( final UUID instanceId ) {
    return new AsyncReportStatusListener( getReportPath(), instanceId, getMimeType() );
  }

  // cancel can only be called from the future created by "newTask".
  protected void cancel() {
    String userName = safeSession == null ? "Unknown" : safeSession.getName();
    log.info( "Report execution canceled: " + url + " , requested by : " + userName );
    this.listener.setStatus( AsyncExecutionStatus.CANCELED );
  }

  @Override public RunnableFuture<IFixedSizeStreamingContent> newTask() {
    return new FutureTask<IFixedSizeStreamingContent>( this ) {
      public boolean cancel( final boolean mayInterruptIfRunning ) {
        try {
          if ( mayInterruptIfRunning ) {
            AbstractAsyncReportExecution.this.cancel();
          }
        } catch ( final Exception e ) {
          // ignored.
        }
        return super.cancel( mayInterruptIfRunning );
      }
    };
  }

  @Override public void requestPage( final int page ) {
    listener.setRequestedPage( page );
  }

  @Override public String toString() {
    return "PentahoAsyncReportExecution{" + "url='" + url + '\'' + ", instanceId='" + auditId + '\'' + ", listener="
      + listener + '}';
  }

  public String getMimeType() {
    return reportComponent.getMimeType();
  }

  public String getReportPath() {
    return this.url;
  }

  public static final IFixedSizeStreamingContent NULL = new NullSizeStreamingContent();

  public static final class NullSizeStreamingContent implements IFixedSizeStreamingContent {

    @Override public InputStream getStream() {
      return new NullInputStream( 0 );
    }

    @Override public long getContentSize() {
      return 0;
    }
  }
}
