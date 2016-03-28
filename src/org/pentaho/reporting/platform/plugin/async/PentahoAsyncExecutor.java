package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.UUID;

public class PentahoAsyncExecutor
  extends
  AbstractPentahoAsyncExecutor<IAsyncReportExecution<IFixedSizeStreamingContent, IAsyncReportState>,
    IAsyncReportState, IFixedSizeStreamingContent>
  implements IPentahoAsyncExecutor {

  public static final String BEAN_NAME = "IPentahoAsyncExecutor";

  public PentahoAsyncExecutor( final int capacity ) {
    super( capacity );
  }

  @Override public void onLogout( final IPentahoSession iPentahoSession ) {
    super.onLogout( iPentahoSession );
    AsyncJobFileStagingHandler.cleanSession( iPentahoSession );
  }

  @Override public void shutdown() {
    super.shutdown();
    AsyncJobFileStagingHandler.cleanStagingDir();
  }

  @Override public UUID addTask( IAsyncReportExecution<IFixedSizeStreamingContent, IAsyncReportState> task,
                                 IPentahoSession session ) {
    return super.addTask( task, session );
  }


  @Override public IAsyncReportState getReportState( UUID id, IPentahoSession session ) {
    return super.getReportState( id, session );
  }

  @Override public void requestPage( UUID id, IPentahoSession session, int page ) {
    super.requestPage( id, session, page );
  }
}
