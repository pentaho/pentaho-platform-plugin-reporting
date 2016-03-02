package org.pentaho.reporting.platform.plugin.async;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

/**
 * Created by dima.prokopenko@gmail.com on 2/11/2016.
 */
public interface IAsyncReportExecution<T> extends Callable<T> {
  void setListener( AsyncReportStatusListener listener );
  IAsyncReportState getState();

  void cancel();
  RunnableFuture<T> newTask();
}
