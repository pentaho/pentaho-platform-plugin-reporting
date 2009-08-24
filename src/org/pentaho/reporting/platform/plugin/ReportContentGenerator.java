package org.pentaho.reporting.platform.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.actionsequence.dom.ActionSequenceDocument;
import org.pentaho.actionsequence.dom.IActionDefinition;
import org.pentaho.actionsequence.dom.IActionSequenceInput;
import org.pentaho.actionsequence.dom.IActionSequenceOutput;
import org.pentaho.actionsequence.dom.actions.PojoAction;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.repository.ISchedule;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.api.repository.ISubscribeContent;
import org.pentaho.platform.api.repository.ISubscription;
import org.pentaho.platform.api.repository.ISubscriptionRepository;
import org.pentaho.platform.engine.core.audit.AuditHelper;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.solution.ActionInfo;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.WebServiceUtil;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.platform.engine.services.solution.SimpleParameterSetter;
import org.pentaho.platform.repository.messages.Messages;
import org.pentaho.platform.repository.subscription.Subscription;
import org.pentaho.platform.repository.subscription.SubscriptionHelper;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.RTFTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultReportParameterValidator;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterValues;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.libraries.base.util.IOUtils;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

public class ReportContentGenerator extends SimpleContentGenerator
{
  private static final Log log = LogFactory.getLog(ReportContentGenerator.class);

  private RENDER_TYPE renderMode = RENDER_TYPE.REPORT;
  private SimpleReportingComponent reportComponent;

  public void createContent(OutputStream outputStream) throws Exception
  {
    final IParameterProvider requestParams = getRequestParameters();

    final String solution = URLDecoder.decode(requestParams.getStringParameter("solution", ""), "UTF-8"); //$NON-NLS-1$
    final String path = URLDecoder.decode(requestParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$
    final String name = URLDecoder.decode(requestParams.getStringParameter("name", requestParams.getStringParameter("action", "")), "UTF-8"); //$NON-NLS-1$
    final boolean subscribe = "true".equals(requestParams.getStringParameter("subscribe", "false"));

    renderMode = RENDER_TYPE.valueOf(requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase());

    final String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);

    final long start = System.currentTimeMillis();
    AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath, getObjectName(), getClass().getName(), MessageTypes.INSTANCE_START,
        renderMode.name() + ": " + instanceId, "", 0, this); //$NON-NLS-1$

    try
    {

      // create inputs from request parameters
      final Map<String, Object> inputs = createInputs(requestParams);

      if (renderMode.equals(RENDER_TYPE.DOWNLOAD))
      {
        final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
        final ISolutionFile file = repository.getSolutionFile(reportDefinitionPath, ISolutionRepository.ACTION_CREATE);

        final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse");
        response.setHeader("Content-Disposition", "attach; filename=\"" + file.getFileName() + "\"");
        response.setHeader("Content-Description", file.getFileName());
        response.setDateHeader("Last-Modified", file.getLastModified());

        // if the user has PERM_CREATE, we'll allow them to pull it for now, this is as relaxed
        // as I am comfortable with but I can imagine a PERM_READ or PERM_EXECUTE being used
        // in the future
        if (repository.hasAccess(file, ISolutionRepository.ACTION_CREATE) || repository.hasAccess(file, ISolutionRepository.ACTION_UPDATE))
        {
          IOUtils.getInstance().copyStreams(new ByteArrayInputStream(file.getData()), outputStream);
        }
      }
      else if (renderMode.equals(RENDER_TYPE.REPORT))
      {
        final ByteArrayOutputStream reportOutput = new ByteArrayOutputStream();
        // produce rendered report
        if (reportComponent == null)
        {
          reportComponent = new SimpleReportingComponent();
        }
        reportComponent.setSession(userSession);
        reportComponent.setOutputStream(reportOutput);
        reportComponent.setReportDefinitionPath(reportDefinitionPath);

        // the requested mime type can be null, in that case the report-component will resolve the desired
        // type from the output-target.
        final String mimeType = getMimeType(requestParams);
        reportComponent.setOutputType(mimeType);

        // add all inputs (request parameters) to report component
        reportComponent.setInputs(inputs);

        // If we haven't set an accepted page, -1 will be the default, which will give us a report
        // with no pages. This default is used so that when we do our parameter interaction with the
        // engine we can spend as little time as possible rendering unused pages, making it no pages.
        // We are going to intentionally reset the accepted page to the first page, 0, at this point,
        // if the accepted page is -1.
        if (reportComponent.isPaginateOutput() && reportComponent.getAcceptedPage() < 0)
        {
          reportComponent.setAcceptedPage(0);
        }

        if (reportComponent.validate())
        {
          if (reportComponent.execute())
          {
            IOUtils.getInstance().copyStreams(new ByteArrayInputStream(reportOutput.toByteArray()), outputStream);
            outputStream.flush();
          }
        }
        else
        {
          outputStream.write("Report validation failed.".getBytes());
          outputStream.flush();
        }

      }
      else if (renderMode.equals(RENDER_TYPE.SUBSCRIBE))
      {
        if (reportComponent == null)
        {
          reportComponent = new SimpleReportingComponent();
        }
        reportComponent.setSession(userSession);
        reportComponent.setReportDefinitionPath(reportDefinitionPath);
        final MasterReport report = reportComponent.getReport();
        final ParameterDefinitionEntry parameterDefinitions[] = report.getParameterDefinition().getParameterDefinitions();
        final String result = saveSubscription(requestParams, parameterDefinitions, reportDefinitionPath, userSession);
        outputStream.write(result.getBytes());
        outputStream.flush();
      }
      else if (renderMode.equals(RENDER_TYPE.XML))
      {
        // handle parameter feedback (XML) services
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        org.w3c.dom.Element parameters = document.createElement("parameters");
        document.appendChild(parameters);

        if (reportComponent == null)
        {
          reportComponent = new SimpleReportingComponent();
        }
        reportComponent.setSession(userSession);
        reportComponent.setReportDefinitionPath(reportDefinitionPath);
        reportComponent.setInputs(inputs);

        MasterReport report = reportComponent.getReport();

        ParameterContext parameterContext = new DefaultParameterContext(report);
        // open parameter context
        parameterContext.open();
        // apply inputs to parameters
        reportComponent.applyInputsToReportParameters(report, parameterContext);

        ParameterDefinitionEntry parameterDefinitions[] = report.getParameterDefinition().getParameterDefinitions();
        for (ParameterDefinitionEntry parameter : parameterDefinitions)
        {
          org.w3c.dom.Element parameterElement = document.createElement("parameter");
          parameters.appendChild(parameterElement);
          parameterElement.setAttribute("name", parameter.getName());
          parameterElement.setAttribute("parameter-group", "parameters");
          if (subscribe)
          {
            parameterElement.setAttribute("parameter-group-label", "Report Parameters");
          }
          parameterElement.setAttribute("type", parameter.getValueType().getName());
          parameterElement.setAttribute("is-mandatory", "" + parameter.isMandatory());

          Object defaultValue = parameter.getDefaultValue(parameterContext);
          if (defaultValue != null)
          {
            if (parameter.getValueType().isArray())
            {
              for (int i = 0; i < Array.getLength(defaultValue); i++)
              {
                org.w3c.dom.Element defaultValueElement = document.createElement("default-value");
                parameterElement.appendChild(defaultValueElement);
                defaultValueElement.setAttribute("value", Array.get(defaultValue, i).toString());
              }
            }
            else if (parameter.getValueType().isAssignableFrom(Date.class))
            {
              // dates are a special thing, in order to get the web (javascript) and the
              // server to be happy about date formats, the best thing for us to do
              // seems to be to convert to long (millis since epoch) since the javascript
              // land doesn't have the same date time formatter
              Date date = (Date) defaultValue;
              org.w3c.dom.Element defaultValueElement = document.createElement("default-value");
              parameterElement.appendChild(defaultValueElement);
              defaultValueElement.setAttribute("value", "" + date.getTime());
            }
            else
            {
              org.w3c.dom.Element defaultValueElement = document.createElement("default-value");
              parameterElement.appendChild(defaultValueElement);
              defaultValueElement.setAttribute("value", "" + defaultValue);
            }
          }

          String attributeNames[] = parameter.getParameterAttributeNames(ParameterAttributeNames.Core.NAMESPACE);
          for (String attributeName : attributeNames)
          {
            String attributeValue = parameter.getParameterAttribute(ParameterAttributeNames.Core.NAMESPACE, attributeName, parameterContext);
            // expecting: label, parameter-render-type, parameter-layout
            // but others possible as well, so we set them all
            parameterElement.setAttribute(attributeName, attributeValue);
          }

          Object selections = inputs.get(parameter.getName());
          if (selections != null)
          {
            org.w3c.dom.Element selectionsElement = document.createElement("selections");
            parameterElement.appendChild(selectionsElement);

            if (selections.getClass().isArray())
            {
              int length = Array.getLength(selections);
              for (int i = 0; i < length; i++)
              {
                Object value = Array.get(selections, i);
                org.w3c.dom.Element selectionElement = document.createElement("selection");
                selectionElement.setAttribute("value", value.toString());
                selectionsElement.appendChild(selectionElement);
              }
            }
            else
            {
              org.w3c.dom.Element selectionElement = document.createElement("selection");
              selectionElement.setAttribute("value", selections.toString());
              selectionsElement.appendChild(selectionElement);
            }
          }

          if (parameter instanceof ListParameter)
          {
            ListParameter asListParam = (ListParameter) parameter;
            parameterElement.setAttribute("is-multi-select", "" + asListParam.isAllowMultiSelection());
            parameterElement.setAttribute("is-strict", "" + asListParam.isStrictValueCheck());

            org.w3c.dom.Element valuesElement = document.createElement("value-choices");
            parameterElement.appendChild(valuesElement);

            ParameterValues possibleValues = asListParam.getValues(parameterContext);
            for (int i = 0; i < possibleValues.getRowCount(); i++)
            {
              Object key = possibleValues.getKeyValue(i);
              Object value = possibleValues.getTextValue(i);

              org.w3c.dom.Element valueElement = document.createElement("value-choice");
              valuesElement.appendChild(valueElement);

              // set
              if (key != null && value != null)
              {
                valueElement.setAttribute("label", "" + value);
                valueElement.setAttribute("value", "" + key);
                valueElement.setAttribute("type", key.getClass().getName());
              }
            }
          }
          else if (parameter instanceof PlainParameter)
          {
            // apply defaults, this is the easy case
            parameterElement.setAttribute("is-multi-select", "false");
            parameterElement.setAttribute("is-strict", "false");
          }
        }
        if (report.getParameterDefinition() instanceof DefaultParameterDefinition)
        {
          ((DefaultParameterDefinition) report.getParameterDefinition()).setValidator(new DefaultReportParameterValidator());
        }
        ValidationResult vr = report.getParameterDefinition().getValidator()
            .validate(new ValidationResult(), report.getParameterDefinition(), parameterContext);
        parameters.setAttribute("is-prompt-needed", "" + !vr.isEmpty());
        parameters.setAttribute("subscribe", "" + subscribe);

        // now add output type chooser
        addOutputParameter(report, parameters, inputs, subscribe);

        String mimeType = getMimeType(requestParams);

        // check if pagination is allowed and turned on
        if (mimeType.equalsIgnoreCase(SimpleReportingComponent.MIME_TYPE_HTML) && vr.isEmpty()
            && "true".equalsIgnoreCase(requestParams.getStringParameter(SimpleReportingComponent.PAGINATE_OUTPUT, "true")))
        {
          ByteArrayOutputStream dontCareOutputStream = new ByteArrayOutputStream();
          reportComponent.setOutputStream(dontCareOutputStream);
          // pagination always uses HTML
          reportComponent.setOutputType(SimpleReportingComponent.MIME_TYPE_HTML);

          // so that we don't actually produce anything, we'll accept no pages in this mode
          int acceptedPage = reportComponent.getAcceptedPage();
          reportComponent.setAcceptedPage(-1);

          // we can ONLY get the # of pages by asking the report to run
          if (reportComponent.isPaginateOutput() && reportComponent.validate())
          {
            reportComponent.execute();
            parameters.setAttribute(SimpleReportingComponent.PAGINATE_OUTPUT, "true");
            parameters.setAttribute("page-count", "" + reportComponent.getPageCount());
            // use the saved value (we changed it to -1 for performance)
            parameters.setAttribute(SimpleReportingComponent.ACCEPTED_PAGE, "" + acceptedPage);
          }
        }

        // if we're going to attempt to handle subscriptions, add related choices as a parameter
        if (subscribe)
        {
          // add subscription choices, as a parameter (last in list)
          addSubscriptionParameter(reportDefinitionPath, parameters, inputs);
        }

        WebServiceUtil.writeDocument(outputStream, document, false);
        // close parameter context
        parameterContext.close();
      }
      reportComponent = null;

      long end = System.currentTimeMillis();
      AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath, getObjectName(), getClass().getName(), MessageTypes.INSTANCE_END,
          renderMode.name() + ": " + instanceId, "", ((float) (end - start) / 1000), this); //$NON-NLS-1$
    } catch (Exception ex)
    {
      log.error(ex.getMessage(), ex);
      long end = System.currentTimeMillis();
      AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath, getObjectName(), getClass().getName(), MessageTypes.INSTANCE_FAILED,
          renderMode.name() + ": " + instanceId, "", ((float) (end - start) / 1000), this); //$NON-NLS-1$
      outputStream.write(ex.getMessage().getBytes("UTF-8"));
      outputStream.flush();
    }
  }

  private ISubscription getSubscription()
  {
    ISubscription subscription = null;
    String subscriptionId = getRequestParameters().getStringParameter("subscription-id", null);
    if (!StringUtils.isEmpty(subscriptionId))
    {
      ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
      subscription = subscriptionRepository.getSubscription(subscriptionId, userSession);
    }
    return subscription;
  }

  /**
   * Safely get our request parameters, while respecting any parameters hooked up to a subscription
   * 
   * @return IParameterProvider the provider of parameters
   */
  private IParameterProvider getRequestParameters()
  {
    IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);

    String subscriptionId = requestParams.getStringParameter("subscription-id", null);
    if (!StringUtils.isEmpty(subscriptionId))
    {
      ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
      ISubscription subscription = subscriptionRepository.getSubscription(subscriptionId, userSession);
      ISubscribeContent content = subscription.getContent();

      Map<String, Object> contentParameters = content.getParameters();
      SimpleParameterSetter parameters = new SimpleParameterSetter();
      parameters.setParameters(contentParameters);

      // add solution,path,name
      ActionInfo info = ActionInfo.parseActionString(content.getActionReference());
      parameters.setParameter("solution", info.getSolutionName());
      parameters.setParameter("path", info.getPath());
      parameters.setParameter("name", info.getActionName());

      SubscriptionHelper.getSubscriptionParameters(subscriptionId, parameters, userSession);

      // add all parameters that were on the url, if any, they will override subscription (editing)
      Iterator requestParamIterator = requestParams.getParameterNames();
      while (requestParamIterator.hasNext())
      {
        String param = (String) requestParamIterator.next();
        parameters.setParameter(param, requestParams.getParameter(param));
      }

      requestParams = parameters;
    }
    return requestParams;
  }

  private String saveSubscription(final IParameterProvider parameterProvider, final ParameterDefinitionEntry parameterDefinitions[],
      final String actionReference, final IPentahoSession userSession)
  {

    if ((userSession == null) || (userSession.getName() == null))
    {
      return Messages.getString("SubscriptionHelper.USER_LOGIN_NEEDED"); //$NON-NLS-1$
    }

    String subscriptionName = (String) parameterProvider.getParameter("subscription-name"); //$NON-NLS-1$

    ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);

    ISubscription subscription = getSubscription();
    if (subscription == null)
    {
      boolean isUniqueName = subscriptionRepository.checkUniqueSubscriptionName(subscriptionName, userSession.getName(), actionReference);
      if (!isUniqueName)
      {
        return Messages.getString("SubscriptionHelper.USER_SUBSCRIPTION_NAME_ALREADY_EXISTS", subscriptionName); //$NON-NLS-1$
      }
    }

    ISubscribeContent content = subscriptionRepository.getContentByActionReference(actionReference);
    if (content == null)
    {
      return (Messages.getString("SubscriptionHelper.ACTION_SEQUENCE_NOT_ALLOWED", parameterProvider.getStringParameter("name", ""))); //$NON-NLS-1$
    }

    HashMap parameters = new HashMap();

    for (ParameterDefinitionEntry parameter : parameterDefinitions)
    {
      String parameterName = parameter.getName();
      Object parameterValue = parameterProvider.getParameter(parameterName);
      if (parameterValue != null)
      {
        parameters.put(parameterName, parameterValue);
      }
    }
    parameters.put(SimpleReportingComponent.OUTPUT_TYPE, parameterProvider.getParameter(SimpleReportingComponent.OUTPUT_TYPE));

    String destination = (String) parameterProvider.getParameter("destination");
    if (subscription == null)
    {
      // create a new subscription
      String subscriptionId = UUIDUtil.getUUIDAsString();
      subscription = new Subscription(subscriptionId, userSession.getName(), subscriptionName, content, destination, Subscription.TYPE_PERSONAL, parameters);
    }
    else
    {
      subscription.setTitle(subscriptionName);
      subscription.setDestination(destination);
      subscription.getParameters().clear();
      subscription.getParameters().putAll(parameters);
      subscription.getSchedules().clear();
    }

    // now add the schedules
    List schedules = subscriptionRepository.getSchedules();
    for (int i = 0; i < schedules.size(); i++)
    {
      ISchedule schedule = (ISchedule) schedules.get(i);
      String scheduleId = schedule.getId();
      String scheduleIdParam = (String) parameterProvider.getParameter("schedule-id"); //$NON-NLS-1$
      if (scheduleId.equals(scheduleIdParam))
      { //$NON-NLS-1$
        subscription.addSchedule(schedule);
      }
    }

    if (subscriptionRepository.addSubscription(subscription))
    {
      return Messages.getString("SubscriptionHelper.USER_SUBSCRIPTION_CREATED"); //$NON-NLS-1$
    }
    else
    {
      // TODO log an error
      return Messages.getString("SubscriptionHelper.USER_SUBSCRIPTION_NOT_CREATE"); //$NON-NLS-1$
    }
  }

  private void addSubscriptionParameter(String reportDefinitionPath, org.w3c.dom.Element parameters, Map<String, Object> inputs)
  {
    ISubscription subscription = getSubscription();

    org.w3c.dom.Document document = parameters.getOwnerDocument();
    org.w3c.dom.Element reportNameParameter = document.createElement("parameter");
    parameters.appendChild(reportNameParameter);
    reportNameParameter.setAttribute("name", "subscription-name");
    reportNameParameter.setAttribute("label", "Report Name");
    reportNameParameter.setAttribute("parameter-group", "subscription");
    reportNameParameter.setAttribute("parameter-group-label", "Report Scheduling Options");
    reportNameParameter.setAttribute("type", "java.lang.String");
    reportNameParameter.setAttribute("is-mandatory", "true");
    reportNameParameter.setAttribute("is-multi-select", "false");
    reportNameParameter.setAttribute("is-strict", "false");
    reportNameParameter.setAttribute("parameter-render-type", "textbox");

    Object reportNameSelection = inputs.get("subscription-name");
    if (reportNameSelection == null && subscription != null)
    {
      // subscription helper will populate with this value, grr.
      reportNameSelection = subscription.getTitle();
    }
    if (reportNameSelection != null)
    {
      org.w3c.dom.Element selectionsElement = document.createElement("selections");
      reportNameParameter.appendChild(selectionsElement);
      org.w3c.dom.Element selectionElement = document.createElement("selection");
      selectionElement.setAttribute("value", reportNameSelection.toString());
      selectionsElement.appendChild(selectionElement);
    }

    ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
    ISubscribeContent subscribeContent = subscriptionRepository.getContentByActionReference(reportDefinitionPath);

    // add subscription choices, as a parameter (last in list)
    org.w3c.dom.Element subscriptionIdElement = document.createElement("parameter");
    parameters.appendChild(subscriptionIdElement);
    subscriptionIdElement.setAttribute("name", "schedule-id");
    subscriptionIdElement.setAttribute("label", "Subscription");
    subscriptionIdElement.setAttribute("parameter-group", "subscription");
    subscriptionIdElement.setAttribute("parameter-group-label", "Schedule Report");
    subscriptionIdElement.setAttribute("type", "java.lang.String");
    subscriptionIdElement.setAttribute("is-mandatory", "true");
    subscriptionIdElement.setAttribute("is-multi-select", "false");
    subscriptionIdElement.setAttribute("is-strict", "true");
    subscriptionIdElement.setAttribute("parameter-render-type", "dropdown");

    org.w3c.dom.Element valuesElement = document.createElement("value-choices");
    subscriptionIdElement.appendChild(valuesElement);

    for (ISchedule schedule : subscribeContent.getSchedules())
    {
      org.w3c.dom.Element valueElement = document.createElement("value-choice");
      valuesElement.appendChild(valueElement);
      valueElement.setAttribute("label", schedule.getTitle());
      valueElement.setAttribute("value", schedule.getId());
      valueElement.setAttribute("type", "java.lang.String");
    }

    // selections (schedules)
    org.w3c.dom.Element selectionsElement = document.createElement("selections");
    subscriptionIdElement.appendChild(selectionsElement);

    Object scheduleIdSelection = inputs.get("schedule-id");
    if (scheduleIdSelection != null)
    {
      org.w3c.dom.Element selectionElement = document.createElement("selection");
      selectionElement.setAttribute("value", scheduleIdSelection.toString());
      selectionsElement.appendChild(selectionElement);
    }

    // if the user hasn't picked a schedule (to change this subscription to), and we
    // have a subscription active, get the schedules on it and add those
    if (scheduleIdSelection == null)
    {
      if (subscription != null)
      {
        List<ISchedule> schedules = subscription.getSchedules();
        for (ISchedule schedule : schedules)
        {
          org.w3c.dom.Element selectionElement = document.createElement("selection");
          selectionElement.setAttribute("value", schedule.getId());
          selectionsElement.appendChild(selectionElement);
        }
      }
    }
  }

  private void addOutputParameter(MasterReport report, org.w3c.dom.Element parameters, Map<String, Object> inputs, boolean subscribe)
  {
    Object lockOutputTypeObj = (Object) report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE);
    if (lockOutputTypeObj != null && "true".equalsIgnoreCase(lockOutputTypeObj.toString()))
    {
      // if the output type is locked, do not allow prompt rendering
      return;
    }

    org.w3c.dom.Document document = parameters.getOwnerDocument();
    org.w3c.dom.Element parameterOutputElement = document.createElement("parameter");
    parameters.appendChild(parameterOutputElement);
    parameterOutputElement.setAttribute("name", SimpleReportingComponent.OUTPUT_TYPE);
    parameterOutputElement.setAttribute("label", "Output Type");
    parameterOutputElement.setAttribute("parameter-group", "parameters");
    if (subscribe)
    {
      parameterOutputElement.setAttribute("parameter-group-label", "Report Parameters");
    }
    parameterOutputElement.setAttribute("type", "java.lang.String");
    parameterOutputElement.setAttribute("is-mandatory", "true");
    parameterOutputElement.setAttribute("is-multi-select", "false");
    parameterOutputElement.setAttribute("is-strict", "true");
    parameterOutputElement.setAttribute("parameter-render-type", "dropdown");

    org.w3c.dom.Element valuesElement = document.createElement("value-choices");
    parameterOutputElement.appendChild(valuesElement);

    org.w3c.dom.Element htmlValueElement = document.createElement("value-choice");
    valuesElement.appendChild(htmlValueElement);
    htmlValueElement.setAttribute("label", "HTML");
    htmlValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_HTML);
    htmlValueElement.setAttribute("type", "java.lang.String");

    org.w3c.dom.Element pdfValueElement = document.createElement("value-choice");
    valuesElement.appendChild(pdfValueElement);
    pdfValueElement.setAttribute("label", "PDF");
    pdfValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_PDF);
    pdfValueElement.setAttribute("type", "java.lang.String");

    org.w3c.dom.Element xlsValueElement = document.createElement("value-choice");
    valuesElement.appendChild(xlsValueElement);
    xlsValueElement.setAttribute("label", "Excel (XLS)");
    xlsValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_XLS);
    xlsValueElement.setAttribute("type", "java.lang.String");

    org.w3c.dom.Element csvValueElement = document.createElement("value-choice");
    valuesElement.appendChild(csvValueElement);
    csvValueElement.setAttribute("label", "CSV");
    csvValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_CSV);
    csvValueElement.setAttribute("type", "java.lang.String");

    org.w3c.dom.Element rtfValueElement = document.createElement("value-choice");
    valuesElement.appendChild(rtfValueElement);
    rtfValueElement.setAttribute("label", "RTF");
    rtfValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_RTF);
    rtfValueElement.setAttribute("type", "java.lang.String");

    Object selections = inputs.get(SimpleReportingComponent.OUTPUT_TYPE);
    if (selections != null)
    {
      org.w3c.dom.Element selectionsElement = document.createElement("selections");
      parameterOutputElement.appendChild(selectionsElement);
      org.w3c.dom.Element selectionElement = document.createElement("selection");
      selectionElement.setAttribute("value", selections.toString());
      selectionsElement.appendChild(selectionElement);
    }
    else
    {
      // use default, if available, from the report
      String preferredOutputType = (String) report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
      if (!StringUtils.isEmpty(preferredOutputType))
      {
        org.w3c.dom.Element selectionsElement = document.createElement("selections");
        parameterOutputElement.appendChild(selectionsElement);
        org.w3c.dom.Element selectionElement = document.createElement("selection");
        selectionElement.setAttribute("value", MimeHelper.getMimeTypeFromExtension("." + preferredOutputType));
        selectionsElement.appendChild(selectionElement);
      }
    }
  }

  public String generateWrapperXaction()
  {
    IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);

    Iterator namesIt = requestParams.getParameterNames();
    while (namesIt.hasNext())
    {
      String name = (String) namesIt.next();
      Object value = requestParams.getParameter(name);
    }

    String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
    String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
    String name = requestParams.getStringParameter("action", null); //$NON-NLS-1$

    // sanitization
    final String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);
    // final ActionInfo actionInfo = ActionInfo.parseActionString(reportDefinitionPath);

    ActionSequenceDocument actionSequenceDocument = new ActionSequenceDocument();
    actionSequenceDocument.setTitle(reportDefinitionPath);
    actionSequenceDocument.setVersion("1");
    actionSequenceDocument.setAuthor("SolutionEngine");
    actionSequenceDocument.setDescription(reportDefinitionPath);
    actionSequenceDocument.setIconLocation("PentahoReporting.png");
    actionSequenceDocument.setHelp("");
    actionSequenceDocument.setResultType("report");
    IActionSequenceInput outputType = actionSequenceDocument.createInput("outputType", ActionSequenceDocument.STRING_TYPE);
    outputType.setDefaultValue("text/html");
    IActionSequenceOutput output = actionSequenceDocument.createOutput("outputstream", "content");
    output.addDestination("response", "content");

    try
    {
      // URI reportURI = new URI("solution:/" + actionInfo.getPath() + "/" + actionInfo.getActionName());
      // actionSequenceDocument.setResourceUri("reportDefinition", reportURI, "application/zip");
      IActionSequenceInput reportDefinitionPathInput = actionSequenceDocument.createInput("report-definition-path", ActionSequenceDocument.STRING_TYPE);
      reportDefinitionPathInput.setDefaultValue(reportDefinitionPath);

      IActionDefinition pojoComponent = actionSequenceDocument.addAction(PojoAction.class);
      pojoComponent.setComponentDefinition("class", SimpleReportingComponent.class.getName());
      pojoComponent.addOutput("outputstream", "content");
      pojoComponent.addInput("report-definition-path", "string");

      // add all prpt inputs
      if (reportComponent == null)
      {
        reportComponent = new SimpleReportingComponent();
      }
      reportComponent.setSession(userSession);
      reportComponent.setReportDefinitionPath(reportDefinitionPath);
      MasterReport report = reportComponent.getReport();
      ParameterDefinitionEntry parameterDefinitions[] = report.getParameterDefinition().getParameterDefinitions();
      for (ParameterDefinitionEntry parameter : parameterDefinitions)
      {
        ParameterContext parameterContext = new DefaultParameterContext(report);
        Object defaultValue = parameter.getDefaultValue(parameterContext);
        if (defaultValue != null)
        {
          IActionSequenceInput input = actionSequenceDocument.createInput(parameter.getName(), ActionSequenceDocument.STRING_TYPE);
          input.setDefaultValue(defaultValue.toString());
        }
        else if (requestParams.getParameter(parameter.getName()) != null)
        {
          IActionSequenceInput input = actionSequenceDocument.createInput(parameter.getName(), ActionSequenceDocument.STRING_TYPE);
          input.setDefaultValue(requestParams.getParameter(parameter.getName()).toString());
        }
        pojoComponent.addInput(parameter.getName(), "string");
      }
      pojoComponent.addInput("outputType", "string");

    } catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }

    return actionSequenceDocument.toString();
  }

  private Map<String, Object> createInputs(final IParameterProvider requestParams)
  {
    Map<String, Object> inputs = new HashMap<String, Object>();
    Iterator<String> paramIter = requestParams.getParameterNames();
    while (paramIter.hasNext())
    {
      String paramName = paramIter.next();
      Object paramValue = requestParams.getParameter(paramName);
      inputs.put(paramName, paramValue);
    }
    return inputs;
  }

  public Log getLogger()
  {
    return log;
  }

  private String getMimeType(final IParameterProvider requestParams)
  {
    String mimeType = requestParams.getStringParameter(SimpleReportingComponent.OUTPUT_TYPE, null);
    if (StringUtils.isEmpty(mimeType))
    {
      // set out default first, takes care of exception/else fall thru
      mimeType = SimpleReportingComponent.MIME_TYPE_HTML;
      try
      {
        final String preferredOutputTarget = (String) reportComponent.getReport().getAttribute(AttributeNames.Core.NAMESPACE,
            AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
        if (HtmlTableModule.TABLE_HTML_FLOW_EXPORT_TYPE.equals(preferredOutputTarget)
            || HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(preferredOutputTarget)
            || HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = SimpleReportingComponent.MIME_TYPE_HTML;
        }
        else if (CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = "text/csv";
        }
        else if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(preferredOutputTarget)
            || HtmlTableModule.TABLE_HTML_FLOW_EXPORT_TYPE.equals(preferredOutputTarget)
            || HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = "text/html";
        }
        else if (PdfPageableModule.PDF_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = "application/pdf";
        }
        else if (RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = "application/rtf";
        }
        else if (ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(preferredOutputTarget))
        {
          mimeType = "application/vnd.ms-excel";
        }
        else if (StringUtils.isEmpty(preferredOutputTarget) == false)
        {
          mimeType = preferredOutputTarget;
        }
      } catch (Exception e)
      {
        log.info(e.getMessage(), e);
      }
    }
    if ("pdf".equalsIgnoreCase(mimeType))
    {
      mimeType = SimpleReportingComponent.MIME_TYPE_PDF;
    }
    else if ("html".equalsIgnoreCase(mimeType))
    {
      mimeType = SimpleReportingComponent.MIME_TYPE_HTML;
    }
    else if ("csv".equalsIgnoreCase(mimeType))
    {
      mimeType = SimpleReportingComponent.MIME_TYPE_CSV;
    }
    else if ("rtf".equalsIgnoreCase(mimeType))
    {
      mimeType = SimpleReportingComponent.MIME_TYPE_RTF;
    }
    else if ("xls".equalsIgnoreCase(mimeType))
    {
      mimeType = SimpleReportingComponent.MIME_TYPE_XLS;
    }
    return mimeType;
  }

  public String getMimeType()
  {
    IParameterProvider requestParams = getRequestParameters();
    renderMode = RENDER_TYPE.valueOf(requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase());
    if (renderMode.equals(RENDER_TYPE.XML))
    {
      return "text/xml";
    }
    else if (renderMode.equals(RENDER_TYPE.SUBSCRIBE))
    {
      return SimpleReportingComponent.MIME_TYPE_HTML;
    }
    else if (renderMode.equals(RENDER_TYPE.DOWNLOAD))
    {
      // perhaps we can invent our own mime-type or use application/zip?
      return "application/octet-stream";
    }

    String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
    String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
    String name = requestParams.getStringParameter("name", requestParams.getStringParameter("action", null)); //$NON-NLS-1$
    String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);

    if (reportComponent == null)
    {
      reportComponent = new SimpleReportingComponent();
    }
    reportComponent.setSession(userSession);
    reportComponent.setReportDefinitionPath(reportDefinitionPath);

    return getMimeType(requestParams);
  }
}
