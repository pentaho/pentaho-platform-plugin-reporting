package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.platform.plugin.ExecuteReportContentHandler;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.InputStream;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

/**
 * Created by dima.prokopenko@gmail.com on 2/2/2016.
 */
public class PentahoAsyncReportExecution implements IAsyncReportExecution<InputStream> {

  private SimpleReportingComponent reportComponent;
  private AsyncJobFileStagingHandler handler;
  private String url;

  private String userSession = "";
  private String instanceId = "";

  private AsyncReportStatusListener listener;

  public PentahoAsyncReportExecution( String url, SimpleReportingComponent reportComponent, AsyncJobFileStagingHandler handler ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
  }

  public void forSession( String userSession ) {
    this.userSession = userSession;
  }

  public void forInstanceId( String insnaceId ) {
    this.instanceId = insnaceId;
  }

  @Override
  public InputStream call() throws Exception {
    final long start = System.currentTimeMillis();
    AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
        .getName(), MessageTypes.INSTANCE_START, instanceId, "", 0, null );

    // let's do it!
    listener.setStatus( AsyncExecutionStatus.WORKING );

    final MasterReport report = reportComponent.getReport();

    //async is always fully buffered
    report.getReportConfiguration().setConfigProperty( ExecuteReportContentHandler.FORCED_BUFFERED_WRITING, "false" );

    if ( reportComponent.execute() ) {
      listener.setStatus( AsyncExecutionStatus.FINISHED );

      long end = System.currentTimeMillis();
      AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
          .getName(), MessageTypes.FAILED, instanceId, "", ( (float) ( end - start ) / 1000 ), null );

      return handler.getStagingContent();
    }

    listener.setStatus( AsyncExecutionStatus.FAILED );

    AuditHelper.audit( userSession, userSession, url, getClass().getName(), getClass()
        .getName(), MessageTypes.FAILED, instanceId, "", 0, null );
    return new NullInputStream( 0 );
  }

  @Override
  public IAsyncReportState getState() {
    return listener;
  }

  @Override public void cancel() {
    this.listener.setStatus( AsyncExecutionStatus.CANCELED );

    // hope processing engine is also do checks isInterrupted() sometimes
    Thread.currentThread().interrupt();
  }

  @Override public RunnableFuture<InputStream> newTask() {
    return new FutureTask<InputStream>( this ) {
      public boolean cancel( boolean mayInterruptIfRunning )  {
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

  @Override
  public String toString() {
    return this.getState().toString();
  }

  @Override
  public void setListener( AsyncReportStatusListener listener ) {
    this.listener = listener;
  }

  public AsyncReportStatusListener getListener() {
    return this.listener;
  }

  public String getMimeType() {
    return reportComponent.getMimeType();
  }

  public String getReportPath() {
    return this.url;
  }
}
