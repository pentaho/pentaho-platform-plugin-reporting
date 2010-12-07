package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

public class ReportContainer extends VerticalPanel implements IParameterSubmissionListener
{
  private class ReportContentFrame extends Frame
  {

    public void onBrowserEvent(final Event event)
    {
      super.onBrowserEvent(event);
      if (event.getTypeInt() == Event.ONLOAD)
      {
        if (StringUtils.isEmpty(url) == false && url.equals(ABOUT_BLANK) == false)
        {
          WaitPopup.getInstance().setVisible(false);
        }
      }
    }

    protected void onLoad()
    {
      super.onLoad();
      if (StringUtils.isEmpty(url) == false && url.equals(ABOUT_BLANK) == false)
      {
        WaitPopup.getInstance().setVisible(false);
      }
    }

    public void setUrl(final String url)
    {
      if (StringUtils.isEmpty(url) == false && url.equals(ABOUT_BLANK) == false)
      {
        WaitPopup.getInstance().setVisible(true);
      }
      super.setUrl(url);
      // ie is not responding to onload
      final Timer t = new Timer()
      {
        public void run()
        {
          WaitPopup.getInstance().setVisible(false);
        }
      };
      t.schedule(1000);
    }

  }

  private static final String ABOUT_BLANK = "about:blank";

  private ParameterControllerPanel parameterControllerPanel;
  private Frame reportContainer;
  private String url = ABOUT_BLANK;

  public ReportContainer(final ResourceBundle messages)
  {
    parameterControllerPanel = new ParameterControllerPanel(this, messages);
    parameterControllerPanel.addParameterSubmissionListener(this);
    reportContainer = new ReportContentFrame();
    reportContainer.sinkEvents(Event.ONLOAD);

    init();
  }

  public void init()
  {
    reportContainer.setUrl(ABOUT_BLANK);
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

  public void parametersReady(final ParameterValues parameterMap, final RENDER_TYPE renderType)
  {
    url = ReportViewerUtil.buildReportUrl(renderType, parameterMap);
  }

  public void showBlank()
  {
    // build url for the report to actually render
    reportContainer.setVisible(false);
    url = ABOUT_BLANK;
    reportContainer.setUrl(url); //$NON-NLS-1$
  }

  private static void makeFullHeight(final Widget widget, final Widget stopWidget)
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
