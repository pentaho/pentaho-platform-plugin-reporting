package org.pentaho.reporting.platform.plugin;

import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.ISubscribeContent;
import org.pentaho.platform.api.repository.ISubscription;
import org.pentaho.platform.api.repository.ISubscriptionRepository;
import org.pentaho.platform.engine.core.solution.ActionInfo;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.platform.engine.services.solution.SimpleParameterSetter;
import org.pentaho.platform.repository.subscription.SubscriptionHelper;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.platform.plugin.gwt.client.ReportViewer.RENDER_TYPE;

public class ReportContentGenerator extends SimpleContentGenerator
{
  private static final Log log = LogFactory.getLog(ReportContentGenerator.class);

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

    try
    {
      switch (renderMode)
      {
        case DOWNLOAD:
        {
          final DownloadReportContentHandler contentHandler =
              new DownloadReportContentHandler(userSession, parameterProviders.get("path"));
          contentHandler.createDownloadContent(outputStream, reportDefinitionPath);
          break;
        }
        case REPORT:
        {
          // create inputs from request parameters
          final ExecuteReportContentHandler executeReportContentHandler = new ExecuteReportContentHandler(this);
          executeReportContentHandler.createReportContent(outputStream, reportDefinitionPath);
          break;
        }
        case SUBSCRIBE:
        {
          final SubscribeContentHandler subscribeContentHandler = new SubscribeContentHandler(this);
          subscribeContentHandler.createSubscribeContent(outputStream, reportDefinitionPath);
          break;
        }
        case XML:
        {
          // create inputs from request parameters
          final ParameterXmlContentHandler parameterXmlContentHandler = new ParameterXmlContentHandler(this, true);
          parameterXmlContentHandler.createParameterContent(outputStream, reportDefinitionPath);
          break;
        }
        case PARAMETER:
        {
          // create inputs from request parameters
          final ParameterXmlContentHandler parameterXmlContentHandler = new ParameterXmlContentHandler(this, false);
          parameterXmlContentHandler.createParameterContent(outputStream, reportDefinitionPath);
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
  }

  public String getInstanceId()
  {
    return instanceId;
  }

  public IPentahoSession getUserSession()
  {
    return userSession;
  }

  private IParameterProvider requestParameters;


  public Map<String, IParameterProvider> getParameterProviders()
  {
    return parameterProviders;
  }

  /**
   * Safely get our request parameters, while respecting any parameters hooked up to a subscription
   *
   * @return IParameterProvider the provider of parameters
   */
  public IParameterProvider getRequestParameters()
  {
    if (requestParameters != null)
    {
      return requestParameters;
    }

    if (parameterProviders == null)
    {
      return new SimpleParameterProvider();
    }

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
    requestParameters = requestParams;
    return requestParams;
  }

  public ISubscription getSubscription()
  {
    final String subscriptionId = getRequestParameters().getStringParameter("subscription-id", null); //$NON-NLS-1$
    if (StringUtils.isEmpty(subscriptionId))
    {
      return null;
    }

    final ISubscriptionRepository subscriptionRepository = PentahoSystem.get(ISubscriptionRepository.class, userSession);
    return subscriptionRepository.getSubscription(subscriptionId, userSession);
  }

  public Map<String, Object> createInputs()
  {
    return createInputs(getRequestParameters());
  }

  protected static Map<String, Object> createInputs(final IParameterProvider requestParams)
  {
    final Map<String, Object> inputs = new HashMap<String, Object>();
    if (requestParams == null)
    {
      return inputs;
    }

    final Iterator paramIter = requestParams.getParameterNames();
    while (paramIter.hasNext())
    {
      final String paramName = (String) paramIter.next();
      final Object paramValue = requestParams.getParameter(paramName);
      if (paramValue == null)
      {
        continue;
      }
      if ("".equals(paramValue))
      {
        continue;
      }
      // only actually add inputs who don't have NULL values
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
    final RENDER_TYPE renderMode = RENDER_TYPE.valueOf
        (requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    if (renderMode.equals(RENDER_TYPE.XML) ||
        renderMode.equals(RENDER_TYPE.PARAMETER))
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

    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    final Map<String, Object> inputs = createInputs(requestParams);
    reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
    reportComponent.setReportDefinitionPath(reportDefinitionPath);
    reportComponent.setInputs(inputs);
    return reportComponent.getMimeType();
  }
}
