package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringTokenizer;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 11:11:15
 *
 * @author Thomas Morgner.
 */
public class ReportViewerUtil
{
  private ReportViewerUtil()
  {
  }


  /**
   * Parses the history tokens and returns a map keyed by the parameter names. The parameter values are
   * stored as list of strings.
   *
   * @return the token map.
   */
  public static Map<String, List<String>> getHistoryTokenParameters()
  {

    final HashMap<String, List<String>> map = new HashMap<String, List<String>>();
    String historyToken = History.getToken();
    if (StringUtils.isEmpty(historyToken))
    {
      return map;
    }

    historyToken = URL.decodeComponent(historyToken);
    final StringTokenizer st = new StringTokenizer(historyToken, "&"); //$NON-NLS-1$
    final int paramTokens = st.countTokens();
    for (int i = 0; i < paramTokens; i++)
    {
      final String fullParam = st.tokenAt(i);

      final StringTokenizer st2 = new StringTokenizer(fullParam, "="); //$NON-NLS-1$
      if (st2.countTokens() != 2)
      {
        continue;
      }

      final String name = st2.tokenAt(0);
      final String value = URL.decodeComponent(st2.tokenAt(1));
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

  /**
   * Builds the URL that is needed to communicate with the backend.
   *
   * @param renderType         the render type, never null.
   * @param reportParameterMap the parameter map, never null.
   * @return the generated URL
   */
  public static String buildReportUrl(final ReportViewer.RENDER_TYPE renderType,
                                      final ParameterValues reportParameterMap)
  {

    if (reportParameterMap == null)
    {
      throw new NullPointerException();
    }
    String reportPath = Window.Location.getPath();
    if (reportPath.indexOf("reportviewer") != -1) //$NON-NLS-1$
    {
      reportPath = reportPath.substring(0, reportPath.indexOf("reportviewer") - 1); //$NON-NLS-1$
      // add query part of url
      reportPath += "?"; //$NON-NLS-1$
    }
    reportPath += "renderMode=" + renderType; // NON-NLS

    final StringBuffer parameters = new StringBuffer(); //$NON-NLS-1$
    for (final String key : reportParameterMap.getParameterNames())
    {
      final String[] valueList = reportParameterMap.getParameterValues(key);
      for (final String value : valueList)
      {
        if (parameters.length() > 0)
        {
          parameters.append("&"); //$NON-NLS-1$
        }
        parameters.append(URL.encodeComponent(key));
        parameters.append("=");
        parameters.append(URL.encodeComponent(value)); //$NON-NLS-1$
      }
    }

    final Map<String, List<String>> historyParams = getHistoryTokenParameters();
    final Map<String, List<String>> requestParams = Window.Location.getParameterMap();
    if (requestParams != null)
    {
      for (final String key : requestParams.keySet())
      {
        final List<String> valueList = requestParams.get(key);
        for (final String value : valueList)
        {
          final String decodedValue = URL.decodeComponent(value);
          // only add new parameters (do not override *ANYTHING*)
          if ((historyParams == null || historyParams.containsKey(key) == false) &&
              (reportParameterMap.containsParameter(key) == false))
          {
            if (parameters.length() > 0)
            {
              parameters.append("&"); //$NON-NLS-1$
            }
            parameters.append(URL.encodeComponent(key));
            parameters.append("=");
            parameters.append(URL.encodeComponent(decodedValue)); //$NON-NLS-1$
          }
        }
      }
    }

    // history token parameters will override default parameters (already on
    // URL)
    // but they will not override user submitted parameters
    if (historyParams != null)
    {
      for (final String key : historyParams.keySet())
      {
        final List<String> valueList = historyParams.get(key);
        for (final String value : valueList)
        {
          // only add new parameters (do not override
          // reportParameterMap)
          if (reportParameterMap == null || reportParameterMap.containsParameter(key) == false)
          {
            if (parameters.length() > 0)
            {
              parameters.append("&"); //$NON-NLS-1$
            }
            parameters.append(URL.encodeComponent(key));
            parameters.append("=");
            parameters.append(URL.encodeComponent(value)); //$NON-NLS-1$
          }
        }
      }
    }
    final String parametersAsString = parameters.toString();
    if (History.getToken().equals(parametersAsString) == false)
    {
      // don't add duplicates, only new ones
      History.newItem(URL.encodeComponent(parametersAsString), false);
    }

    reportPath += "&" + parametersAsString;

    if (GWT.isScript() == false)
    {
      reportPath = reportPath.substring(1);
      reportPath = "?solution=steel-wheels&path=reports&name=Inventory.prpt" + reportPath; //$NON-NLS-1$
      final String url = "http://localhost:8080/pentaho/content/reporting" + reportPath + "&userid=joe&password=password"; //$NON-NLS-1$ //$NON-NLS-2$
      System.out.println(url);
      return url;
    }
    return reportPath;
  }


  public static native boolean isInPUC()
    /*-{
      return (top.mantle_initialized == true);
    }-*/;

  public static native void showPUCMessageDialog(String title, String message)
    /*-{
      top.mantle_showMessage(title, message);
    }-*/;

  public static void showErrorDialog(final ResourceBundle messages, final String error)
  {
    final String title = messages.getString("error", "Error");//$NON-NLS-1$ 
    showMessageDialog(messages, title, error);
  }

  public static int parseInt(final String text, final int defaultValue)
  {
    if (ReportViewerUtil.isEmpty(text))
    {
      return defaultValue;
    }
    try
    {
      return Integer.parseInt(text);
    }
    catch (NumberFormatException nfe)
    {
      return defaultValue;
    }
  }

  public static void showMessageDialog(final ResourceBundle messages, final String title, final String message)
  {
    if (ReportViewerUtil.isInPUC())
    {
      ReportViewerUtil.showPUCMessageDialog(title, message);
      return;
    }

    final DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setText(title);
    final VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    dialogContent.add(new HTML(message, true));
    final HorizontalPanel buttonPanel = new HorizontalPanel();
    DOM.setStyleAttribute(buttonPanel.getElement(), "padding", "0px 5px 5px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    buttonPanel.setWidth("100%"); //$NON-NLS-1$
    buttonPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    final Button okButton = new Button(messages.getString("ok", "OK")); //$NON-NLS-1$ //$NON-NLS-2$
    okButton.addClickHandler(new ClickHandler()
    {
      public void onClick(final ClickEvent event)
      {
        dialogBox.hide();
      }
    });
    buttonPanel.add(okButton);
    dialogContent.add(buttonPanel);
    dialogBox.setWidget(dialogContent);
    dialogBox.center();
    // prompt
  }

  public static boolean isEmpty(final String text)
  {
    return text == null || "".equals(text);
  }
  
}
