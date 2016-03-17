package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class PentahoAsyncReportExecution implements IAsyncReportExecution<InputStream> {

  private SimpleReportingComponent reportComponent;
  private AsyncJobFileStagingHandler handler;
  private String url;

  private String instanceId = UUID.randomUUID().toString();
  private final IPentahoSession safeSession;

  private IAsyncReportListener listener;

  /**
   * Creates callable execuiton task.
   *
   * @param url             for audit purposes
   * @param reportComponent component responsible for gererating report
   * @param handler         content staging handler between requests
   * @param instanceId      id of caller. Used to track state of task. For example gap between submission and actual execution start
   */
  public PentahoAsyncReportExecution( final String url, final SimpleReportingComponent reportComponent,
      final AsyncJobFileStagingHandler handler, String instanceId ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.instanceId = instanceId == null ? UUID.randomUUID().toString() : instanceId;
    this.safeSession = PentahoSessionHolder.getSession();
  }

  @Override public InputStream call() throws Exception {
    listener.setStatus( AsyncExecutionStatus.WORKING );
    PentahoSessionHolder.setSession( safeSession );
    try {
      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();
      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.INSTANCE_START, instanceId, "", 0, null );

      if ( reportComponent.execute() ) {

        final long end = System.currentTimeMillis();
        AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
            MessageTypes.INSTANCE_END, instanceId, "", ( (float) ( end - start ) / 1000 ), null );

        listener.setStatus( AsyncExecutionStatus.FINISHED );

        return handler.getStagingContent();
      }

      listener.setStatus( AsyncExecutionStatus.FAILED );

      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.FAILED, instanceId, "", 0, null );
      return new NullInputStream( 0 );
    } finally {
      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.removeSession();
    }
  }

  @Override public void cancel() {
    this.listener.setStatus( AsyncExecutionStatus.CANCELED );
  }

  @Override public RunnableFuture<InputStream> newTask() {
    return new FutureTask<InputStream>( this ) {
      public boolean cancel( final boolean mayInterruptIfRunning ) {
        try {
          if ( mayInterruptIfRunning ) {
            PentahoAsyncReportExecution.this.cancel();
          }
        } finally {
          return super.cancel( mayInterruptIfRunning );
        }
      }
    };
  }

  @Override public String toString() {
    return "PentahoAsyncReportExecution{" + "url='" + url + '\'' + ", instanceId='" + instanceId + '\'' + ", listener="
        + listener + '}';
  }

  @Override public void setListener( final IAsyncReportListener listener ) {
    this.listener = listener;
  }

  @Override public IAsyncReportState getState() {
    return listener;
  }

  public String getMimeType() {
    return reportComponent.getMimeType();
  }

  public String getReportPath() {
    return this.url;
  }
}
