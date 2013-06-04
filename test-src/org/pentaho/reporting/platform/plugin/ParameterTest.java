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
 * Copyright 2010-2013 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import java.io.File;

import junit.framework.TestCase;
import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.api.engine.IServiceManager;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.security.userrole.ws.MockUserRoleListService;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.plugin.services.pluginmgr.SystemPathXmlPluginProvider;
import org.pentaho.platform.plugin.services.pluginmgr.servicemgr.DefaultServiceManager;
import org.pentaho.platform.repository2.unified.fs.FileSystemBackedUnifiedRepository;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.TempDirectoryNameGenerator;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.springframework.security.userdetails.UserDetailsService;

public class ParameterTest extends TestCase
{
  private MicroPlatform microPlatform;

  public ParameterTest()
  {

  }

  @Override
  protected void setUp() throws Exception {
    new File("./resource/solution/system/tmp").mkdirs();

    microPlatform = new MicroPlatform("./resource/solution"); //$NON-NLS-1$
    microPlatform.define(ISolutionEngine.class, SolutionEngine.class);
    microPlatform.define(IUnifiedRepository.class, FileSystemBackedUnifiedRepository.class);
    microPlatform.define(IPluginProvider.class, SystemPathXmlPluginProvider.class);
    microPlatform.define(IServiceManager.class, DefaultServiceManager.class, IPentahoDefinableObjectFactory.Scope.GLOBAL);
    microPlatform.define(PentahoNameGenerator.class, TempDirectoryNameGenerator.class, IPentahoDefinableObjectFactory.Scope.GLOBAL);
    microPlatform.define(IUserRoleListService.class, MockUserRoleListService.class);
    microPlatform.define(UserDetailsService.class, MockUserDetailsService.class);
    microPlatform.start();
    IPentahoSession session = new StandaloneSession("test user");
    PentahoSessionHolder.setSession(session);
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
  }

  public void testParameterProcessing() throws Exception
  {
    final ParameterContentGenerator contentGenerator = new ParameterContentGenerator();
    final ParameterXmlContentHandler handler = new ParameterXmlContentHandler(contentGenerator);
    handler.createParameterContent(System.out, "resource/solution/test/reporting/Product Sales.prpt", "resource/solution/test/reporting/Product Sales.prpt", false, null);
  }

}
