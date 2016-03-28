package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;

public interface IAsyncReportListener extends ReportProgressListener {

  void setStatus( AsyncExecutionStatus status );

  boolean isFirstPageMode();

  int getRequestedPage();

  void updateGenerationStatus( int generatedPage );

}
