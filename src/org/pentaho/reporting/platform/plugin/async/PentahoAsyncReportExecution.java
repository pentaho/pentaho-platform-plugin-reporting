package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class PentahoAsyncReportExecution implements IAsyncReportExecution<InputStream> {

  private SimpleReportingComponent reportComponent;
  private AsyncJobFileStagingHandler handler;
  private String url;

  private String userSession = "";
  private String instanceId = "";
  private final IPentahoSession safeSession;

  private IAsyncReportListener listener;

  public PentahoAsyncReportExecution( final String url, final SimpleReportingComponent reportComponent, final AsyncJobFileStagingHandler handler ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.safeSession = PentahoSessionHolder.getSession();
  }

  public void forSession( final String userSession ) {
    this.userSession = userSession;
  }

  public void forInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  @Override
  public InputStream call() throws Exception {
    listener.setStatus( AsyncExecutionStatus.WORKING );
    PentahoSessionHolder.setSession( safeSession );
    try {
      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();
      AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
        .getName(), MessageTypes.INSTANCE_START, instanceId, "", 0, null );

      if ( reportComponent.execute() ) {

        final long end = System.currentTimeMillis();
        AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
          .getName(), MessageTypes.INSTANCE_END, instanceId, "", ( (float) ( end - start ) / 1000 ), null );

        listener.setStatus( AsyncExecutionStatus.FINISHED );

        return handler.getStagingContent();
      }

      listener.setStatus( AsyncExecutionStatus.FAILED );

      AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
        .getName(), MessageTypes.FAILED, instanceId, "", 0, null );
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
      public boolean cancel( final boolean mayInterruptIfRunning )  {
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
    return "PentahoAsyncReportExecution{"
      + "url='" + url + '\''
      + ", instanceId='" + instanceId + '\''
      + ", listener=" + listener
      + '}';
  }

  @Override
  public void setListener( final IAsyncReportListener listener ) {
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
