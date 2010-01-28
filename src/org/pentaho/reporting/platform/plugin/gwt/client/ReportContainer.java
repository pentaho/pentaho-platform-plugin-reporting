package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ReportContainer extends VerticalPanel implements IParameterSubmissionListener
{
  private ParameterControllerPanel parameterControllerPanel;
  private Frame reportContainer;
  private ReportViewer viewer;
  private String url = "about:blank";

  public ReportContainer(final ReportViewer viewer, ResourceBundle messages)
  {
    this.viewer = viewer;

    reportContainer = new Frame();

    parameterControllerPanel = new ParameterControllerPanel(viewer, this, messages);
    parameterControllerPanel.addParameterSubmissionListener(this);

    init();
  }

  public void init() {
	reportContainer.setUrl("about:blank");
    reportContainer.setVisible(true);
    clear();
    reportContainer.setHeight("100%"); //$NON-NLS-1$
    reportContainer.setWidth("100%"); //$NON-NLS-1$
    reportContainer.getElement().setAttribute("frameBorder", "0"); //$NON-NLS-1$ //$NON-NLS-2$
    add(parameterControllerPanel);
    add(reportContainer);
    setWidth("100%"); //$NON-NLS-1$
    setHeight("100%"); //$NON-NLS-1$
    makeFullHeight(reportContainer, this);
    reportContainer.setUrl(url);
  }
  
  public void hideParameterController()
  {
    parameterControllerPanel.clear();
    parameterControllerPanel.removeFromParent();
    parameterControllerPanel.setVisible(false);
  }

  public void parametersReady(Map<String, List<String>> parameterMap, RENDER_TYPE renderType)
  {
    url = viewer.buildReportUrl(renderType, parameterMap, parameterControllerPanel.isAutoSubmit());
  }

  public void showBlank()
  {
    // build url for the report to actually render
    reportContainer.setVisible(false);
    url = "about:blank";
    reportContainer.setUrl(url); //$NON-NLS-1$
  }

  private void makeFullHeight(Widget widget, Widget stopWidget)
  {
    final List<com.google.gwt.dom.client.Element> parentList = new ArrayList<com.google.gwt.dom.client.Element>();
    com.google.gwt.dom.client.Element parent = widget.getElement();
    while (parent != stopWidget.getElement() && parent != null)
    {
      parentList.add(parent);
      parent = parent.getParentElement();
    }
    Collections.reverse(parentList);
    for (int i = 1; i < parentList.size(); i++)
    {
      parentList.get(i).getStyle().setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

}
