/*
 * This program is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software 
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this 
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html 
 * or from the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright 2008 Pentaho Corporation.  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.parser.base.ReportGenerator;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.xml.sax.InputSource;

public class ReportCreator
{
  public static MasterReport createReport(final InputStream inputStream,
                                          final URL url) throws IOException, ResourceException
  {
    final ReportGenerator generator = ReportGenerator.createInstance();
    final InputSource repDefInputSource = new InputSource(inputStream);
    return generator.parseReport(repDefInputSource, url);
  }

  /**
   * @deprecated Use the one without the session instead.
   * @param reportDefinitionPath
   * @param session
   * @return
   * @throws ResourceException
   * @throws IOException
   */
  public static MasterReport createReport(final String reportDefinitionPath,
                                          IPentahoSession session) throws ResourceException, IOException
  {
    return createReport(reportDefinitionPath);
  }

  public static MasterReport createReport(final String reportDefinitionPath) throws ResourceException, IOException
  {
    final ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();
    final HashMap helperObjects = new HashMap();
    // add the runtime context so that PentahoResourceData class can get access
    // to the solution repo
    final ResourceKey key = resourceManager.createKey(RepositoryResourceLoader.SOLUTION_SCHEMA_NAME + RepositoryResourceLoader.SCHEMA_SEPARATOR
        + reportDefinitionPath, helperObjects);
    final Resource resource = resourceManager.create(key, null, MasterReport.class);
    return (MasterReport) resource.getResource();
  }

}
