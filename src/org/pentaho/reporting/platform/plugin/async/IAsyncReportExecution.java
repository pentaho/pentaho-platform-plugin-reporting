package org.pentaho.reporting.platform.plugin.async;

import java.util.concurrent.Callable;
import java.util.concurrent.RunnableFuture;

public interface IAsyncReportExecution<T> extends Callable<T> {

  void setListener( IAsyncReportListener listener );

  void cancel();

  RunnableFuture<T> newTask();
}
