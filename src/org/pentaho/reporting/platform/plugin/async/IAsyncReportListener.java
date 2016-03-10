package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

public interface IAsyncReportListener extends ReportProgressListener, IAsyncReportState {

  void setStatus( AsyncExecutionStatus status );

  boolean isFirstPageMode();
}
