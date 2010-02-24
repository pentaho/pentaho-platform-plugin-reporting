package org.pentaho.reporting.platform.plugin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
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
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
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
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ReportContentGenerator extends SimpleContentGenerator
{
  private static final Log log = LogFactory.getLog(ReportContentGenerator.class);

  private SimpleReportingComponent reportComponent;

  public ReportContentGenerator()
  {
  }

  public void createContent(final OutputStream outputStream) throws Exception
  {
    final String id = UUIDUtil.getUUIDAsString();
    setInstanceId(id);
    final IParameterProvider requestParams = getRequestParameters();

    final String solution = URLDecoder.decode(requestParams.getStringParameter("solution", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final String path = URLDecoder.decode(requestParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    final String name = URLDecoder.decode(requestParams.getStringParameter("name", requestParams.getStringParameter("action", "")), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

    final RENDER_TYPE renderMode = RENDER_TYPE.valueOf
        (requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$

    final String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);

    final long start = System.currentTimeMillis();
    AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath,
        getObjectName(), getClass().getName(), MessageTypes.INSTANCE_START,
        instanceId, "", 0, this); //$NON-NLS-1$

    String result = MessageTypes.INSTANCE_END;
    try
    {
      switch (renderMode)
      {
        case DOWNLOAD:
        {
          createDownloadContent(outputStream, reportDefinitionPath);
          break;
        }
        case REPORT:
        {
          // create inputs from request parameters
          final Map<String, Object> inputs = createInputs(requestParams);
          createReportContent(outputStream, reportDefinitionPath, inputs);
          break;
        }
        case SUBSCRIBE:
        {
          createSubscribeContent(outputStream, reportDefinitionPath, requestParams);
          break;
        }
        case XML:
        {
          // create inputs from request parameters
          final Map<String, Object> inputs = createInputs(requestParams);
          createParameterContent(outputStream, reportDefinitionPath, requestParams, inputs);
          break;
        }
        default:
          throw new IllegalArgumentException();
      }
    }
    catch (Exception ex)
    {
      final String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
      log.error(exceptionMessage, ex);

      result = MessageTypes.INSTANCE_FAILED;

      if (outputStream != null)
      {
        outputStream.write(exceptionMessage.getBytes("UTF-8")); //$NON-NLS-1$
        outputStream.flush();
      }
      else
      {
        throw new IllegalArgumentException();
      }
    }
    finally
    {
      reportComponent = null;

      final long end = System.currentTimeMillis();
      AuditHelper.audit(userSession.getId(), userSession.getName(), reportDefinitionPath,
          getObjectName(), getClass().getName(), result,
          instanceId, "", ((float) (end - start) / 1000), this); //$NON-NLS-1$
    }
  }

  private void createReportContent(final OutputStream outputStream,
                                   final String reportDefinitionPath,
                                   final Map<String, Object> inputs)
      throws Exception
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
    // Hoever, the report-component will inspect the inputs independently from the mimetype here.

    final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
    final ISolutionFile file = repository.getSolutionFile(reportDefinitionPath, ISolutionRepository.ACTION_EXECUTE);


    // add all inputs (request parameters) to report component
    reportComponent.setInputs(inputs);
    final String mimeType = reportComponent.getMimeType();

    // If we haven't set an accepted page, -1 will be the default, which will give us a report
    // with no pages. This default is used so that when we do our parameter interaction with the
    // engine we can spend as little time as possible rendering unused pages, making it no pages.
    // We are going to intentionally reset the accepted page to the first page, 0, at this point,
    // if the accepted page is -1.
    if (reportComponent.isPaginateOutput() && reportComponent.getAcceptedPage() < 0)
    {
      reportComponent.setAcceptedPage(0);
    }

    if (log.isDebugEnabled())
    {
      log.debug(Messages.getString("ReportPlugin.logStartGenerateContent", mimeType,//$NON-NLS-1$
          String.valueOf(reportComponent.isPaginateOutput()), String.valueOf(reportComponent.getAcceptedPage())));
    }
    if (reportComponent.validate() &&
        reportComponent.execute())
    {
      final byte[] bytes = reportOutput.toByteArray();
      if (parameterProviders.get("path") != null &&
          parameterProviders.get("path").getParameter("httpresponse") != null) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      {
        final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse"); //$NON-NLS-1$ //$NON-NLS-2$
        final String extension = MimeHelper.getExtension(mimeType);
        String filename = file.getFileName();
        if (filename.lastIndexOf(".") != -1)
        { //$NON-NLS-1$
          filename = filename.substring(0, filename.lastIndexOf(".")); //$NON-NLS-1$
        }
        response.setHeader("Content-Disposition", "inline; filename=\"" + filename + extension + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Size", String.valueOf(bytes.length));
      }
      if (log.isDebugEnabled())
      {
        log.debug(Messages.getString("ReportPlugin.logEndGenerateContent", String.valueOf(bytes.length)));//$NON-NLS-1$
      }

      outputStream.write(bytes);
      outputStream.flush();
    }
    else
    {
      if (parameterProviders.get("path") != null &&
          parameterProviders.get("path").getParameter("httpresponse") != null) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      {
        final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse"); //$NON-NLS-1$ //$NON-NLS-2$
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
      if (log.isDebugEnabled())
      {
        log.debug(Messages.getString("ReportPlugin.logErrorGenerateContent"));//$NON-NLS-1$
      }
      outputStream.write(org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportValidationFailed").getBytes()); //$NON-NLS-1$
      outputStream.flush();
    }
  }

  private void createParameterContent(final OutputStream outputStream,
                                      final String reportDefinitionPath,
                                      final IParameterProvider requestParams,
                                      final Map<String, Object> inputs)
      throws Exception
  {
    final boolean subscribe = "true".equals(requestParams.getStringParameter("subscribe", "false")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    // handle parameter feedback (XML) services
    final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    final Element parameters = document.createElement("parameters"); //$NON-NLS-1$
    document.appendChild(parameters);

    if (reportComponent == null)
    {
      reportComponent = new SimpleReportingComponent();
    }
    reportComponent.setSession(userSession);
    reportComponent.setReportDefinitionPath(reportDefinitionPath);
    reportComponent.setInputs(inputs);

    final MasterReport report = reportComponent.getReport();

    final ParameterContext parameterContext = new DefaultParameterContext(report);
    // open parameter context
    parameterContext.open();
    // apply inputs to parameters
    ValidationResult validationResult = new ValidationResult();
    validationResult = reportComponent.applyInputsToReportParameters(parameterContext, validationResult);

    final ReportParameterDefinition reportParameterDefinition = report.getParameterDefinition();

    final ParameterDefinitionEntry[] parameterDefinitions = reportParameterDefinition.getParameterDefinitions();
    for (final ParameterDefinitionEntry parameter : parameterDefinitions)
    {
      final String hiddenVal = parameter.getParameterAttribute
          (ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.HIDDEN, parameterContext);
      if ("true".equals(hiddenVal))
      {
        continue;
      }

      final Element parameterElement =
          createParameterElement(subscribe, inputs, document, parameterContext, parameter);

      parameters.appendChild(parameterElement);
    }

    final ValidationResult vr = reportParameterDefinition.getValidator().validate
        (validationResult, reportParameterDefinition, parameterContext);
    parameters.setAttribute("is-prompt-needed", "" + !vr.isEmpty()); //$NON-NLS-1$ //$NON-NLS-2$
    parameters.setAttribute("subscribe", "" + subscribe); //$NON-NLS-1$ //$NON-NLS-2$

    // now add output type chooser
    addOutputParameter(report, parameters, inputs, subscribe);

    final String mimeType = reportComponent.getMimeType();

    // check if pagination is allowed and turned on
    if (mimeType.equalsIgnoreCase(SimpleReportingComponent.MIME_TYPE_HTML) && vr.isEmpty()
        && "true".equalsIgnoreCase(requestParams.getStringParameter(SimpleReportingComponent.PAGINATE_OUTPUT, "true"))) //$NON-NLS-1$ //$NON-NLS-2$
    {
      final NullOutputStream dontCareOutputStream = new NullOutputStream();
      reportComponent.setOutputStream(dontCareOutputStream);
      // pagination always uses HTML
      reportComponent.setOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);

      // so that we don't actually produce anything, we'll accept no pages in this mode
      final int acceptedPage = reportComponent.getAcceptedPage();
      reportComponent.setAcceptedPage(-1);

      // we can ONLY get the # of pages by asking the report to run
      if (reportComponent.isPaginateOutput() && reportComponent.validate())
      {
        reportComponent.execute();
        parameters.setAttribute(SimpleReportingComponent.PAGINATE_OUTPUT, "true"); //$NON-NLS-1$
        parameters.setAttribute("page-count", String.valueOf(reportComponent.getPageCount())); //$NON-NLS-1$ //$NON-NLS-2$
        // use the saved value (we changed it to -1 for performance)
        parameters.setAttribute(SimpleReportingComponent.ACCEPTED_PAGE, "" + acceptedPage); //$NON-NLS-1$
      }
    }
    else if (vr.isEmpty() == false)
    {
      final Element errors = document.createElement("errors"); //$NON-NLS-1$
      parameters.appendChild(errors);
      for (final String property : vr.getProperties())
      {
        for (final ValidationMessage message : vr.getErrors(property))
        {
          final Element error = document.createElement("error"); //$NON-NLS-1$
          error.setAttribute("parameter", property);
          error.setAttribute("message", message.getMessage());
          errors.appendChild(error);
        }
      }
      final ValidationMessage[] globalMessages = vr.getErrors();
      for (int i = 0; i < globalMessages.length; i++)
      {
        final ValidationMessage globalMessage = globalMessages[i];
        final Element error = document.createElement("global-error"); //$NON-NLS-1$
        error.setAttribute("message", globalMessage.getMessage());
        errors.appendChild(error);
      }
    }

    final String autoSubmitStr = requestParams.getStringParameter("autoSubmit", null);
    if ("true".equals(autoSubmitStr))
    {
      parameters.setAttribute("autoSubmit", "true");
    }
    else if ("false".equals(autoSubmitStr))
    {
      parameters.setAttribute("autoSubmit", "false");
    }
    else
    {
      final Object o = report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.AUTO_SUBMIT_PARAMETER);
      if(Boolean.TRUE.equals(o))
      {
        parameters.setAttribute("autoSubmit", "true");
      }
      else if(Boolean.FALSE.equals(o))
      {
        parameters.setAttribute("autoSubmit", "false");
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

  private void createSubscribeContent(final OutputStream outputStream,
                                      final String reportDefinitionPath,
                                      final IParameterProvider requestParams)
      throws ResourceException, IOException
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

  private void createDownloadContent(final OutputStream outputStream, final String reportDefinitionPath)
      throws IOException
  {
    final ISolutionRepository repository = PentahoSystem.get(ISolutionRepository.class, userSession);
    final ISolutionFile file = repository.getSolutionFile(reportDefinitionPath, ISolutionRepository.ACTION_CREATE);
    final HttpServletResponse response = (HttpServletResponse) parameterProviders.get("path").getParameter("httpresponse"); //$NON-NLS-1$ //$NON-NLS-2$

    // if the user has PERM_CREATE, we'll allow them to pull it for now, this is as relaxed
    // as I am comfortable with but I can imagine a PERM_READ or PERM_EXECUTE being used
    // in the future
    if (file.isDirectory() == false && file.isRoot() == false &&
        repository.hasAccess(file, ISolutionRepository.ACTION_CREATE) ||
        repository.hasAccess(file, ISolutionRepository.ACTION_UPDATE))
    {
      final byte[] data = file.getData();
      if (data == null)
      {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      }
      else
      {
        response.setHeader("Content-Disposition", "attach; filename=\"" + file.getFileName() + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        response.setHeader("Content-Description", file.getFileName()); //$NON-NLS-1$
        response.setDateHeader("Last-Modified", file.getLastModified()); //$NON-NLS-1$
        response.setContentLength(data.length); //$NON-NLS-1$
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);

        outputStream.write(data);
      }
    }
    else
    {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
  }

  private Element createParameterElement(final boolean subscribe,
                                         final Map<String,Object> inputs,
                                         final Document document,
                                         final ParameterContext parameterContext,
                                         final ParameterDefinitionEntry parameter)
      throws BeanException, ReportDataFactoryException
  {
    final Element parameterElement = document.createElement("parameter"); //$NON-NLS-1$
    parameterElement.setAttribute("name", parameter.getName()); //$NON-NLS-1$
    parameterElement.setAttribute("parameter-group", "parameters"); //$NON-NLS-1$ //$NON-NLS-2$
    if (subscribe)
    {
      parameterElement.setAttribute("parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportParameters")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    parameterElement.setAttribute("type", parameter.getValueType().getName()); //$NON-NLS-1$
    parameterElement.setAttribute("is-mandatory", "" + parameter.isMandatory()); //$NON-NLS-1$ //$NON-NLS-2$

    final Object defaultValue = parameter.getDefaultValue(parameterContext);
    final Class declaredValueType = parameter.getValueType();
    if (defaultValue != null)
    {
      if (declaredValueType.isArray())
      {
        if (defaultValue.getClass().isArray())
        {
          final int length = Array.getLength(defaultValue);
          for (int i = 0; i < length; i++)
          {
            final Element defaultValueElement = document.createElement("default-value"); //$NON-NLS-1$
            parameterElement.appendChild(defaultValueElement);
            defaultValueElement.setAttribute("value",
                convertParameterValueToString(Array.get(defaultValue, i), declaredValueType.getComponentType())); //$NON-NLS-1$
          }
        }
        else
        {
          final Element defaultValueElement = document.createElement("default-value"); //$NON-NLS-1$
          parameterElement.appendChild(defaultValueElement);
          defaultValueElement.setAttribute("value",
              convertParameterValueToString(defaultValue, declaredValueType.getComponentType())); //$NON-NLS-1$
        }
      }
      else if (declaredValueType.isAssignableFrom(Date.class))
      {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        final Date date = (Date) defaultValue;
        final Element defaultValueElement = document.createElement("default-value"); //$NON-NLS-1$
        parameterElement.appendChild(defaultValueElement);
        defaultValueElement.setAttribute("value", sdf.format(date)); //$NON-NLS-1$ //$NON-NLS-2$
      }
      else
      {
        final Element defaultValueElement = document.createElement("default-value"); //$NON-NLS-1$
        parameterElement.appendChild(defaultValueElement);
        defaultValueElement.setAttribute("value",
            convertParameterValueToString(defaultValue, declaredValueType)); //$NON-NLS-1$
      }
    }

    final String[] attributeNames = parameter.getParameterAttributeNames(ParameterAttributeNames.Core.NAMESPACE);
    for (final String attributeName : attributeNames)
    {
      final String attributeValue = parameter.getParameterAttribute(ParameterAttributeNames.Core.NAMESPACE, attributeName, parameterContext);
      // expecting: label, parameter-render-type, parameter-layout
      // but others possible as well, so we set them all
      parameterElement.setAttribute(attributeName, attributeValue);
    }

    final Object selections = inputs.get(parameter.getName());
    if (selections != null)
    {
      final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
      parameterElement.appendChild(selectionsElement);

      if (selections.getClass().isArray())
      {
        final int length = Array.getLength(selections);
        for (int i = 0; i < length; i++)
        {
          final Object value = Array.get(selections, i);
          final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
          selectionElement.setAttribute("value", String.valueOf(value)); //$NON-NLS-1$
          selectionsElement.appendChild(selectionElement);
        }
      }
      else
      {
        final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
        selectionElement.setAttribute("value", String.valueOf(selections)); //$NON-NLS-1$
        selectionsElement.appendChild(selectionElement);
      }
    }

    if (parameter instanceof ListParameter)
    {
      final ListParameter asListParam = (ListParameter) parameter;
      parameterElement.setAttribute("is-multi-select", "" + asListParam.isAllowMultiSelection()); //$NON-NLS-1$ //$NON-NLS-2$
      parameterElement.setAttribute("is-strict", "" + asListParam.isStrictValueCheck()); //$NON-NLS-1$ //$NON-NLS-2$

      final Element valuesElement = document.createElement("value-choices"); //$NON-NLS-1$
      parameterElement.appendChild(valuesElement);

      final ParameterValues possibleValues = asListParam.getValues(parameterContext);
      for (int i = 0; i < possibleValues.getRowCount(); i++)
      {
        final Object key = possibleValues.getKeyValue(i);
        final Object value = possibleValues.getTextValue(i);

        final Element valueElement = document.createElement("value-choice"); //$NON-NLS-1$
        valuesElement.appendChild(valueElement);

        // set
        if (key != null && value != null)
        {
          valueElement.setAttribute("label", String.valueOf(value)); //$NON-NLS-1$ //$NON-NLS-2$
          if (asListParam.isAllowMultiSelection() &&
              parameter.getValueType().isArray())
          {
            valueElement.setAttribute("value", convertParameterValueToString//$NON-NLS-1$
                (key, parameter.getValueType().getComponentType()));
            valueElement.setAttribute("type", parameter.getValueType().getComponentType().getName()); //$NON-NLS-1$
          }
          else
          {
            valueElement.setAttribute("value", convertParameterValueToString(key, parameter.getValueType())); //$NON-NLS-1$ //$NON-NLS-2$
            valueElement.setAttribute("type", parameter.getValueType().getName()); //$NON-NLS-1$
          }
        }
      }
    }
    else if (parameter instanceof PlainParameter)
    {
      // apply defaults, this is the easy case
      parameterElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      parameterElement.setAttribute("is-strict", "false"); //$NON-NLS-1$ //$NON-NLS-2$
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
        throw new BeanException(Messages.getErrorString("ReportPlugin.errorNonDateParameterValue"));
      }
      final Date d = (Date) value;
      return SimpleReportingComponent.DATE_FORMAT.format(d);
    }
    if (Number.class.isAssignableFrom(type))
    {
      final ValueConverter numConverter = ConverterRegistry.getInstance().getValueConverter(BigDecimal.class);
      return numConverter.toAttributeValue(new BigDecimal(String.valueOf(value)));
    }
    return valueConverter.toAttributeValue(value);
  }

  private ISubscription getSubscription()
  {
    ISubscription subscription = null;
    final String subscriptionId = getRequestParameters().getStringParameter("subscription-id", null); //$NON-NLS-1$
    if (!StringUtils.isEmpty(subscriptionId))
    {
      final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
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

    final String subscriptionId = requestParams.getStringParameter("subscription-id", null); //$NON-NLS-1$
    if (!StringUtils.isEmpty(subscriptionId))
    {
      final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
      final ISubscription subscription = subscriptionRepository.getSubscription(subscriptionId, userSession);
      final ISubscribeContent content = subscription.getContent();

      final Map<String, Object> contentParameters = content.getParameters();
      final SimpleParameterSetter parameters = new SimpleParameterSetter();
      parameters.setParameters(contentParameters);

      // add solution,path,name
      final ActionInfo info = ActionInfo.parseActionString(content.getActionReference());
      parameters.setParameter("solution", info.getSolutionName()); //$NON-NLS-1$
      parameters.setParameter("path", info.getPath()); //$NON-NLS-1$
      parameters.setParameter("name", info.getActionName()); //$NON-NLS-1$

      SubscriptionHelper.getSubscriptionParameters(subscriptionId, parameters, userSession);

      // add all parameters that were on the url, if any, they will override subscription (editing)
      final Iterator requestParamIterator = requestParams.getParameterNames();
      while (requestParamIterator.hasNext())
      {
        final String param = (String) requestParamIterator.next();
        parameters.setParameter(param, requestParams.getParameter(param));
      }

      requestParams = parameters;
    }
    return requestParams;
  }

  private String saveSubscription(final IParameterProvider parameterProvider,
                                  final ParameterDefinitionEntry parameterDefinitions[],
                                  final String actionReference,
                                  final IPentahoSession userSession)
  {

    if ((userSession == null) || (userSession.getName() == null))
    {
      return Messages.getString("SubscriptionHelper.USER_LOGIN_NEEDED"); //$NON-NLS-1$
    }

    final String subscriptionName = (String) parameterProvider.getParameter("subscription-name"); //$NON-NLS-1$

    final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);

    ISubscription subscription = getSubscription();
    if (subscription == null)
    {
      final boolean isUniqueName = subscriptionRepository.checkUniqueSubscriptionName(subscriptionName, userSession.getName(), actionReference);
      if (!isUniqueName)
      {
        return Messages.getString("SubscriptionHelper.USER_SUBSCRIPTION_NAME_ALREADY_EXISTS", subscriptionName); //$NON-NLS-1$
      }
    }

    final ISubscribeContent content = subscriptionRepository.getContentByActionReference(actionReference);
    if (content == null)
    {
      return (Messages.getString("SubscriptionHelper.ACTION_SEQUENCE_NOT_ALLOWED", parameterProvider.getStringParameter("name", ""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    final HashMap<String, Object> parameters = new HashMap<String, Object>();

    for (final ParameterDefinitionEntry parameter : parameterDefinitions)
    {
      final String parameterName = parameter.getName();
      final Object parameterValue = parameterProvider.getParameter(parameterName);
      if (parameterValue != null)
      {
        parameters.put(parameterName, parameterValue);
      }
    }
    parameters.put(SimpleReportingComponent.OUTPUT_TYPE, parameterProvider.getParameter(SimpleReportingComponent.OUTPUT_TYPE));

    final String destination = (String) parameterProvider.getParameter("destination"); //$NON-NLS-1$
    if (subscription == null)
    {
      // create a new subscription
      final String subscriptionId = UUIDUtil.getUUIDAsString();
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
    final List schedules = subscriptionRepository.getSchedules();
    for (int i = 0; i < schedules.size(); i++)
    {
      final ISchedule schedule = (ISchedule) schedules.get(i);
      final String scheduleId = schedule.getId();
      final String scheduleIdParam = (String) parameterProvider.getParameter("schedule-id"); //$NON-NLS-1$
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

  private void addSubscriptionParameter(final String reportDefinitionPath,
                                        final Element parameters,
                                        final Map<String, Object> inputs)
  {
    final ISubscription subscription = getSubscription();

    final Document document = parameters.getOwnerDocument();

    final Element reportNameParameter = document.createElement("parameter"); //$NON-NLS-1$
    parameters.appendChild(reportNameParameter);
    reportNameParameter.setAttribute("name", "subscription-name"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportName")); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("parameter-group", "subscription"); //$NON-NLS-1$ //$NON-NLS-2$
    reportNameParameter.setAttribute("parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportSchedulingOptions")); //$NON-NLS-1$ //$NON-NLS-2$
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
      final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
      reportNameParameter.appendChild(selectionsElement);
      final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
      selectionElement.setAttribute("value", reportNameSelection.toString()); //$NON-NLS-1$
      selectionsElement.appendChild(selectionElement);
    }

    final String email = PentahoSystem.getSystemSetting("smtp-email/email_config.xml", "mail.userid", "");
    if (StringUtils.isEmpty(email) == false)
    {

      // create email destination parameter
      final Element emailParameter = document.createElement("parameter"); //$NON-NLS-1$
      parameters.appendChild(emailParameter);
      emailParameter.setAttribute("name", "destination"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.Destination")); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("parameter-group", "subscription"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportSchedulingOptions")); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-mandatory", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("is-strict", "false"); //$NON-NLS-1$ //$NON-NLS-2$
      emailParameter.setAttribute("parameter-render-type", "textbox"); //$NON-NLS-1$ //$NON-NLS-2$

      Object destinationSelection = inputs.get("destination");
      if (destinationSelection == null && subscription != null)
      {
        destinationSelection = subscription.getTitle();
      }
      if (destinationSelection != null)
      {
        final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
        emailParameter.appendChild(selectionsElement);
        final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
        selectionElement.setAttribute("value", destinationSelection.toString()); //$NON-NLS-1$
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
    subscriptionIdElement.setAttribute("parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ScheduleReport")); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-mandatory", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("is-strict", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    subscriptionIdElement.setAttribute("parameter-render-type", "dropdown"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element valuesElement = document.createElement("value-choices"); //$NON-NLS-1$
    subscriptionIdElement.appendChild(valuesElement);

    for (final ISchedule schedule : subscribeContent.getSchedules())
    {
      final Element valueElement = document.createElement("value-choice"); //$NON-NLS-1$
      valuesElement.appendChild(valueElement);
      valueElement.setAttribute("label", schedule.getTitle()); //$NON-NLS-1$
      valueElement.setAttribute("value", schedule.getId()); //$NON-NLS-1$
      valueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    // selections (schedules)
    final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
    subscriptionIdElement.appendChild(selectionsElement);

    final Object scheduleIdSelection = inputs.get("schedule-id"); //$NON-NLS-1$
    if (scheduleIdSelection != null)
    {
      final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
      selectionElement.setAttribute("value", scheduleIdSelection.toString()); //$NON-NLS-1$
      selectionsElement.appendChild(selectionElement);
    }

    // if the user hasn't picked a schedule (to change this subscription to), and we
    // have a subscription active, get the schedules on it and add those
    if (scheduleIdSelection == null)
    {
      if (subscription != null)
      {
        final List<ISchedule> schedules = subscription.getSchedules();
        for (final ISchedule schedule : schedules)
        {
          final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
          selectionElement.setAttribute("value", schedule.getId()); //$NON-NLS-1$
          selectionsElement.appendChild(selectionElement);
        }
      }
    }
  }

  private void addOutputParameter(final MasterReport report,
                                  final Element parameters,
                                  final Map<String, Object> inputs,
                                  final boolean subscribe)
  {
    final Object lockOutputTypeObj = report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE);
    if (Boolean.TRUE.equals(lockOutputTypeObj)) //$NON-NLS-1$
    {
      // if the output type is locked, do not allow prompt rendering
      return;
    }

    final Document document = parameters.getOwnerDocument();
    final Element parameterOutputElement = document.createElement("parameter"); //$NON-NLS-1$
    parameters.appendChild(parameterOutputElement);
    parameterOutputElement.setAttribute("name", SimpleReportingComponent.OUTPUT_TYPE); //$NON-NLS-1$
    parameterOutputElement.setAttribute("label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.OutputType")); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("parameter-group", "parameters"); //$NON-NLS-1$ //$NON-NLS-2$
    if (subscribe)
    {
      parameterOutputElement.setAttribute("parameter-group-label", org.pentaho.reporting.platform.plugin.messages.Messages.getString("ReportPlugin.ReportParameters")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    parameterOutputElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-mandatory", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-multi-select", "false"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("is-strict", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    parameterOutputElement.setAttribute("parameter-render-type", "dropdown"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element valuesElement = document.createElement("value-choices"); //$NON-NLS-1$
    parameterOutputElement.appendChild(valuesElement);

    final Element htmlValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(htmlValueElement);
    htmlValueElement.setAttribute("label", "HTML"); //$NON-NLS-1$ //$NON-NLS-2$
    htmlValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_HTML); //$NON-NLS-1$
    htmlValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element pdfValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(pdfValueElement);
    pdfValueElement.setAttribute("label", "PDF"); //$NON-NLS-1$ //$NON-NLS-2$
    pdfValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_PDF); //$NON-NLS-1$
    pdfValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element xlsValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(xlsValueElement);
    xlsValueElement.setAttribute("label", "Excel (XLS)"); //$NON-NLS-1$ //$NON-NLS-2$
    xlsValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_XLS); //$NON-NLS-1$
    xlsValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element csvValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(csvValueElement);
    csvValueElement.setAttribute("label", "CSV"); //$NON-NLS-1$ //$NON-NLS-2$
    csvValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_CSV); //$NON-NLS-1$
    csvValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element rtfValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(rtfValueElement);
    rtfValueElement.setAttribute("label", "RTF"); //$NON-NLS-1$ //$NON-NLS-2$
    rtfValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_RTF); //$NON-NLS-1$
    rtfValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Element txtValueElement = document.createElement("value-choice"); //$NON-NLS-1$
    valuesElement.appendChild(txtValueElement);
    txtValueElement.setAttribute("label", "Plain Text"); //$NON-NLS-1$ //$NON-NLS-2$
    txtValueElement.setAttribute("value", SimpleReportingComponent.MIME_TYPE_TXT); //$NON-NLS-1$
    txtValueElement.setAttribute("type", "java.lang.String"); //$NON-NLS-1$ //$NON-NLS-2$

    final Object selections = inputs.get(SimpleReportingComponent.OUTPUT_TYPE);
    if (selections != null)
    {
      final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
      parameterOutputElement.appendChild(selectionsElement);
      final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
      selectionElement.setAttribute("value", selections.toString()); //$NON-NLS-1$
      selectionsElement.appendChild(selectionElement);
    }
    else
    {
      // use default, if available, from the report
      final String preferredOutputType = (String) report.getAttribute(AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE);
      if (!StringUtils.isEmpty(preferredOutputType))
      {
        final Element selectionsElement = document.createElement("selections"); //$NON-NLS-1$
        parameterOutputElement.appendChild(selectionsElement);
        final Element selectionElement = document.createElement("selection"); //$NON-NLS-1$
        selectionElement.setAttribute("value", MimeHelper.getMimeTypeFromExtension("." + preferredOutputType)); //$NON-NLS-1$ //$NON-NLS-2$
        selectionsElement.appendChild(selectionElement);
      }
    }
  }

  public String generateWrapperXaction()
  {
    final IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);

    final String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
    final String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
    final String name = requestParams.getStringParameter("action", null); //$NON-NLS-1$

    // sanitization
    final String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);
    // final ActionInfo actionInfo = ActionInfo.parseActionString(reportDefinitionPath);

    final ActionSequenceDocument actionSequenceDocument = new ActionSequenceDocument();
    actionSequenceDocument.setTitle(reportDefinitionPath);
    actionSequenceDocument.setVersion("1"); //$NON-NLS-1$
    actionSequenceDocument.setAuthor("SolutionEngine"); //$NON-NLS-1$
    actionSequenceDocument.setDescription(reportDefinitionPath);
    actionSequenceDocument.setIconLocation("PentahoReporting.png"); //$NON-NLS-1$
    actionSequenceDocument.setHelp(""); //$NON-NLS-1$
    actionSequenceDocument.setResultType("report"); //$NON-NLS-1$
    final IActionSequenceInput outputType = actionSequenceDocument.createInput("outputType", ActionSequenceDocument.STRING_TYPE); //$NON-NLS-1$
    outputType.setDefaultValue("text/html"); //$NON-NLS-1$
    final IActionSequenceOutput output = actionSequenceDocument.createOutput("outputstream", "content"); //$NON-NLS-1$ //$NON-NLS-2$
    output.addDestination("response", "content"); //$NON-NLS-1$ //$NON-NLS-2$

    try
    {
      // URI reportURI = new URI("solution:/" + actionInfo.getPath() + "/" + actionInfo.getActionName());
      // actionSequenceDocument.setResourceUri("reportDefinition", reportURI, "application/zip");
      final IActionSequenceInput reportDefinitionPathInput = actionSequenceDocument.createInput("report-definition-path", ActionSequenceDocument.STRING_TYPE); //$NON-NLS-1$
      reportDefinitionPathInput.setDefaultValue(reportDefinitionPath);

      final IActionDefinition pojoComponent = actionSequenceDocument.addAction(PojoAction.class);
      pojoComponent.setComponentDefinition("class", SimpleReportingComponent.class.getName()); //$NON-NLS-1$
      pojoComponent.addOutput("outputstream", "content"); //$NON-NLS-1$ //$NON-NLS-2$
      pojoComponent.addInput("report-definition-path", "string"); //$NON-NLS-1$ //$NON-NLS-2$

      // add all prpt inputs
      if (reportComponent == null)
      {
        reportComponent = new SimpleReportingComponent();
      }
      reportComponent.setSession(userSession);
      reportComponent.setReportDefinitionPath(reportDefinitionPath);
      reportComponent.setInputs(createInputs(requestParams));
      final MasterReport report = reportComponent.getReport();
      final ParameterDefinitionEntry[] parameterDefinitions = report.getParameterDefinition().getParameterDefinitions();
      final ParameterContext parameterContext = new DefaultParameterContext(report);
      for (final ParameterDefinitionEntry parameter : parameterDefinitions)
      {
        final Object defaultValue = parameter.getDefaultValue(parameterContext);
        if (defaultValue != null)
        {
          final IActionSequenceInput input = actionSequenceDocument.createInput(parameter.getName(), ActionSequenceDocument.STRING_TYPE);
          input.setDefaultValue(convertParameterValueToString(defaultValue, parameter.getValueType()));
        }
        else
        {
          final Object paramValue = requestParams.getParameter(parameter.getName());
          if (paramValue != null)
          {
            final IActionSequenceInput input =
                actionSequenceDocument.createInput(parameter.getName(), ActionSequenceDocument.STRING_TYPE);
            input.setDefaultValue(convertParameterValueToString(paramValue, parameter.getValueType()));
          }
        }
        pojoComponent.addInput(parameter.getName(), "string"); //$NON-NLS-1$
      }
      pojoComponent.addInput("outputType", "string"); //$NON-NLS-1$ //$NON-NLS-2$

    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }

    return actionSequenceDocument.toString();
  }

  private Map<String, Object> createInputs(final IParameterProvider requestParams)
  {
    final Map<String, Object> inputs = new HashMap<String, Object>();
    final Iterator paramIter = requestParams.getParameterNames();
    while (paramIter.hasNext())
    {
      final String paramName = (String) paramIter.next();
      final Object paramValue = requestParams.getParameter(paramName);
      inputs.put(paramName, paramValue);
    }
    return inputs;
  }

  public Log getLogger()
  {
    return log;
  }

  public String getMimeType()
  {
    final IParameterProvider requestParams = getRequestParameters();
    final RENDER_TYPE renderMode = RENDER_TYPE.valueOf(requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    if (renderMode.equals(RENDER_TYPE.XML))
    {
      return "text/xml"; //$NON-NLS-1$
    }
    else if (renderMode.equals(RENDER_TYPE.SUBSCRIBE))
    {
      return SimpleReportingComponent.MIME_TYPE_HTML;
    }
    else if (renderMode.equals(RENDER_TYPE.DOWNLOAD))
    {
      // perhaps we can invent our own mime-type or use application/zip?
      return "application/octet-stream"; //$NON-NLS-1$
    }

    final String solution = requestParams.getStringParameter("solution", null); //$NON-NLS-1$
    final String path = requestParams.getStringParameter("path", null); //$NON-NLS-1$
    final String name = requestParams.getStringParameter("name", requestParams.getStringParameter("action", null)); //$NON-NLS-1$ //$NON-NLS-2$
    final String reportDefinitionPath = ActionInfo.buildSolutionPath(solution, path, name);

    if (reportComponent == null)
    {
      reportComponent = new SimpleReportingComponent();
    }

    final Map<String, Object> inputs = createInputs(requestParams);
    reportComponent.setSession(userSession);
    reportComponent.setReportDefinitionPath(reportDefinitionPath);
    reportComponent.setInputs(inputs);
    return reportComponent.getMimeType();
  }
}
