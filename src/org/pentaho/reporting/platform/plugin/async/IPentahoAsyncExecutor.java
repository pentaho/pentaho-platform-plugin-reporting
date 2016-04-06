package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.UUID;
import java.util.concurrent.Future;

public interface IPentahoAsyncExecutor<TReportState extends IAsyncReportState> {

  Future<IFixedSizeStreamingContent> getFuture( UUID id, IPentahoSession session );

  void cleanFuture( UUID id, IPentahoSession session );

  UUID addTask( IAsyncReportExecution<TReportState> task, IPentahoSession session );

  TReportState getReportState( UUID id, IPentahoSession session );

  void requestPage( UUID id, IPentahoSession session, int page );
}
