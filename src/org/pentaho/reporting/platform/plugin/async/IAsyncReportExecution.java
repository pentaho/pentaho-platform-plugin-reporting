package org.pentaho.reporting.platform.plugin.async;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public interface IAsyncReportExecution<T> extends Callable<T> {

  /**
   * Assigns the UUID. This is called exclusively from the AsyncExecutor, which manages ids and guarantees the validity of them.
   *
   * @param id
   */
  void notifyTaskQueued( UUID id );

  /**
   * Return the current state. Never null.
   * @return
     */
  IAsyncReportState getState();
  String getReportPath();

  /**
   * Get generated content mime-type suggestion to
   * set proper http response header
   *
   * @return
   */
  String getMimeType();

  RunnableFuture<T> newTask();

  void requestPage( int page );
}
