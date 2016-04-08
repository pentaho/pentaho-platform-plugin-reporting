package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public interface IAsyncReportExecution<V extends IAsyncReportState> extends Callable<IFixedSizeStreamingContent> {

  /**
   * Assigns the UUID and create task listener. This should be called before actual execution if we expect any state
   * from listener object.
   * This is called exclusively from the AsyncExecutor, which manages ids and guarantees the validity of them.
   *
   * @param id
   */
  void notifyTaskQueued( UUID id );

  /**
   * Return the current state. Never null.
   * @return
     */
  V getState();

  String getReportPath();

  /**
   * Get generated content mime-type suggestion to
   * set proper http response header
   *
   * @return
   */
  String getMimeType();

  RunnableFuture<IFixedSizeStreamingContent> newTask();

  void requestPage( int page );
}
