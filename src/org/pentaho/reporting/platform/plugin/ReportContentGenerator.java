package org.pentaho.reporting.platform.plugin;

import java.io.File;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.solution.SimpleParameterProvider;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.platform.plugin.services.pluginmgr.PluginClassLoader;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;

public class ReportContentGenerator extends ParameterContentGenerator {
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public enum RENDER_TYPE
  {
    REPORT, XML, PARAMETER, DOWNLOAD
  }
  
  private static final Log log = LogFactory.getLog(ReportContentGenerator.class);

  public ReportContentGenerator() {
  }

  public void createContent(final OutputStream outputStream) throws Exception {
    final String id = UUIDUtil.getUUIDAsString();
    String path = null;
    RENDER_TYPE renderMode = null;
    setInstanceId(id);
    IUnifiedRepository unifiedRepository = PentahoSystem.get(IUnifiedRepository.class, null);
    final IParameterProvider requestParams = getRequestParameters();
    final IParameterProvider pathParams = getPathParameters();

    if (requestParams != null && requestParams.getStringParameter("path", null) != null) {
      path = URLDecoder.decode(requestParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    } else if (pathParams != null && pathParams.getStringParameter("path", null) != null) {
      path = URLDecoder.decode(pathParams.getStringParameter("path", ""), "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
    
    if (requestParams != null && requestParams.getStringParameter("renderMode", null) != null) {
      renderMode = RENDER_TYPE.valueOf
          (requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    } else if (pathParams != null && pathParams.getStringParameter("renderMode", null) != null) {
      renderMode = RENDER_TYPE.valueOf
          (pathParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    }

    // If render mode is not passed in the request or path parameter, then we will assume that the render type is REPORT
    if (renderMode == null) {
      renderMode = RENDER_TYPE.REPORT;
    }

    RepositoryFile prptFile = unifiedRepository.getFile(idTopath(path));


    try {
      switch (renderMode) {
        case DOWNLOAD: {
          final DownloadReportContentHandler contentHandler = new DownloadReportContentHandler(userSession,
              parameterProviders.get("path"));//$NON-NLS-1$
          contentHandler.createDownloadContent(outputStream, idTopath(prptFile.getPath()));
          break;
        }
        case REPORT: {
          // create inputs from request parameters
          final ExecuteReportContentHandler executeReportContentHandler = new ExecuteReportContentHandler(this);//$NON-NLS-1$
          executeReportContentHandler.createReportContent(outputStream, prptFile.getId(), false);
          break;
        }
        default:
          throw new IllegalArgumentException();
      }
    } catch (Exception ex) {
      final String exceptionMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
      log.error(exceptionMessage, ex);

      if (outputStream != null) {
        outputStream.write(exceptionMessage.getBytes("UTF-8")); //$NON-NLS-1$
        outputStream.flush();
      } else {
        throw new IllegalArgumentException();
      }
    }
  }

  public String getInstanceId() {
    return instanceId;
  }

  public Map<String, IParameterProvider> getParameterProviders() {
    return parameterProviders;
  }

  public Log getLogger() {
    return log;
  }

  public String getMimeType() {
    final IParameterProvider requestParams = getRequestParameters();
    final IParameterProvider pathParams = getPathParameters();
    RENDER_TYPE renderMode = null;
    String path = null;
    IUnifiedRepository unifiedRepository = PentahoSystem.get(IUnifiedRepository.class, null);
    if (requestParams != null && requestParams.getStringParameter("renderMode", null) != null) {
      renderMode = RENDER_TYPE.valueOf
          (requestParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    } else if (pathParams != null && pathParams.getStringParameter("renderMode", null) != null) {
      renderMode = RENDER_TYPE.valueOf
          (pathParams.getStringParameter("renderMode", RENDER_TYPE.REPORT.toString()).toUpperCase()); //$NON-NLS-1$
    }
    // If render mode is not passed in the request or path parameter, then we will assume that the render type is REPORT
    if (renderMode == null) {
      renderMode = RENDER_TYPE.REPORT;
    }
    if (renderMode.equals(RENDER_TYPE.XML) ||
        renderMode.equals(RENDER_TYPE.PARAMETER)) {
      return "text/xml"; //$NON-NLS-1$
    } else if (renderMode.equals(RENDER_TYPE.DOWNLOAD)) {
      // perhaps we can invent our own mime-type or use application/zip?
      return "application/octet-stream"; //$NON-NLS-1$
    }
    if (requestParams != null && requestParams.getStringParameter("path", null) != null) {
      path = requestParams.getStringParameter("path", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    } else if (pathParams != null && pathParams.getStringParameter("path", null) != null) {
      path = pathParams.getStringParameter("path", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    RepositoryFile prptFile = unifiedRepository.getFile(idTopath(path));
    final boolean isMobile = "true".equals(requestParams.getStringParameter("mobile", "false")); //$NON-NLS-1$ //$NON-NLS-2$

    final SimpleReportingComponent reportComponent = new SimpleReportingComponent();
    final Map<String, Object> inputs = createInputs(requestParams);
    reportComponent.setForceDefaultOutputTarget(isMobile);
    reportComponent.setDefaultOutputTarget(HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE);
    reportComponent.setReportFileId(prptFile.getId());
    reportComponent.setInputs(inputs);
    return reportComponent.getMimeType();
  }

  public String getSystemRelativePluginPath(ClassLoader classLoader) {
    File dir = getPluginDir(classLoader);
    if (dir == null) {
      return null;
    }
    // get the full path with \ converted to /
    String path = dir.getAbsolutePath().replace('\\', ISolutionRepository.SEPARATOR);
    int pos = path.lastIndexOf(ISolutionRepository.SEPARATOR + "system" + ISolutionRepository.SEPARATOR); //$NON-NLS-1$
    if (pos != -1) {
      path = path.substring(pos + 8);
    }
    return path;
  }

  protected File getPluginDir(ClassLoader classLoader) {
    if (classLoader instanceof PluginClassLoader) {
      return ((PluginClassLoader) classLoader).getPluginDir();
    }
    return null;
  }

}
