package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.List;
import java.util.Map;

import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

public interface IParameterSubmissionListener
{
  public void parametersReady(Map<String,List<String>> parameterMap, RENDER_TYPE renderType);
  public void showBlank();
}
