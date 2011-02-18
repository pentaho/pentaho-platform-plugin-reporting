package org.pentaho.reporting.platform.plugin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPluginManager;
import org.pentaho.platform.api.engine.IPluginResourceLoader;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.solution.SimpleContentGenerator;
import org.pentaho.platform.plugin.services.pluginmgr.PluginClassLoader;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: rmansoor
 * Date: 2/14/11
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ReportViewerDelegateContentGenerator extends SimpleContentGenerator {
    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ReportViewerDelegateContentGenerator.class);

    @Override
    public Log getLogger() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createContent(OutputStream outputStream) throws Exception {
        IPluginResourceLoader pluginResourceLoader = PentahoSystem.get(IPluginResourceLoader.class);
        IPluginManager pluginManager = PentahoSystem.get(IPluginManager.class);
        ClassLoader classLoader = pluginManager.getClassLoader("Pentaho Reporting Plugin");
        InputStream inputStream = pluginResourceLoader.getResourceAsStream(classLoader, "/reportviewer/report.html");
        int val;
        while ((val = inputStream.read()) != -1) {
            outputStream.write(val);
        }
        outputStream.flush();
    }

    @Override
    public String getMimeType() {
        return SimpleReportingComponent.MIME_TYPE_HTML;
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
