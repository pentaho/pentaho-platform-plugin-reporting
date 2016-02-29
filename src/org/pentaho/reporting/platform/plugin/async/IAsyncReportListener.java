package org.pentaho.reporting.platform.plugin.async;

import java.util.EventListener;

/**
 * Created by dima.prokopenko@gmail.com on 2/11/2016.
 */
public interface IAsyncReportListener extends EventListener {
  void setStatus( AsyncExecutionStatus status );

  void setProgress( int progress );
}
