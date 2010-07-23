package org.pentaho.reporting.platform.plugin;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.ISchedule;
import org.pentaho.platform.api.repository.ISubscribeContent;
import org.pentaho.platform.api.repository.ISubscription;
import org.pentaho.platform.api.repository.ISubscriptionRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.WebServiceUtil;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PlainTextPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.RTFTableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelTableModule;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterValues;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationMessage;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.util.NullOutputStream;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.engine.classic.core.util.beans.ValueConverter;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.platform.plugin.messages.Messages;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 16:24:30
 *
 * @author Thomas Morgner.
 */
public class ParameterXmlContentHandler
{
  private static final Log logger = LogFactory.getLog(ParameterXmlContentHandler.class);
  
  private ReportContentGenerator contentGenerator;
  private Document document;
  private ParameterContext parameterContext;
  private IParameterProvider requestParameters;
  private IPentahoSession userSession;

  public ParameterXmlContentHandler(final ReportContentGenerator contentGenerator)
  {
    this.contentGenerator = contentGenerator;
    this.requestParameters = contentGenerator.getRequestParameters();
    this.userSession = contentGenerator.getUserSession();
  }

  public IParameterProvider getRequestParameters()
  {
    return requestParameters;
  }


  public void createParameterContent(final OutputStream outputStream,
                                      final String reportDefinitionPath) throws Exception
  {
    document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final IParameterProvider requestParams = getRequestParameters();
    final Map<String, Object> inputs = contentGenerator.createInputs();

    final boolean subscribe = "true".equals(requestParams.getStringParameter("subscribe", "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // handle parameter feedback (XML) services

    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    reportComponent.setSession(userSession);
    reportComponent.setReportDefinitionPath(reportDefinitionPath);
    reportComponent.setPaginateOutput(true);
    reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
    reportComponent.setInputs(inputs);

    final MasterReport report = reportComponent.getReport();

    parameterContext = new DefaultParameterContext(report);
    try
    {
      // open parameter context
      parameterContext.open();
      // apply inputs to parameters
      ValidationResult validationResult = new ValidationResult();
      validationResult = reportComponent.applyInputsToReportParameters(parameterContext, validationResult);

      final ReportParameterDefinition reportParameterDefinition = report.getParameterDefinition();
      final ValidationResult vr = reportParameterDefinition.getValidator().validate(validationResult, reportParameterDefinition, parameterContext);

      final Element parameters = document.createElement("parameters"); //$NON-NLS-1$
      parameters.setAttribute("is-prompt-needed", String.valueOf(vr.isEmpty() == false)); //$NON-NLS-1$ //$NON-NLS-2$
      parameters.setAttribute("subscribe", String.valueOf(subscribe)); //$NON-NLS-1$ //$NON-NLS-2$

      // check if pagination is allowed and turned on
      if (vr.isEmpty()) //$NON-NLS-1$ //$NON-NLS-2$
      {
        appendPageCount(reportComponent, parameters);
      }

      final Boolean autoSubmitFlag = requestFlag("autoSubmit", report,
          AttributeNames.Core.NAMESPACE, AttributeNames.Core.AUTO_SUBMIT_PARAMETER,
          "org.pentaho.reporting.engine.classic.core.ParameterAutoSubmit");
      if (Boolean.TRUE.equals(autoSubmitFlag))
      {
        parameters.setAttribute("autoSubmit", "true");
      }
      else if (Boolean.FALSE.equals(autoSubmitFlag))
      {
        parameters.setAttribute("autoSubmit", "false");
      }

      final Boolean autoSubmitUiFlag = requestFlag("autoSubmitUI", report, // NON-NLS
          AttributeNames.Core.NAMESPACE, AttributeNames.Core.AUTO_SUBMIT_DEFAULT,
          "org.pentaho.reporting.engine.classic.core.ParameterAutoSubmitUI");
      if (Boolean.FALSE.equals(autoSubmitUiFlag))
      {
        parameters.setAttribute("autoSubmitUI", "false"); // NON-NLS
      }
      else
      {
        parameters.setAttribute("autoSubmitUI", "true"); // NON-NLS
      }

      final Boolean showParameterUI = requestFlag("showParameterUI", report, // NON-NLS
          AttributeNames.Core.NAMESPACE, AttributeNames.Core.SHOW_PARAMETER_UI, null);
      if (Boolean.FALSE.equals(showParameterUI))
      {
        parameters.setAttribute("show-parameter-ui", "false"); // NON-NLS
      }
      else
      {
        parameters.setAttribute("show-parameter-ui", "true"); // NON-NLS
      }

      parameters.setAttribute("layout", requestConfiguration("layout", report, // NON-NLS
          AttributeNames.Core.NAMESPACE, AttributeNames.Core.PARAMETER_UI_LAYOUT,
          "org.pentaho.reporting.engine.classic.core.ParameterUiLayout"));

      final ParameterDefinitionEntry[] parameterDefinitions = reportParameterDefinition.getParameterDefinitions();
      boolean outputTypeParameterSeen = false;
      for (final ParameterDefinitionEntry parameter : parameterDefinitions)
      {
        if (SimpleReportingComponent.OUTPUT_TARGET.equals(parameter.getName()))
        {
          outputTypeParameterSeen = true;
        }
        final Object selections = inputs.get(parameter.getName());
        final Element parameterElement = createParameterElement(parameter, selections);
        parameters.appendChild(parameterElement);
      }

      // now add output type chooser
      if (outputTypeParameterSeen == false)
      {
        final Object selections = reportComponent.getComputedOutputTarget();
        addOutputParameter(report, parameters, selections);
      }

      if (vr.isEmpty() == false)
      {
        parameters.appendChild(createErrorElements(vr));
      }

      // if we're going to attempt to handle subscriptions, add related choices as a parameter
      if (subscribe)
      {
        // add subscription choices, as a parameter (last in list)
        addSubscriptionParameter(reportDefinitionPath, parameters, inputs);
      }

      document.appendChild(parameters);
      WebServiceUtil.writeDocument(outputStream, document, false);
      // close parameter context
    }
    finally
    {
      parameterContext.close();
    }
  }


  private Element createParameterElement(final ParameterDefinitionEntry parameter,
                                         final Object selections) throws BeanException, ReportDataFactoryException
  {
    final Element parameterElement = document.createElement("parameter"); //$NON-NLS-1$
    parameterElement.setAttribute("name", parameter.getName()); //$NON-NLS-1$
    parameterElement.setAttribute("parameter-group", "parameters"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterElement.setAttribute("parameter-group-label", Messages.getString("ReportPlugin.ReportParameters")); //$NON-NLS-1$ //$NON-NLS-2$
    parameterElement.setAttribute("type", parameter.getValueType().getName()); //$NON-NLS-1$
    parameterElement.setAttribute("is-mandatory", String.valueOf(parameter.isMandatory())); //$NON-NLS-1$ //$NON-NLS-2$

    final String[] attributeNames = parameter.getParameterAttributeNames(ParameterAttributeNames.Core.NAMESPACE);
    for (final String attributeName : attributeNames)
    {
      final String attributeValue = parameter.getParameterAttribute(ParameterAttributeNames.Core.NAMESPACE, attributeName, parameterContext);
      // expecting: label, parameter-render-type, parameter-layout
      // but others possible as well, so we set them all
      parameterElement.setAttribute(attributeName, attributeValue);
    }

    final HashSet<Object> selectionSet = new HashSet<Object>();
    if (selections != null)
    {
      if (selections.getClass().isArray())
      {
        final int length = Array.getLength(selections);
        for (int i = 0; i < length; i++)
        {
          final Object value = Array.get(selections, i);
          selectionSet.add(value);
        }
      }
      else
      {
        selectionSet.add(selections);
      }
    }

    if (parameter instanceof ListParameter)
    {
      final ListParameter asListParam = (ListParameter) parameter;
      parameterElement.setAttribute("is-multi-select", String.valueOf(asListParam.isAllowMultiSelection())); //$NON-NLS-1$ //$NON-NLS-2$
      parameterElement.setAttribute("is-strict", String.valueOf(asListParam.isStrictValueCheck())); //$NON-NLS-1$ //$NON-NLS-2$

      final Element valuesElement = document.createElement("values"); //$NON-NLS-1$
      parameterElement.appendChild(valuesElement);

      final Class valueType;
      if (asListParam.isAllowMultiSelection() && parameter.getValueType().isArray())
      {
        valueType = parameter.getValueType().getComponentType();
      }
      else
      {
        valueType = parameter.getValueType();
      }

      final ParameterValues possibleValues = asListParam.getValues(parameterContext);
      for (int i = 0; i < possibleValues.getRowCount(); i++)
      {
        final Object key = possibleValues.getKeyValue(i);
        final Object value = possibleValues.getTextValue(i);

        if (key == null)
        {
          continue;
        }

        final Element valueElement = document.createElement("value"); //$NON-NLS-1$
        valuesElement.appendChild(valueElement);

        valueElement.setAttribute("label", String.valueOf(value)); //$NON-NLS-1$ //$NON-NLS-2$
        valueElement.setAttribute("type", valueType.getName()); //$NON-NLS-1$
        valueElement.setAttribute("selected", String.valueOf(selectionSet.contains(value)));//$NON-NLS-1$

        if (value == null)
        {
          valueElement.setAttribute("null", "true"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
        {
          valueElement.setAttribute("null", "false"); //$NON-NLS-1$ //$NON-NLS-2$
          valueElement.setAttribute("value", convertParameterValueToString(key, valueType)); //$NON-NLS-1$ //$NON-NLS-2$
        }
      }
    }
    else if (parameter instanceof PlainParameter)
    {
      // apply defaults, this is the easy case
      parameterElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      parameterElement.setAttribute("is-strict", "false"); //$NON-NLS-1$ //$NON-NLS-2$

      if (selections != null)
      {
        final Element valuesElement = document.createElement("values"); //$NON-NLS-1$
        parameterElement.appendChild(valuesElement);

        final Element valueElement = document.createElement("value"); //$NON-NLS-1$
        valuesElement.appendChild(valueElement);

        final Class valueType = parameter.getValueType();
        valueElement.setAttribute("type", valueType.getName()); //$NON-NLS-1$
        valueElement.setAttribute("selected", "true");//$NON-NLS-1$
        valueElement.setAttribute("null", "false"); //$NON-NLS-1$ //$NON-NLS-2$
        valueElement.setAttribute("value", convertParameterValueToString(selections, valueType)); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    return parameterElement;
  }

  private String convertParameterValueToString(final Object value, final Class type) throws BeanException
  {
    if (value == null)
    {
      return null;
    }

    final ValueConverter valueConverter = ConverterRegistry.getInstance().getValueConverter(type);
    if (valueConverter == null)
    {
      return String.valueOf(value);
    }
    if (Date.class.isAssignableFrom(type))
    {
      if (value instanceof Date == false)
      {
        throw new BeanException(Messages.getString("ReportPlugin.errorNonDateParameterValue"));
      }
      final Date d = (Date) value;
      final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");//$NON-NLS-1$
      return dateFormat.format(d);
    }
    if (Number.class.isAssignableFrom(type))
    {
      final ValueConverter numConverter = ConverterRegistry.getInstance().getValueConverter(BigDecimal.class);
      return numConverter.toAttributeValue(new BigDecimal(String.valueOf(value)));
    }
    return valueConverter.toAttributeValue(value);
  }

  private Element createErrorElements(final ValidationResult vr)
  {
    final Element errors = document.createElement("errors"); //$NON-NLS-1$
    for (final String property : vr.getProperties())
    {
      for (final ValidationMessage message : vr.getErrors(property))
      {
        final Element error = document.createElement("error"); //$NON-NLS-1$
        error.setAttribute("parameter", property);//$NON-NLS-1$
        error.setAttribute("message", message.getMessage());//$NON-NLS-1$
        errors.appendChild(error);
      }
    }
    final ValidationMessage[] globalMessages = vr.getErrors();
    for (int i = 0; i < globalMessages.length; i++)
    {
      final ValidationMessage globalMessage = globalMessages[i];
      final Element error = document.createElement("global-error"); //$NON-NLS-1$
      error.setAttribute("message", globalMessage.getMessage());//$NON-NLS-1$
      errors.appendChild(error);
    }
    return errors;
  }

  private static void appendPageCount(final SimpleReportingComponent reportComponent, final Element parameters)
      throws Exception
  {
    if (HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(reportComponent.getComputedOutputTarget()) == false)
    {
      return;
    }

    reportComponent.setOutputStream(new NullOutputStream());
    // pagination always uses HTML
    reportComponent.setOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);

    // so that we don't actually produce anything, we'll accept no pages in this mode
    final int acceptedPage = reportComponent.getAcceptedPage();
    reportComponent.setAcceptedPage(-1);

    // we can ONLY get the # of pages by asking the report to run
    if (reportComponent.validate())
    {
      final int totalPageCount = reportComponent.paginate();
      parameters.setAttribute(SimpleReportingComponent.PAGINATE_OUTPUT, "true"); //$NON-NLS-1$
      parameters.setAttribute("page-count", String.valueOf(totalPageCount)); //$NON-NLS-1$ //$NON-NLS-2$
      // use the saved value (we changed it to -1 for performance)
      parameters.setAttribute(SimpleReportingComponent.ACCEPTED_PAGE, String.valueOf(acceptedPage)); //$NON-NLS-1$
    }
  }


  private void addSubscriptionParameter(final String reportDefinitionPath,
                                        final Element parameters,
                                        final Map<String, Object> inputs)
  {
    final ISubscription subscription = contentGenerator.getSubscription();

    final Document document = parameters.getOwnerDocument();

    final Element reportNameParameter = document.createElement("parameter"); //$NON-NLS-1$
    parameters.appendChild(reportNameParameter);
    reportNameParameter.setAttribute("name", "subscription-name"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("label", Messages.getString("ReportPlugin.ReportName")); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("parameter-group", "subscription"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute(
        "parameter-group-label", Messages.getString("ReportPlugin.ReportSchedulingOptions")); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("is-mandatory", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("is-strict", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("parameter-render-type", "textbox"); //$NON-NLS-1$ //$NON-NLS-2$

    Object reportNameSelection = inputs.get("subscription-name"); //$NON-NLS-1$
    if (reportNameSelection == null && subscription != null)
    {
      // subscription helper will populate with this value, grr.
      reportNameSelection = subscription.getTitle();
    }
    if (reportNameSelection != null)
    {
      final Element selectionsElement = document.createElement("values"); //$NON-NLS-1$
      reportNameParameter.appendChild(selectionsElement);
      final Element selectionElement = document.createElement("value"); //$NON-NLS-1$
      selectionElement.setAttribute("value", reportNameSelection.toString()); //$NON-NLS-1$
      selectionElement.setAttribute("selected", "true");//$NON-NLS-1$
      selectionElement.setAttribute("type", "java.lang.String");//$NON-NLS-1$
      selectionsElement.appendChild(selectionElement);
    }

    final String email = PentahoSystem.getSystemSetting("smtp-email/email_config.xml", "mail.smtp.host", "");//$NON-NLS-1$
    if (StringUtils.isEmpty(email) == false)
    {

      // create email destination parameter
      final Element emailParameter = document.createElement("parameter"); //$NON-NLS-1$
      parameters.appendChild(emailParameter);
      emailParameter.setAttribute("name", "destination"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.Destination")); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("parameter-group", "subscription"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute(
          "parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportSchedulingOptions")); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-mandatory", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-strict", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("parameter-render-type", "textbox"); //$NON-NLS-1$ //$NON-NLS-2$

      Object destinationSelection = inputs.get("destination");//$NON-NLS-1$
      if (destinationSelection == null && subscription != null)
      {
        destinationSelection = subscription.getTitle();
      }
      if (destinationSelection != null)
      {
        final Element selectionsElement = document.createElement("values"); //$NON-NLS-1$
        emailParameter.appendChild(selectionsElement);
        final Element selectionElement = document.createElement("value"); //$NON-NLS-1$
        selectionElement.setAttribute("value", destinationSelection.toString()); //$NON-NLS-1$
        selectionElement.setAttribute("selected", "true");//$NON-NLS-1$
        selectionElement.setAttribute("type", "java.lang.String");//$NON-NLS-1$
        selectionsElement.appendChild(selectionElement);
      }
    }

    final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
    final ISubscribeContent subscribeContent = subscriptionRepository.getContentByActionReference(reportDefinitionPath);

    // add subscription choices, as a parameter (last in list)
    final Element subscriptionIdElement = document.createElement("parameter"); //$NON-NLS-1$
    parameters.appendChild(subscriptionIdElement);
    subscriptionIdElement.setAttribute("name", "schedule-id"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.Subscription")); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("parameter-group", "subscription"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute(
        "parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ScheduleReport")); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-mandatory", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-strict", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("parameter-render-type", "dropdown"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element valuesElement = document.createElement("values"); //$NON-NLS-1$
    subscriptionIdElement.appendChild(valuesElement);

    // if the user hasn't picked a schedule (to change this subscription to), and we
    // have a subscription active, get the schedules on it and add those
    final Object scheduleIdSelection = inputs.get("schedule-id"); //$NON-NLS-1$
    final HashSet<Object> selectedSchedules = new HashSet<Object>();
    if (scheduleIdSelection != null)
    {
      selectedSchedules.add(selectedSchedules);
    }
    else
    {
      if (subscription != null)
      {
        final List<ISchedule> schedules = subscription.getSchedules();
        for (final ISchedule schedule : schedules)
        {
          selectedSchedules.add(schedule.getId());
        }
      }
    }

    for (final ISchedule schedule : subscribeContent.getSchedules())
    {
      final Element valueElement = document.createElement("value"); //$NON-NLS-1$
      valuesElement.appendChild(valueElement);
      valueElement.setAttribute("label", schedule.getTitle()); //$NON-NLS-1$
      valueElement.setAttribute("value", schedule.getId()); //$NON-NLS-1$
      valueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
      valueElement.setAttribute("selected", String.valueOf(selectedSchedules.contains(schedule.getId())));//$NON-NLS-1$
    }

  }

  private void addOutputParameter(final MasterReport report,
                                  final Element parameters,
                                  final Object selections)
  {
    final Object lockOutputTypeObj = report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE);
    if (Boolean.TRUE.equals(lockOutputTypeObj)) //$NON-NLS-1$
    {
      // if the output type is locked, do not allow prompt rendering
      return;
    }

    final String selectedOutputType;
    if (selections != null)
    {
      selectedOutputType = selections.toString();
    }
    else
    {
      // use default, if available, from the report
      final String preferredOutputType = (String) report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
      if (!StringUtils.isEmpty(preferredOutputType))
      {
        selectedOutputType = preferredOutputType;
      }
      else
      {
        selectedOutputType = null;
      }
    }

    final Document document = parameters.getOwnerDocument();
    final Element parameterOutputElement = document.createElement("parameter"); //$NON-NLS-1$
    parameters.appendChild(parameterOutputElement);
    parameterOutputElement.setAttribute("name", SimpleReportingComponent.OUTPUT_TARGET); //$NON-NLS-1$
    parameterOutputElement.setAttribute("label", Messages.getString("ReportPlugin.OutputType")); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("parameter-group", "parameters"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("parameter-group-label", Messages.getString("ReportPlugin.ReportParameters")); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-mandatory", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-strict", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("parameter-render-type", "dropdown"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element valuesElement = document.createElement("values"); //$NON-NLS-1$
    parameterOutputElement.appendChild(valuesElement);

    final Element htmlPaginatedElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(htmlPaginatedElement);
    htmlPaginatedElement.setAttribute("label", "HTML (Paginated)"); //$NON-NLS-1$ //$NON-NLS-2$
    htmlPaginatedElement.setAttribute("value", HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE); //$NON-NLS-1$
    htmlPaginatedElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    htmlPaginatedElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE.equals(selectedOutputType)));

    final Element htmlValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(htmlValueElement);
    htmlValueElement.setAttribute("label", "HTML"); //$NON-NLS-1$ //$NON-NLS-2$
    htmlValueElement.setAttribute("value", HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE); //$NON-NLS-1$
    htmlValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    htmlValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(HtmlTableModule.TABLE_HTML_STREAM_EXPORT_TYPE.equals(selectedOutputType)));

    final Element pdfValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(pdfValueElement);
    pdfValueElement.setAttribute("label", "PDF"); //$NON-NLS-1$ //$NON-NLS-2$
    pdfValueElement.setAttribute("value", PdfPageableModule.PDF_EXPORT_TYPE); //$NON-NLS-1$
    pdfValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    pdfValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(PdfPageableModule.PDF_EXPORT_TYPE.equals(selectedOutputType)));

    final Element xlsValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(xlsValueElement);
    xlsValueElement.setAttribute("label", "Excel (XLS)"); //$NON-NLS-1$ //$NON-NLS-2$
    xlsValueElement.setAttribute("value", ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE); //$NON-NLS-1$
    xlsValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    xlsValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(ExcelTableModule.EXCEL_FLOW_EXPORT_TYPE.equals(selectedOutputType)));

    final Element csvValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(csvValueElement);
    csvValueElement.setAttribute("label", "CSV"); //$NON-NLS-1$ //$NON-NLS-2$
    csvValueElement.setAttribute("value", CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE); //$NON-NLS-1$
    csvValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    csvValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(CSVTableModule.TABLE_CSV_STREAM_EXPORT_TYPE.equals(selectedOutputType)));

    final Element rtfValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(rtfValueElement);
    rtfValueElement.setAttribute("label", "RTF"); //$NON-NLS-1$ //$NON-NLS-2$
    rtfValueElement.setAttribute("value", RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE); //$NON-NLS-1$
    rtfValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    rtfValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(RTFTableModule.TABLE_RTF_FLOW_EXPORT_TYPE.equals(selectedOutputType)));

    final Element txtValueElement = document.createElement("value"); //$NON-NLS-1$
    valuesElement.appendChild(txtValueElement);
    txtValueElement.setAttribute("label", "Plain Text"); //$NON-NLS-1$ //$NON-NLS-2$
    txtValueElement.setAttribute("value", PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE); //$NON-NLS-1$
    txtValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    txtValueElement.setAttribute("selected",//$NON-NLS-1$
        String.valueOf(PlainTextPageableModule.PLAINTEXT_EXPORT_TYPE.equals(selectedOutputType)));

  }

  private Boolean requestFlag(final String parameter,
                              final MasterReport report,
                              final String attributeNamespace, final String attributeName,
                              final String configurationKey)
  {
    if (parameter != null)
    {
      final IParameterProvider parameters = getRequestParameters();
      final String parameterValue = parameters.getStringParameter(parameter, "");
      if ("true".equals(parameterValue))
      {
        return Boolean.TRUE;
      }
      if ("false".equals(parameterValue))
      {
        return Boolean.FALSE;
      }
    }

    if (attributeNamespace != null && attributeName != null)
    {
      final Object attr = report.getAttribute(attributeNamespace, attributeName);
      if (Boolean.TRUE.equals(attr) || "true".equals(attr))
      {
        return Boolean.TRUE;
      }
      if (Boolean.FALSE.equals(attr) || "false".equals(attr))
      {
        return Boolean.FALSE;
      }
    }

    if (configurationKey != null)
    {
      final String configProperty = report.getConfiguration().getConfigProperty(configurationKey);
      if ("true".equals(configProperty))
      {
        return Boolean.TRUE;
      }
      if ("false".equals(configProperty))
      {
        return Boolean.FALSE;
      }
    }
    return null;
  }


  private String requestConfiguration(final String parameter,
                                       final MasterReport report,
                                       final String attributeNamespace, final String attributeName,
                                       final String configurationKey)
  {
    if (parameter != null)
    {
      final IParameterProvider parameters = getRequestParameters();
      final String parameterValue = parameters.getStringParameter(parameter, "");
      if (StringUtils.isEmpty(parameterValue) == false)
      {
        return parameterValue;
      }
    }

    if (attributeNamespace != null && attributeName != null)
    {
      final Object attr = report.getAttribute(attributeNamespace, attributeName);
      if (attr != null && StringUtils.isEmpty(String.valueOf(attr)) == false)
      {
        return String.valueOf(attr);
      }
    }

    if (configurationKey != null)
    {
      final String configProperty = report.getConfiguration().getConfigProperty(configurationKey);
      if (StringUtils.isEmpty(configProperty) == false)
      {
        return configProperty;
      }
    }
    return null;
  }
}
