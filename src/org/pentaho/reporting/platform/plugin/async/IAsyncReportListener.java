package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

public interface IAsyncReportListener extends ReportProgressListener {

  void setStatus( AsyncExecutionStatus status );

  void setTotalPages( int totalPages );

  boolean isFirstPageMode();
}
