package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.OutputStream;

public class PentahoAsyncReportExecution extends AbstractAsyncReportExecution<IAsyncReportState> {

  private static final Log log = LogFactory.getLog( PentahoAsyncReportExecution.class );

  public PentahoAsyncReportExecution( String url,
                                      SimpleReportingComponent reportComponent,
                                      AsyncJobFileStagingHandler handler,
                                      IPentahoSession safeSession,
                                      String auditId ) {
    super( url, reportComponent, handler, safeSession, auditId );
  }


  /**
   * Generate report and return input stream to a generated report from server.
   * <p>
   * Pay attention - it is important to set proper status during execution. In case 'fail' or 'complete' status not set
   * - status remains 'working' and executor unable to determine that actual execution has ended.
   *
   * @return input stream for client
   * @throws Exception
   */
  @Override public IFixedSizeStreamingContent call() throws Exception {
    final AsyncReportStatusListener listener = getListener();
    if ( listener == null ) {
      throw new NullPointerException( "No listener for async report execution: " + url );
    }

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
      return NULL;
    } catch ( final Throwable ee ) {
      // it is bad practice to catch throwable.
      // but we has to to set proper execution status in any case.
      // Example: NoSuchMethodError (instance of Error) in case of usage of
      // uncompilable jar versions.
      // We have to avoid to hang on working status.
      log.error( "fail to execute report in async mode: " + ee );
      // to be sure after an error output stream is closed
      IOUtils.closeQuietly( handler.getStagingOutputStream() );
      fail();
      return NULL;
    } finally {
      // in case report processor not going to close it
      OutputStream out = handler.getStagingOutputStream();
      IOUtils.closeQuietly( out );

      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.removeSession();
    }
  }

  @Override
  public IAsyncReportState getState() {
    final AsyncReportStatusListener listener = getListener();
    if ( listener == null ) {
      throw new IllegalStateException( "Cannot query state until job is added to the executor." );
    }
    return listener.getState();
  }

}
