package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

  private static final Log log = LogFactory.getLog( PentahoAsyncReportExecution.class );

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
                                      final String auditId ) {
    this.reportComponent = reportComponent;
    this.handler = handler;
    this.url = url;
    this.auditId = auditId;
    this.safeSession = PentahoSessionHolder.getSession();
  }

  public void notifyTaskQueued( final UUID id ) {
    ArgumentNullException.validate( "id", id );

    if ( listener != null ) {
      throw new IllegalStateException( "This instance has already been scheduled." );
    }
    this.listener = createListener( id );
  }

  /**
   * Generate report and return input stream to a generated report from server.
   *
   * Pay attention - it is important to set proper status during execution. In case
   * 'fail' or 'complete' status not set - status remains 'working' and executor unable to
   * determine that actual execution has ended.
   *
   * @return input stream for client
   * @throws Exception
   */
  @Override public InputStream call() throws Exception {
    try {
      listener.setStatus( AsyncExecutionStatus.WORKING );

      PentahoSessionHolder.setSession( safeSession );

      ReportListenerThreadHolder.setListener( listener );
      final long start = System.currentTimeMillis();
      AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
          MessageTypes.INSTANCE_START, auditId, "", 0, null );

      if ( reportComponent.execute() ) {

        final long end = System.currentTimeMillis();
        AuditHelper.audit( safeSession.getId(), safeSession.getId(), url, getClass().getName(), getClass().getName(),
            MessageTypes.INSTANCE_END, auditId, "", ( (float) ( end - start ) / 1000 ), null );


        final InputStream stagingContent = handler.getStagingContent();

        listener.setStatus( AsyncExecutionStatus.FINISHED );

        return stagingContent;
      }

      // in case execute just returns false without an exception.
      fail();
      return new NullInputStream( 0 );
    } catch ( final Throwable ee ) {
      // it is bad practice to catch throwable.
      // but we has to to set proper execution status in any case.
      // Example: NoSuchMethodError (instance of Error) in case of usage of
      // uncompilable jar versions.
      // We have to avoid to hang on working status.
      log.error( "fail to execute report in async mode: " + ee );

      fail();
      return new NullInputStream( 0 );
    } finally {
      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.removeSession();
    }
  }

  private void fail() {
    // do not erase canceled status
    if ( !listener.getState().getStatus().equals( AsyncExecutionStatus.CANCELED ) ) {
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
    this.listener.setStatus( AsyncExecutionStatus.CANCELED );
  }

  @Override public RunnableFuture<InputStream> newTask() {
    return new FutureTask<InputStream>( this ) {
      public boolean cancel( final boolean mayInterruptIfRunning ) {
        try {
          if ( mayInterruptIfRunning ) {
            PentahoAsyncReportExecution.this.cancel();
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

  @Override public IAsyncReportState getState() {
    if ( listener == null ) {
      throw new IllegalStateException( "Cannot query state until job is added to the executor." );
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
