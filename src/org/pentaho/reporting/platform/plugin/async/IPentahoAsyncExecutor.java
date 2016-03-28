package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Created by dima.prokopenko@gmail.com on 2/25/2016.
 */
public interface IPentahoAsyncExecutor {

  Future<IFixedSizeStreamingContent> getFuture( UUID id, IPentahoSession session );

  void cleanFuture( UUID id, IPentahoSession session );

  UUID addTask( IAsyncReportExecution<IFixedSizeStreamingContent,
    IAsyncReportState> task, IPentahoSession session );

  IAsyncReportState getReportState( UUID id, IPentahoSession session );

  void requestPage( UUID id, IPentahoSession session, int page );
}
