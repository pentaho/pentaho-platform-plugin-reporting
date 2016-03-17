package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.reporting.engine.classic.core.ReportInterruptedException;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

public class PentahoAsyncReportExecution implements IAsyncReportExecution<InputStream> {

  private static final Log logger = LogFactory.getLog( PentahoAsyncReportExecution.class );

  private SimpleReportingComponent reportComponent;
  private AsyncJobFileStagingHandler handler;
  private String url;

  private String userSession = "";
  private String instanceId = "";

  private IAsyncReportListener listener;

  public PentahoAsyncReportExecution( final String url, final SimpleReportingComponent reportComponent,
      final AsyncJobFileStagingHandler handler ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
  }

  public void forSession( final String userSession ) {
    this.userSession = userSession;
  }

  public void forInstanceId( final String instanceId ) {
    this.instanceId = instanceId;
  }

  @Override public InputStream call() throws Exception {
    try {
      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();

      AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass().getName(),
          MessageTypes.INSTANCE_START, instanceId, "", 0, null );

      if ( reportComponent.execute() ) {
        final long end = System.currentTimeMillis();
        AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass().getName(),
            MessageTypes.INSTANCE_END, instanceId, "", ( (float) ( end - start ) / 1000 ), null );

        listener.setStatus( AsyncExecutionStatus.FINISHED );
        return handler.getStagingContent();
      }

      if ( listener.getStatus() != AsyncExecutionStatus.CANCELED ) {
        // don't override cancel status.
        listener.setStatus( AsyncExecutionStatus.FAILED );
      }

      AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass().getName(), MessageTypes.FAILED,
          instanceId, "", 0, null );
      return new NullInputStream( 0 );
    } finally {
      ReportListenerThreadHolder.clear();
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

  @Override public String getMimeType() {
    return reportComponent.getMimeType();
  }

  @Override public IAsyncReportState getState() {
    return this.listener;
  }

  @Override public String getReportPath() {
    return this.url;
  }
}
