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
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;


public class ParameterContentGenerator extends SimpleContentGenerator {
  /**
   *
   */
  private static final long serialVersionUID = 1L;
  private String path = null;
  private IParameterProvider requestParameters;

  @Override
  public void createContent(OutputStream outputStream) throws Exception {
    IUnifiedRepository unifiedRepository = PentahoSystem.get(IUnifiedRepository.class, null);
    final IParameterProvider requestParams = getRequestParameters();
    final IParameterProvider pathParams = getPathParameters();

    if (requestParams != null && requestParams.getStringParameter("path", null) != null) {
      path = URLDecoder.decode(requestParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    } else if (pathParams != null && pathParams.getStringParameter("path", null) != null) {
      path = URLDecoder.decode(pathParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    RepositoryFile prptFile = unifiedRepository.getFile(idTopath(path));
    final ParameterXmlContentHandler parameterXmlContentHandler = new ParameterXmlContentHandler(this);
    parameterXmlContentHandler.createParameterContent(outputStream, prptFile.getId());
  }

  @Override
  public String getMimeType() {
    return "text/xml";
  }

  @Override
  public Log getLogger() {
    return LogFactory.getLog(ParameterContentGenerator.class);
  }

  protected String idTopath(String id) {
    String path = id.replace(":", "/");
    if (path != null && path.length() > 0 && path.charAt(0) != '/') {
      path = "/" + path;
    }
    return path;
  }

  /**
   * Safely get our request parameters, while respecting any parameters hooked up to a subscription
   *
   * @return IParameterProvider the provider of parameters
   */
  public IParameterProvider getRequestParameters() {
    if (requestParameters != null) {
      return requestParameters;
    }

    if (parameterProviders == null) {
      return new SimpleParameterProvider();
    }

    IParameterProvider requestParams = parameterProviders.get(IParameterProvider.SCOPE_REQUEST);

    requestParameters = requestParams;
    return requestParams;
  }

  private IParameterProvider pathParameters;

  public IParameterProvider getPathParameters() {
    if (pathParameters != null) {
      return pathParameters;
    }

    IParameterProvider pathParams = parameterProviders.get("path");

    pathParameters = pathParams;
    return pathParams;
  }

  public Map<String, Object> createInputs() {
    return createInputs(getRequestParameters());
  }

  protected static Map<String, Object> createInputs(final IParameterProvider requestParams) {
    final Map<String, Object> inputs = new HashMap<String, Object>();
    if (requestParams == null) {
      return inputs;
    }

    final Iterator paramIter = requestParams.getParameterNames();
    while (paramIter.hasNext()) {
      final String paramName = (String) paramIter.next();
      final Object paramValue = requestParams.getParameter(paramName);
      if (paramValue == null) {
        continue;
      }
      // only actually add inputs who don't have NULL values
      inputs.put(paramName, paramValue);
    }
    return inputs;
  }

  public IPentahoSession getUserSession() {
    return userSession;
  }
}
