package org.pentaho.reporting.platform.plugin.async;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public interface IAsyncReportExecution<T> extends Callable<T> {
  void setListener( IAsyncReportListener listener );

  IAsyncReportState getState();
  String getReportPath();

  /**
   * Get generated content mime-type suggestion to
   * set proper http response header
   *
   * @return
   */
  String getMimeType();

  /**
   * Set user session string id to be used for auditing.
   *
   * @param userSession - string session id for audit pusposes
   */
  void forSession( String userSession );

  /**
   * Set instance id for auditing purposes
   *
   * @param instanceId - string instance id
   */
  void forInstanceId( String instanceId );

  /**
   * Attempt to cancel running task. Exact implementation can also provide
   * some additional clean-up.
   */
  void cancel();

  RunnableFuture<T> newTask();
}
