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
import java.util.HashMap;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

public class ReportCreator
{
  /**
   *
   * @param reportDefinitionPath
   * @param pentahoSession
   * @return
   * @throws ResourceException
   * @throws IOException
   * @deprecated the session is not used, so kill this method
   */
  public static MasterReport createReport(final String reportDefinitionPath,
                                          IPentahoSession pentahoSession) throws ResourceException, IOException
  {
    return createReport(reportDefinitionPath);
  }
  
  public static MasterReport createReport(final String reportDefinitionPath) throws ResourceException, IOException
  {
    ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();
    HashMap helperObjects = new HashMap();
    // add the runtime context so that PentahoResourceData class can get access
    // to the solution repo
    ResourceKey key = resourceManager.createKey(RepositoryResourceLoader.SOLUTION_SCHEMA_NAME + RepositoryResourceLoader.SCHEMA_SEPARATOR
        + reportDefinitionPath, helperObjects);
    Resource resource = resourceManager.create(key, null, MasterReport.class);
    return (MasterReport) resource.getResource();
  }

}
