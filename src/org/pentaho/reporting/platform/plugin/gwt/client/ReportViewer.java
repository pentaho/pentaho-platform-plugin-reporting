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
import com.google.gwt.core.client.GWT;
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
  private String solution = Window.Location.getParameter("solution"); //$NON-NLS-1$
  private String path = Window.Location.getParameter("path"); //$NON-NLS-1$
  private String name = Window.Location.getParameter("name"); //$NON-NLS-1$
  private ResourceBundle messages = new ResourceBundle();
  private ReportContainer container = new ReportContainer(this, messages);
  
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
    messages.loadBundle("messages/", "messages", true, ReportViewer.this); //$NON-NLS-1$ //$NON-NLS-2$
  }

  public void bundleLoaded(String bundleName)
  {

    // build report container
    // this widget has:
    // +report parameter panel
    // +page controller (if paged output)
    // +the report itself

    History.addValueChangeHandler(historyHandler);
    initUI();
    setupNativeHooks(this);
    hideParentReportViewers();
  }

  private void initUI()
  {
    RootPanel panel = RootPanel.get("content"); //$NON-NLS-1$
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
    $wnd.reportViewer_hide = function() {
      viewer.@org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer::hide()();
    }
  }-*/;

  private void hide() {
    container.hideParameterController();
  }
  
  private native void hideParentReportViewers()
  /*-{
    var count = 10;
    var myparent = $wnd.parent;
    while (myparent != null && count >= 0) {
      if ($wnd != myparent) {
        if(typeof myparent.reportViewer_hide == 'function') {
          myparent.reportViewer_hide();
        } 
      }
      if (myparent == myparent.parent) {
        // BISERVER-3614:
        // while we don't know why this would ever be the case,
        // we know that this does happen and it will bring a browser
        // to its knees, it's as if top.parent == top
        break;
      }
      myparent = myparent.parent;
      if (myparent == top) {
        break;
      }
      count--;
    }
  }-*/;
  
  public void openUrlInDialog(String title, String url, String width, String height)
  {
    if (StringUtils.isEmpty(height))
    {
      height = "600px"; //$NON-NLS-1$
    }
    if (StringUtils.isEmpty(width))
    {
      width = "800px"; //$NON-NLS-1$
    }
    if (height.endsWith("px") == false) //$NON-NLS-1$
    {
      height += "px"; //$NON-NLS-1$
    }
    if (width.endsWith("px") == false) //$NON-NLS-1$
    {
      width += "px"; //$NON-NLS-1$
    }

    final DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setText(title);
    VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px"); //$NON-NLS-1$ //$NON-NLS-2$

    Frame frame = new Frame(url);
    frame.setSize(width, height);
    dialogContent.add(frame);
    HorizontalPanel buttonPanel = new HorizontalPanel();
    DOM.setStyleAttribute(buttonPanel.getElement(), "padding", "0px 5px 5px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    buttonPanel.setWidth("100%"); //$NON-NLS-1$
    buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    Button okButton = new Button(messages.getString("ok", "OK")); //$NON-NLS-1$ //$NON-NLS-2$
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
    StringTokenizer st = new StringTokenizer(historyToken, "&"); //$NON-NLS-1$
    int paramTokens = st.countTokens();
    for (int i = 0; i < paramTokens; i++)
    {
      String fullParam = st.tokenAt(i);
      StringTokenizer st2 = new StringTokenizer(fullParam, "="); //$NON-NLS-1$
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

  public String buildReportUrl(RENDER_TYPE renderType,
                               Map<String, List<String>> reportParameterMap,
                               final boolean autoSubmit)
  {
    String reportPath = Window.Location.getPath();
    if (reportPath.indexOf("reportviewer") != -1) //$NON-NLS-1$
    {
      reportPath = reportPath.substring(0, reportPath.indexOf("reportviewer") - 1); //$NON-NLS-1$
      // add query part of url
      reportPath += "?"; //$NON-NLS-1$
    }

    String parameters = ""; //$NON-NLS-1$
    if (reportParameterMap != null)
    {
      for (String key : reportParameterMap.keySet())
      {
        List<String> valueList = reportParameterMap.get(key);
        for (String value : valueList)
        {
          if (StringUtils.isEmpty(parameters) == false)
          {
            parameters += "&"; //$NON-NLS-1$
          }
          parameters += key + "=" + URL.encodeComponent(value); //$NON-NLS-1$
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
              parameters += "&"; //$NON-NLS-1$
            }
            parameters += key + "=" + URL.encodeComponent(decodedValue); //$NON-NLS-1$
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
              parameters += "&"; //$NON-NLS-1$
            }
            parameters += key + "=" + URL.encodeComponent(value); //$NON-NLS-1$
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
    if (Window.Location.getParameter("paginate") == null || "".equals(Window.Location.getParameter("paginate"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    {
      reportPath += "&paginate=true"; //$NON-NLS-1$
    }

    reportPath += "&renderMode=" + renderType; //$NON-NLS-1$
    reportPath += "&autoSubmit=" + autoSubmit; //$NON-NLS-1$
    if (GWT.isScript() == false) {
      reportPath = reportPath.substring(1);
      reportPath = "?solution=steel-wheels&path=reports&name=Inventory.prpt" + reportPath;
      String url = "http://localhost:8080/pentaho/content/reporting" + reportPath + "&userid=joe&password=password";
      System.out.println(url);
      return url;
    }
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
