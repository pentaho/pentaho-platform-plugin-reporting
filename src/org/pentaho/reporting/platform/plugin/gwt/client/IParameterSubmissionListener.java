package org.pentaho.reporting.platform.plugin.gwt.client;

import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

public interface IParameterSubmissionListener
{
  public void parametersReady(ParameterValues parameterMap, RENDER_TYPE renderType);

  public void showBlank();
}
