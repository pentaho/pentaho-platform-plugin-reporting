package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.MasterReport;

public interface ReportOutputHandlerSelector
{
  public String getOutputType();

  public MasterReport getReport();

  public boolean isUseJcrOutput();

  public String getJcrOutputPath();

  public <T> T getInput(String parameterName, T defaultValue, Class<T> idx);
}
