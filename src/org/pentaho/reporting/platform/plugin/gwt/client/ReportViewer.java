package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.gwt.widgets.client.utils.i18n.IResourceBundleLoadCallback;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringTokenizer;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ReportViewer implements EntryPoint, IResourceBundleLoadCallback
{
  private String solution = Window.Location.getParameter("solution");
  private String path = Window.Location.getParameter("path");
  private String name = Window.Location.getParameter("name");
  private ResourceBundle messages = new ResourceBundle();

  private ValueChangeHandler<String> historyHandler = new ValueChangeHandler<String>()
  {
    public void onValueChange(ValueChangeEvent<String> event)
    {
      initUI();
    }
  };

  public enum RENDER_TYPE
  {
    REPORT, XML, SUBSCRIBE, DOWNLOAD
  };

  public void onModuleLoad()
  {
    messages.loadBundle("messages/", "messages", true, ReportViewer.this);
  }

  public void bundleLoaded(String bundleName)
  {
    setupNativeHooks(this);

    // build report container
    // this widget has:
    // +report parameter panel
    // +page controller (if paged output)
    // +the report itself

    History.addValueChangeHandler(historyHandler);
    initUI();
  }

  private void initUI()
  {
    ReportContainer container = new ReportContainer(this, messages);
    RootPanel panel = RootPanel.get("content");
    panel.clear();
    panel.add(container);

  }

  private native void setupNativeHooks(ReportViewer viewer)
  /*-{
    $wnd.reportViewer_openUrlInDialog = function(title, message, width, height) {
      viewer.@org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer::openUrlInDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(title, message, width, height);
    }
    top.reportViewer_openUrlInDialog = function(title, message, width, height) {
      viewer.@org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer::openUrlInDialog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(title, message, width, height);
    }
  }-*/;

  public void openUrlInDialog(String title, String url, String width, String height)
  {
    if (StringUtils.isEmpty(height))
    {
      height = "600px";
    }
    if (StringUtils.isEmpty(width))
    {
      width = "800px";
    }
    if (height.endsWith("px") == false)
    {
      height += "px";
    }
    if (width.endsWith("px") == false)
    {
      width += "px";
    }

    final DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setText(title);
    VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px");

    Frame frame = new Frame(url);
    frame.setSize(width, height);
    dialogContent.add(frame);
    HorizontalPanel buttonPanel = new HorizontalPanel();
    DOM.setStyleAttribute(buttonPanel.getElement(), "padding", "0px 5px 5px 5px");
    buttonPanel.setWidth("100%");
    buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    Button okButton = new Button(messages.getString("ok", "OK"));
    okButton.addClickHandler(new ClickHandler()
    {
      public void onClick(ClickEvent event)
      {
        dialogBox.hide();
      }
    });
    buttonPanel.add(okButton);
    dialogContent.add(buttonPanel);
    dialogBox.setWidget(dialogContent);

    // dialogBox.setHeight(height);
    // dialogBox.setWidth(width);
    dialogBox.center();
  }

  public Map<String, List<String>> getHistoryTokenParameters()
  {
    HashMap<String, List<String>> map = new HashMap<String, List<String>>();
    String historyToken = History.getToken();
    if (StringUtils.isEmpty(historyToken))
    {
      return map;
    }
    historyToken = URL.decodeComponent(historyToken);
    StringTokenizer st = new StringTokenizer(historyToken, "&");
    int paramTokens = st.countTokens();
    for (int i = 0; i < paramTokens; i++)
    {
      String fullParam = st.tokenAt(i);
      StringTokenizer st2 = new StringTokenizer(fullParam, "=");
      if (st2.countTokens() != 2)
      {
        continue;
      }
      String name = st2.tokenAt(0);
      String value = URL.decodeComponent(st2.tokenAt(1));
      List<String> paramValues = map.get(name);
      if (paramValues == null)
      {
        paramValues = new ArrayList<String>();
        map.put(name, paramValues);
      }
      paramValues.add(value);
    }
    // tokenize this guy & and =
    return map;
  }

  public String buildReportUrl(RENDER_TYPE renderType, Map<String, List<String>> reportParameterMap)
  {
    String reportPath = Window.Location.getPath();
    if (reportPath.indexOf("reportviewer") != -1)
    {
      reportPath = reportPath.substring(0, reportPath.indexOf("reportviewer") - 1);
      // add query part of url
      reportPath += "?";
    }

    String parameters = "";
    if (reportParameterMap != null)
    {
      for (String key : reportParameterMap.keySet())
      {
        List<String> valueList = reportParameterMap.get(key);
        for (String value : valueList)
        {
          if (StringUtils.isEmpty(parameters) == false)
          {
            parameters += "&";
          }
          parameters += key + "=" + URL.encodeComponent(value);
        }
      }
    }

    Map<String, List<String>> historyParams = getHistoryTokenParameters();
    Map<String, List<String>> requestParams = Window.Location.getParameterMap();
    if (requestParams != null)
    {
      for (String key : requestParams.keySet())
      {
        List<String> valueList = requestParams.get(key);
        for (String value : valueList)
        {
          String decodedValue = URL.decodeComponent(value);
          // only add new parameters (do not override *ANYTHING*)
          if ((historyParams == null || historyParams.containsKey(key) == false)
              && (reportParameterMap == null || reportParameterMap.containsKey(key) == false))
          {
            if (StringUtils.isEmpty(parameters) == false)
            {
              parameters += "&";
            }
            parameters += key + "=" + URL.encodeComponent(decodedValue);
          }
        }
      }
    }

    // history token parameters will override default parameters (already on URL)
    // but they will not override user submitted parameters
    if (historyParams != null)
    {
      for (String key : historyParams.keySet())
      {
        List<String> valueList = historyParams.get(key);
        for (String value : valueList)
        {
          // only add new parameters (do not override reportParameterMap)
          if (reportParameterMap == null || reportParameterMap.containsKey(key) == false)
          {
            if (StringUtils.isEmpty(parameters) == false)
            {
              parameters += "&";
            }
            parameters += key + "=" + URL.encodeComponent(value);
          }
        }
      }
    }
    if (History.getToken().equals(parameters) == false)
    {
      // don't add duplicates, only new ones
      History.newItem(URL.encodeComponent(parameters), false);
    }

    reportPath += parameters;

    // by default, in the report viewer, unless otherwise specified, pagination will be turned on
    if (Window.Location.getParameter("paginate") == null || "".equals(Window.Location.getParameter("paginate")))
    {
      reportPath += "&paginate=true";
    }

    reportPath += "&renderMode=" + renderType;
    return reportPath;
  }

  public String getSolution()
  {
    return solution;
  }

  public void setSolution(String solution)
  {
    this.solution = solution;
  }

  public String getPath()
  {
    return path;
  }

  public void setPath(String path)
  {
    this.path = path;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }
}
