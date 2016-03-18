package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class PentahoAsyncReportExecution implements IAsyncReportExecution<InputStream> {

  private final SimpleReportingComponent reportComponent;
  private final AsyncJobFileStagingHandler handler;
  private final String url;
  private final String auditId;
  private final IPentahoSession safeSession;

  private AsyncReportStatusListener listener;

  /**
   * Creates callable execuiton task.
   *
   * @param url             for audit purposes
   * @param reportComponent component responsible for gererating report
   * @param handler         content staging handler between requests
   */
  public PentahoAsyncReportExecution( final String url,
                                      final SimpleReportingComponent reportComponent,
                                      final AsyncJobFileStagingHandler handler,
                                      final String auditId) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.auditId = auditId;
    this.safeSession = PentahoSessionHolder.getSession();
  }

  public void notifyTaskQueued(UUID id) {
    ArgumentNullException.validate("id", id);

    if (listener != null) {
      throw new IllegalStateException("This instance has already been scheduled.");
    }
    this.listener = createListener(id);
  }

  @Override public InputStream call() throws Exception {

    listener.setStatus( AsyncExecutionStatus.WORKING );
    PentahoSessionHolder.setSession( safeSession );
    try {
      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();
      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.INSTANCE_START, auditId, "", 0, null );

      if ( reportComponent.execute() ) {

        final long end = System.currentTimeMillis();
        AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
            MessageTypes.INSTANCE_END, auditId, "", ( (float) ( end - start ) / 1000 ), null );

        listener.setStatus( AsyncExecutionStatus.FINISHED );

        return handler.getStagingContent();
      }

      listener.setStatus( AsyncExecutionStatus.FAILED );

      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.FAILED, auditId, "", 0, null );
      return new NullInputStream( 0 );
    } finally {
      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.removeSession();
    }
  }

  protected AsyncReportStatusListener createListener(UUID instanceId) {
    return new AsyncReportStatusListener( getReportPath(), instanceId, getMimeType() );
  }

  // cancel can only be called from the future created by "newTask".
  protected void cancel() {
    this.listener.setStatus( AsyncExecutionStatus.CANCELED );
  }

  @Override public RunnableFuture<InputStream> newTask() {
    return new FutureTask<InputStream>( this ) {
      public boolean cancel( final boolean mayInterruptIfRunning ) {
        try {
          if ( mayInterruptIfRunning ) {
            PentahoAsyncReportExecution.this.cancel();
          }
        } catch (final Exception e) {
          // ignored.
        }
        return super.cancel( mayInterruptIfRunning );
      }
    };
  }

  @Override public String toString() {
    return "PentahoAsyncReportExecution{" + "url='" + url + '\'' + ", instanceId='" + auditId + '\'' + ", listener="
        + listener + '}';
  }

  @Override public IAsyncReportState getState() {
    if (listener == null) {
      throw new IllegalStateException("Cannot query state until job is added to the executor.");
    }
    return listener.getState();
  }

  public String getMimeType() {
    return reportComponent.getMimeType();
  }

  public String getReportPath() {
    return this.url;
  }
}
