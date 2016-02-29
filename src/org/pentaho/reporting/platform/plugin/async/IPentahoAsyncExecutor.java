package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * Created by dima.prokopenko@gmail.com on 2/25/2016.
 */
public interface IPentahoAsyncExecutor {
  UUID addTask( PentahoAsyncReportExecution task, IPentahoSession session );

  Future<InputStream> getFuture( UUID id, IPentahoSession session );

  IAsyncReportState getReportState( UUID id, IPentahoSession session );

  void cleanFuture( UUID id, IPentahoSession session );
}
