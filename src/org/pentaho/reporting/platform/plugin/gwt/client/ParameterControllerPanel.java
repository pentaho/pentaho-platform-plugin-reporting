package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pentaho.gwt.widgets.client.utils.i18n.ResourceBundle;
import org.pentaho.gwt.widgets.client.utils.string.StringUtils;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;
import org.pentaho.reporting.platform.plugin.gwt.client.images.PageImages;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

public class ParameterControllerPanel extends VerticalPanel
{
  private List<IParameterSubmissionListener> listeners = new ArrayList<IParameterSubmissionListener>();
  private ReportViewer viewer;

  // all the parameters will be forced into strings
  private Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();
  private List<Element> parameterElements = new ArrayList<Element>();

  private DisclosurePanel parameterDisclosurePanel;
  private VerticalPanel parameterContainer = new VerticalPanel();
  private CheckBox submitParametersOnChangeCheckBox;
  private Button submitSubscriptionButton;
  private Button submitParametersButton;
  private boolean subscriptionPressed = false;
  private final ResourceBundle messages;
  private Boolean autoSubmitState;

  private class AutoSubmitChangeHandler implements ValueChangeHandler<Boolean>
  {
    public void onValueChange(final ValueChangeEvent<Boolean> booleanValueChangeEvent)
    {
      autoSubmitState = submitParametersOnChangeCheckBox.getValue();
    }
  }

  private ClickHandler submitParametersListener = new ClickHandler()
  {
    public void onClick(ClickEvent event)
    {
      if (promptNeeded() == false)
      {
        subscriptionPressed = false;
        // async call
        fetchParameters(false);
      }
    }
  };

  private ClickHandler submitSubscriptionListener = new ClickHandler()
  {
    public void onClick(ClickEvent event)
    {
      if (promptNeeded() == false)
      {
        subscriptionPressed = true;
        // async call
        fetchParameters(false);
      }
    }
  };

  private RequestCallback parameterRequestCallback = new RequestCallback()
  {
    public void onError(Request request, Throwable exception)
    {
      showMessageDialog(messages.getString("error"), messages.getString("couldNotFetchParams")); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public void onResponseReceived(Request request, Response response)
    {
      if (response.getStatusCode() != Response.SC_OK)
      {
        showMessageDialog(messages.getString("error"), messages.getString("couldNotFetchParams")); //$NON-NLS-1$ //$NON-NLS-2$
        return;
      }

      final Document resultDoc;
      try
      {
        resultDoc = (Document) XMLParser.parse(response.getText());
      } catch (Exception e)
      {
        showMessageDialog(messages.getString("error"), response.getText()); //$NON-NLS-1$
        return;
      }

      clear();

      Element parametersElement = (Element) resultDoc.getDocumentElement();

      String layout = "vertical"; //$NON-NLS-1$
      if (StringUtils.isEmpty(Window.Location.getParameter("layout")) == false) //$NON-NLS-1$
      {
        layout = Window.Location.getParameter("layout"); //$NON-NLS-1$
      }

      boolean showParameters = true;
      if (Window.Location.getParameter("showParameters") != null && !"".equals(Window.Location.getParameter("showParameters"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      {
        showParameters = "true".equalsIgnoreCase(Window.Location.getParameter("showParameters")); //$NON-NLS-1$ //$NON-NLS-2$
      }

      HashMap<String, ArrayList<String>> errors = buildErrors(resultDoc);//$NON-NLS-1$ 
      ArrayList globalErrors = errors.get(null);
      
      if (showParameters)
      {
        NodeList parameterNodes = parametersElement.getElementsByTagName("parameter"); //$NON-NLS-1$
        if (parameterNodes == null || parameterNodes.getLength() == 0)
        {
          fireParametersReady(parameterMap, RENDER_TYPE.REPORT);
          // add pagination controller (if needed)
          if ("true".equals(parametersElement.getAttribute("paginate"))) //$NON-NLS-1$ //$NON-NLS-2$
          {
            add(buildPaginationController(parametersElement));
          }
          return;
        }
        if (globalErrors != null && globalErrors.isEmpty() == false)
        {
          add(buildGlobalErrors(globalErrors));
        }
        add(parameterDisclosurePanel);

        // build parameter UI from document
        parameterContainer.clear();
        parameterElements.clear();

        // create a new parameter map
        parameterMap = new HashMap<String, List<String>>();

        Map<String, List<Element>> parameterGroupMap = new HashMap<String, List<Element>>();
        for (int i = 0; i < parameterNodes.getLength(); i++)
        {
          Element parameterElement = (Element) parameterNodes.item(i);
          parameterElements.add(parameterElement);
          String parameterGroupName = parameterElement.getAttribute("parameter-group"); //$NON-NLS-1$
          if (parameterGroupName == null)
          {
            // default group
            parameterGroupName = "parameters"; //$NON-NLS-1$
          }
          List<Element> groupList = parameterGroupMap.get(parameterGroupName);
          if (groupList == null)
          {
            groupList = new ArrayList<Element>();
            parameterGroupMap.put(parameterGroupName, groupList);
          }
          groupList.add(parameterElement);
        }

        // must preserve order
        for (String parameterGroupName : parameterGroupMap.keySet())
        {
          final Panel parameterGroupPanel;
          if (layout.equals("flow")) //$NON-NLS-1$
          {
            parameterGroupPanel = new FlowPanel();
          }
          else
          {
            parameterGroupPanel = new VerticalPanel();
          }

          String groupLabel = null;
          List<Element> groupList = parameterGroupMap.get(parameterGroupName);
          for (Element parameterElement : groupList)
          {
            groupLabel = parameterElement.getAttribute("parameter-group-label"); //$NON-NLS-1$
            String label = parameterElement.getAttribute("label"); //$NON-NLS-1$
            if (label == null || "".equals(label)) //$NON-NLS-1$
            {
              label = parameterElement.getAttribute("name").trim(); //$NON-NLS-1$
            }
            else
            {
              label = label.trim();
            }
            String tooltip = parameterElement.getAttribute("tooltip"); //$NON-NLS-1$
            Label parameterLabel = new Label(label);
            parameterLabel.setTitle(tooltip);
            parameterLabel.setStyleName("parameter-label"); //$NON-NLS-1$

            VerticalPanel parameterPanel = new VerticalPanel();
            parameterPanel.setStyleName("parameter"); //$NON-NLS-1$
            parameterPanel.setTitle(tooltip);
            parameterPanel.add(parameterLabel);

            Widget parameterWidget = buildParameterWidget(parameterElement);
            if (parameterWidget != null)
            {
              // only add the parameter if it has a UI
              final String parameterName = parameterElement.getAttribute("name"); //$NON-NLS-1$              
              ArrayList<String> parameterErrors = errors.get(parameterName);
              if (parameterErrors != null) {
                for (String error : parameterErrors) {
                  Label errorLabel = new Label(error);
                  errorLabel.setStyleName("parameter-error-label");
                  DOM.setStyleAttribute(errorLabel.getElement(), "color", "red");
                  parameterPanel.add(errorLabel);
                }
                parameterPanel.setStyleName("parameter-error"); //$NON-NLS-1$
              }

              parameterPanel.add(parameterWidget);
              
              if (layout.equals("flow")) //$NON-NLS-1$
              {
                SimplePanel div = new SimplePanel();
                div.setStyleName("parameter-flow"); //$NON-NLS-1$
                div.add(parameterPanel);
                parameterGroupPanel.add(div);
              }
              else
              {
                parameterGroupPanel.add(parameterPanel);
              }
            }
          }
          if (groupLabel != null && !"".equals(groupLabel)) //$NON-NLS-1$
          {
            CaptionPanel parameterGroupCaptionPanel = new CaptionPanel();
            parameterGroupCaptionPanel.setCaptionText(groupLabel);
            parameterGroupCaptionPanel.setStyleName("parameter"); //$NON-NLS-1$
            parameterGroupCaptionPanel.setContentWidget(parameterGroupPanel);
            parameterContainer.add(parameterGroupCaptionPanel);
          }
          else
          {
            parameterContainer.add(parameterGroupPanel);
          }
        }

        // add parameter submit button/auto-submit checkbox
        FlowPanel submitPanel = new FlowPanel();
        submitPanel.setWidth("100%"); //$NON-NLS-1$
        submitPanel.setStyleName("parameter-submit-panel"); //$NON-NLS-1$
        if ("true".equalsIgnoreCase(parametersElement.getAttribute("subscribe"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
          submitPanel.add(submitSubscriptionButton);
        }

        // handle the auto-submit defaults.
        final String autoSubmitAttr = parametersElement.getAttribute("autoSubmit");
        if (StringUtils.isEmpty(autoSubmitAttr) == false)
        {
          submitParametersOnChangeCheckBox.setValue("true".equals(autoSubmitAttr));
          autoSubmitState = "true".equals(autoSubmitAttr);
        }

        submitPanel.add(submitParametersButton);
        submitPanel.add(submitParametersOnChangeCheckBox);
        parameterContainer.add(submitPanel);

        parameterDisclosurePanel.setContent(parameterContainer);

        // add pagination controller (if needed)
        if ("true".equals(parametersElement.getAttribute("paginate"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
          add(buildPaginationController(parametersElement));
        }
        if (globalErrors != null && globalErrors.isEmpty() == false)
        {
          add(buildGlobalErrors(globalErrors));
        }

        // if parameters are valid, submit them for report rendering
        if ("false".equals(parametersElement.getAttribute("is-prompt-needed"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
          if (subscriptionPressed)
          {
            fireParametersReady(parameterMap, RENDER_TYPE.SUBSCRIBE);
          }
          else
          {
            fireParametersReady(parameterMap, RENDER_TYPE.REPORT);
          }
        }
        else
        {
          firePromptNeeded();
        }

      }
      else
      {
        // add pagination controller (if needed)
        if ("true".equals(parametersElement.getAttribute("paginate"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
          add(buildPaginationController(parametersElement));
        }

        // do not show the parameter UI, but we must still fire events
        if ("false".equals(parametersElement.getAttribute("is-prompt-needed"))) //$NON-NLS-1$ //$NON-NLS-2$
        {
          if (subscriptionPressed)
          {
            fireParametersReady(parameterMap, RENDER_TYPE.SUBSCRIBE);
          }
          else
          {
            fireParametersReady(parameterMap, RENDER_TYPE.REPORT);
          }
        }
        else
        {
          firePromptNeeded();
        }
      }
    }

  };

  public ParameterControllerPanel(final ReportViewer viewer, final ResourceBundle messages)
  {
    this.viewer = viewer;
    this.messages = messages;

    parameterDisclosurePanel = new DisclosurePanel(messages.getString("reportParameters", "Report Parameters")); //$NON-NLS-1$ //$NON-NLS-2$
    submitParametersButton = new Button(messages.getString("viewReport", "View Report")); //$NON-NLS-1$ //$NON-NLS-2$
    submitSubscriptionButton = new Button(messages.getString("schedule", "Schedule")); //$NON-NLS-1$ //$NON-NLS-2$

    submitParametersOnChangeCheckBox = new CheckBox(messages.getString("autoSubmit", "Auto-Submit")); //$NON-NLS-1$ //$NON-NLS-2$

    setWidth("100%"); //$NON-NLS-1$
    setStyleName("parameter-application"); //$NON-NLS-1$
    parameterContainer.setStyleName("parameter-container"); //$NON-NLS-1$
    parameterContainer.setWidth("100%"); //$NON-NLS-1$

    parameterDisclosurePanel.setStyleName("parameter-disclosure"); //$NON-NLS-1$
    parameterDisclosurePanel.setOpen(true);
    parameterDisclosurePanel.setAnimationEnabled(true);
    parameterDisclosurePanel.setWidth("100%"); //$NON-NLS-1$
    final String autoSubmitAttr = Window.Location.getParameter("autoSubmit");
    if (StringUtils.isEmpty(autoSubmitAttr) == false)
    {
      // auto-submit state is only not-null, if the user explicitly requested a value via the URL
      submitParametersOnChangeCheckBox.setValue("true".equals(autoSubmitAttr));
      autoSubmitState = "true".equals(autoSubmitAttr);
    }
    else
    {
      // by default we claim to have a null value, so that the report on the server gets the chance
      // to define a sensible default ..
      submitParametersOnChangeCheckBox.setValue(true);
      autoSubmitState = null;
    }
    submitParametersOnChangeCheckBox.setTitle(messages.getString("submitTooltip")); //$NON-NLS-1$
    submitParametersOnChangeCheckBox.addValueChangeHandler(new AutoSubmitChangeHandler()); //$NON-NLS-1$
    submitParametersButton.addClickHandler(submitParametersListener);
    submitSubscriptionButton.addClickHandler(submitSubscriptionListener);

    // async call
    fetchParameters(false);
  }

  private HashMap<String, ArrayList<String>> buildErrors(final Document doc) {
    HashMap<String, ArrayList<String>> errorMap = new HashMap<String, ArrayList<String>>();
    NodeList errors = doc.getElementsByTagName("error");
    for (int i=0;i<errors.getLength();i++) {
      Element error = (Element)errors.item(i);
      String parameter = error.getAttribute("parameter");
      String msg = error.getAttribute("message");
      ArrayList<String> errorList = errorMap.get(parameter);
      if (errorList == null) {
        errorList = new ArrayList<String>();
        errorMap.put(parameter, errorList);
      }
      errorList.add(msg);
    }

    NodeList globalErrors = doc.getElementsByTagName("global-error");
     for (int i=0;i<globalErrors.getLength();i++) {
       Element error = (Element)globalErrors.item(i);
       String msg = error.getAttribute("message");
       ArrayList<String> errorList = errorMap.get(null);
       if (errorList == null) {
         errorList = new ArrayList<String>();
         errorMap.put(null, errorList);
       }
       errorList.add(msg);
     }
     return errorMap;
  }

  private Widget buildGlobalErrors(final ArrayList<String> errors)
  {
    VerticalPanel parameterPanel = new VerticalPanel();
    parameterPanel.setStyleName("parameter-error"); //$NON-NLS-1$

    // only add the parameter if it has a UI
    if (errors!= null) {
      for (String error : errors) {
        Label errorLabel = new Label(error);
        errorLabel.setStyleName("parameter-error-label");
        DOM.setStyleAttribute(errorLabel.getElement(), "color", "red");
        parameterPanel.add(errorLabel);
      }
    }
    return parameterPanel;
  }
  
  private Widget buildPaginationController(final Element parametersElement)
  {
    // need to add/build UI for pagination controls
    int acceptedPage = 0;
    if (parametersElement.getAttribute("accepted-page") != null && !"".equals(parametersElement.getAttribute("accepted-page"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    {
      acceptedPage = Math.max(0, Integer.parseInt(parametersElement.getAttribute("accepted-page"))); //$NON-NLS-1$
    }

    int pageCount = 0;
    if (parametersElement.getAttribute("page-count") != null && !"".equals(parametersElement.getAttribute("page-count"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    {
      pageCount = Integer.parseInt(parametersElement.getAttribute("page-count")); //$NON-NLS-1$
    }
    final int finalPageCount = pageCount;

    if (finalPageCount <= acceptedPage)
    {
      // we can't accept pages out of range, this can happen if we are on a page and then change a parameter value
      // resulting in a new report with less pages
      // when this happens, we'll just reduce the accepted page
      acceptedPage = Math.max(0, finalPageCount - 1);
    }
    final int finalAcceptedPage = acceptedPage;
    // add our default page, so we can keep this between selections of other parameters, otherwise it will not be on the
    // set of params are default back to zero (page 1)
    List<String> pageList = new ArrayList<String>();
    pageList.add("" + (finalAcceptedPage)); //$NON-NLS-1$
    parameterMap.put("accepted-page", pageList); //$NON-NLS-1$

    final Image backToFirstPage = PageImages.images.backToFirstPage().createImage();
    final Image backPage = PageImages.images.backButton().createImage();
    final Image forwardPage = PageImages.images.forwardButton().createImage();
    final Image forwardToLastPage = PageImages.images.forwardToLastPage().createImage();

    MouseOutHandler mouseOutHandler = new MouseOutHandler()
    {
      public void onMouseOut(com.google.gwt.event.dom.client.MouseOutEvent event)
      {
        backToFirstPage.removeStyleDependentName("hover"); //$NON-NLS-1$
        backPage.removeStyleDependentName("hover"); //$NON-NLS-1$
        forwardPage.removeStyleDependentName("hover"); //$NON-NLS-1$
        forwardToLastPage.removeStyleDependentName("hover"); //$NON-NLS-1$
      }
    };

    MouseOverHandler mouseOverHandler = new MouseOverHandler()
    {
      public void onMouseOver(MouseOverEvent event)
      {
        if (event.getSource() == backToFirstPage)
        {
          DOM.setStyleAttribute(backToFirstPage.getElement(), "backgroundColor", ""); //$NON-NLS-1$ //$NON-NLS-2$
          backToFirstPage.addStyleDependentName("hover"); //$NON-NLS-1$
        }
        else if (event.getSource() == backPage)
        {
          DOM.setStyleAttribute(backPage.getElement(), "backgroundColor", ""); //$NON-NLS-1$ //$NON-NLS-2$
          backPage.addStyleDependentName("hover"); //$NON-NLS-1$
        }
        else if (event.getSource() == forwardPage)
        {
          DOM.setStyleAttribute(forwardPage.getElement(), "backgroundColor", ""); //$NON-NLS-1$ //$NON-NLS-2$
          forwardPage.addStyleDependentName("hover"); //$NON-NLS-1$
        }
        else if (event.getSource() == forwardToLastPage)
        {
          DOM.setStyleAttribute(forwardToLastPage.getElement(), "backgroundColor", ""); //$NON-NLS-1$ //$NON-NLS-2$
          forwardToLastPage.addStyleDependentName("hover"); //$NON-NLS-1$
        }
      }
    };

    backToFirstPage.addMouseOverHandler(mouseOverHandler);
    backPage.addMouseOverHandler(mouseOverHandler);
    forwardPage.addMouseOverHandler(mouseOverHandler);
    forwardToLastPage.addMouseOverHandler(mouseOverHandler);

    backToFirstPage.addMouseOutHandler(mouseOutHandler);
    backPage.addMouseOutHandler(mouseOutHandler);
    forwardPage.addMouseOutHandler(mouseOutHandler);
    forwardToLastPage.addMouseOutHandler(mouseOutHandler);

    backToFirstPage.setStyleName("pageControllerButton"); //$NON-NLS-1$
    backPage.setStyleName("pageControllerButton"); //$NON-NLS-1$
    forwardPage.setStyleName("pageControllerButton"); //$NON-NLS-1$
    forwardToLastPage.setStyleName("pageControllerButton"); //$NON-NLS-1$

    ClickHandler pageClickHandler = new ClickHandler()
    {
      public void onClick(ClickEvent event)
      {
        boolean submit = true;
        if (event.getSource() == backToFirstPage)
        {
          if (finalAcceptedPage > 0)
          {
            List<String> pageList = new ArrayList<String>();
            pageList.add("0"); //$NON-NLS-1$
            parameterMap.put("accepted-page", pageList); //$NON-NLS-1$
          }
          else
          {
            submit = false;
          }
        }
        else if (event.getSource() == forwardToLastPage)
        {
          if (finalAcceptedPage + 1 < finalPageCount)
          {
            List<String> pageList = new ArrayList<String>();
            pageList.add("" + (finalPageCount - 1)); //$NON-NLS-1$
            parameterMap.put("accepted-page", pageList); //$NON-NLS-1$
          }
          else
          {
            submit = false;
          }
        }
        else if (event.getSource() == backPage)
        {
          if (finalAcceptedPage > 0)
          {
            List<String> pageList = new ArrayList<String>();
            pageList.add("" + (finalAcceptedPage - 1)); //$NON-NLS-1$
            parameterMap.put("accepted-page", pageList); //$NON-NLS-1$
          }
          else
          {
            submit = false;
          }
        }
        else if (event.getSource() == forwardPage)
        {
          if (finalAcceptedPage + 1 < finalPageCount)
          {
            List<String> pageList = new ArrayList<String>();
            pageList.add("" + (finalAcceptedPage + 1)); //$NON-NLS-1$
            parameterMap.put("accepted-page", pageList); //$NON-NLS-1$
          }
          else
          {
            submit = false;
          }
        }
        if (submit)
        {
          submitParametersListener.onClick(null);
        }
      };
    };

    backToFirstPage.addClickHandler(pageClickHandler);
    backPage.addClickHandler(pageClickHandler);
    forwardPage.addClickHandler(pageClickHandler);
    forwardToLastPage.addClickHandler(pageClickHandler);

    final TextBox pageBox = new TextBox();
    pageBox.setTextAlignment(TextBox.ALIGN_RIGHT);
    pageBox.addKeyUpHandler(new KeyUpHandler()
    {
      public void onKeyUp(KeyUpEvent event)
      {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
        {
          String error = null;
          try
          {
            List<String> pageList = new ArrayList<String>();
            int page = Integer.parseInt(pageBox.getText());
            if (page < 1)
            {
              throw new Exception(messages.getString("firstPageMustBePositive", "<BR>First page must a positive number<BR><BR>")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            if (page > finalPageCount)
            {
              throw new Exception(messages.getString("pageOutOfRange", "<BR>Page out of range, max page is : {0} <BR><BR>", "" + finalPageCount)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            pageList.add("" + (page - 1)); //$NON-NLS-1$
            parameterMap.put("accepted-page", pageList); //$NON-NLS-1$
            submitParametersListener.onClick(null);
          } catch (NumberFormatException t)
          {
            error = messages.getString("numericDigitsOnly", "<BR>Page number must contain numeric digits only.<BR><BR>"); //$NON-NLS-1$ //$NON-NLS-2$
          } catch (Throwable t)
          {
            error = t.getMessage();
          }
          if (error != null)
          {
            final DialogBox dialogBox = new DialogBox(false, true);
            dialogBox.setText(messages.getString("error", "Error")); //$NON-NLS-1$ //$NON-NLS-2$
            VerticalPanel dialogContent = new VerticalPanel();
            DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
            dialogContent.add(new HTML(error, true));
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
            dialogBox.center();
          }
        }
      }
    });
    // pages are zero based, but expose them to the user as 1 based
    if (acceptedPage <= 0)
    {
      pageBox.setText("1"); //$NON-NLS-1$
    }
    else
    {
      pageBox.setText("" + (acceptedPage + 1)); //$NON-NLS-1$
    }
    pageBox.setVisibleLength(3);

    HorizontalPanel pageControlPanel = new HorizontalPanel();
    pageControlPanel.setSpacing(1);
    pageControlPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    pageControlPanel.add(backToFirstPage);
    pageControlPanel.add(backPage);

    String pageStr = "&nbsp;&nbsp;" + messages.getString("page", "Page") + "&nbsp;"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    pageControlPanel.add(new HTML(pageStr));
    pageControlPanel.add(pageBox);
    String pageCountStr = "&nbsp;" + messages.getString("of", "of") + "&nbsp;" + pageCount + "&nbsp;"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    pageControlPanel.add(new HTML(pageCountStr));

    pageControlPanel.add(forwardPage);
    pageControlPanel.add(forwardToLastPage);

    HorizontalPanel pageControlPanelWrapper = new HorizontalPanel();
    pageControlPanelWrapper.setStyleName("pageControllerPanel"); //$NON-NLS-1$
    pageControlPanelWrapper.setWidth("100%"); //$NON-NLS-1$
    pageControlPanelWrapper.add(pageControlPanel);

    return pageControlPanelWrapper;
  }

  private Widget buildParameterWidget(final Element parameterElement)
  {
    final String parameterName = parameterElement.getAttribute("name"); //$NON-NLS-1$
    String renderType = parameterElement.getAttribute("parameter-render-type"); //$NON-NLS-1$
    if (renderType != null)
    {
      renderType = renderType.trim();
    }

    final boolean isStrict = "true".equalsIgnoreCase(parameterElement.getAttribute("is-strict")); //$NON-NLS-1$ //$NON-NLS-2$
    final NodeList choiceElements = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
    if (isStrict && choiceElements != null && choiceElements.getLength() == 0)
    {
      // if the parameter is strict but we have no valid choices for it, it is impossible
      // for the user to give it a value, so we will hide this parameter
      // it is highly likely that the parameter is driven by another parameter which
      // doesn't have a value yet, so eventually, we'll show this parameter.. we hope
      return null;
    }

    final NodeList selectionsElements = parameterElement.getElementsByTagName("selection"); //$NON-NLS-1$

    final List<String> parameterSelections = new ArrayList<String>();
    for (int i = 0; i < selectionsElements.getLength(); i++)
    {
      String selectionValue = ((Element) selectionsElements.item(i)).getAttribute("value"); //$NON-NLS-1$
      if (isStrict)
      {
        // we have to make sure the selectionValue is valid (in our list of known possible values)
        NodeList choices = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
        for (int j = 0; j < choices.getLength(); j++)
        {
          final Element choiceElement = (Element) choices.item(j);
          final String choiceValue = choiceElement.getAttribute("value"); //$NON-NLS-1$
          if (choiceValue.equals(selectionValue))
          {
            parameterSelections.add(selectionValue);
            break;
          }
        }
      }
      else
      {
        parameterSelections.add(selectionValue);
      }
    }
    parameterMap.put(parameterName, parameterSelections);

    // get default values
    final NodeList defaultValueElements = parameterElement.getElementsByTagName("default-value"); //$NON-NLS-1$

    // if there are no selections, add the defaults, if they exist while respecting strictness
    if (parameterSelections.isEmpty() && defaultValueElements != null && defaultValueElements.getLength() > 0)
    {
      for (int i = 0; i < defaultValueElements.getLength(); i++)
      {
        String defaultValue = ((Element) defaultValueElements.item(i)).getAttribute("value"); //$NON-NLS-1$

        if (isStrict)
        {
          // we have to make sure the selectionValue is valid (in our list of known possible values)
          NodeList choices = parameterElement.getElementsByTagName("value-choice"); //$NON-NLS-1$
          for (int j = 0; j < choices.getLength(); j++)
          {
            final Element choiceElement = (Element) choices.item(j);
            final String choiceValue = choiceElement.getAttribute("value"); //$NON-NLS-1$
            if (choiceValue.equals(defaultValue))
            {
              parameterSelections.add(defaultValue);
              break;
            }
          }
        }
        else
        {
          parameterSelections.add(defaultValue);
        }
      }
    }

    if ("radio".equalsIgnoreCase(renderType) || "checkbox".equalsIgnoreCase(renderType)) //$NON-NLS-1$ //$NON-NLS-2$
    {
      return new CheckBoxParameterUI(this, parameterSelections, parameterElement);
    }
    else if ("togglebutton".equalsIgnoreCase(renderType)) //$NON-NLS-1$
    {
      return new ToggleButtonParameterUI(this, parameterSelections, parameterElement);
    }
    else if ("list".equalsIgnoreCase(renderType) || "dropdown".equalsIgnoreCase(renderType)) //$NON-NLS-1$ //$NON-NLS-2$
    {
      return new ListParameterUI(this, parameterSelections, parameterElement);
    }
    else if ("datepicker".equalsIgnoreCase(renderType)) //$NON-NLS-1$
    {
      return new DateParameterUI(this, parameterSelections, parameterElement);
    }
    else
    {
      return new PlainParameterUI(this, parameterSelections, parameterElement);
    }
  }

  public void fetchParameters(boolean isOnChange)
  {
    if (isOnChange == false || (isOnChange && submitParametersOnChangeCheckBox.getValue()))
    {
      for (IParameterSubmissionListener listener : listeners)
      {
        listener.showBlank();
      }
      RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, viewer.buildReportUrl
          (RENDER_TYPE.XML, parameterMap, autoSubmitState));
      requestBuilder.setCallback(parameterRequestCallback);
      try
      {
        requestBuilder.send();
      } catch (RequestException re)
      {
        Window.alert(messages.getString("couldNotFetchParameters", "Could not fetch parameter metadata from server.")); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
  }

  private boolean promptNeeded()
  {
    // before we submit, let's check prompting needs
    boolean promptNeeded = false;
    String message = "<BR>"; //$NON-NLS-1$
    for (Element parameter : parameterElements)
    {
      if ("true".equals(parameter.getAttribute("is-mandatory"))) //$NON-NLS-1$ //$NON-NLS-2$
      {
        // then let's make sure we have a value for it
        List<String> paramList = parameterMap.get(parameter.getAttribute("name").trim()); //$NON-NLS-1$
        if (paramList == null || paramList.size() == 0)
        {
          promptNeeded = true;
          String paramTitle = parameter.getAttribute("label").trim(); //$NON-NLS-1$
          if (paramTitle == null || "".equals(paramTitle)) //$NON-NLS-1$
          {
            paramTitle = parameter.getAttribute("name").trim(); //$NON-NLS-1$
          }
          message += messages.getString("parameterMissing", "Parameter [{0}] is missing.", paramTitle); //$NON-NLS-1$ //$NON-NLS-2$
          message += "<BR>"; //$NON-NLS-1$
        }
      }
    }
    message += "<BR>"; //$NON-NLS-1$

    if (promptNeeded)
    {
      showMessageDialog(messages.getString("missingParameter", "Missing Parameter"), message); //$NON-NLS-1$ //$NON-NLS-2$
    }
    return promptNeeded;
  }

  private void showMessageDialog(String title, String message)
  {
    final DialogBox dialogBox = new DialogBox(false, true);
    dialogBox.setText(title);
    VerticalPanel dialogContent = new VerticalPanel();
    DOM.setStyleAttribute(dialogContent.getElement(), "padding", "0px 5px 0px 5px"); //$NON-NLS-1$ //$NON-NLS-2$
    dialogContent.add(new HTML(message, true));
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
    dialogBox.center();
    // prompt
  }

  public void addParameterSubmissionListener(IParameterSubmissionListener listener)
  {
    listeners.add(listener);
  }

  public void removeParameterSubmissionListener(IParameterSubmissionListener listener)
  {
    listeners.remove(listener);
  }

  private void fireParametersReady(Map<String, List<String>> parameterMap, RENDER_TYPE renderType)
  {
    for (IParameterSubmissionListener listener : listeners)
    {
      listener.parametersReady(parameterMap, renderType);
    }
  }

  private void firePromptNeeded()
  {
    for (IParameterSubmissionListener listener : listeners)
    {
      listener.showBlank();
    }
  }

  public Boolean isAutoSubmit()
  {
    return autoSubmitState;
  }

}
