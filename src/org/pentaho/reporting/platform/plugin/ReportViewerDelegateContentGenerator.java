package org.pentaho.reporting.platform.plugin;
/*
 * Copyright 2006 - 2008 Pentaho Corporation.  All rights reserved.
 * This software was developed by Pentaho Corporation and is provided under the terms
 * of the Mozilla Public License, Version 1.1, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to http://www.mozilla.org/MPL/MPL-1.1.txt. The Original Code is the Pentaho
 * BI Platform.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 *
 * Created Feb 14, 2011
 * @author rmansoor
 */
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
        ClassLoader classLoader = pluginManager.getClassLoader("reporting");
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
