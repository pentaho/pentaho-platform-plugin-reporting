/*!
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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory;
import org.pentaho.platform.api.engine.IPluginProvider;
import org.pentaho.platform.api.engine.IServiceManager;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.engine.IUserRoleListService;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.security.userrole.ws.MockUserRoleListService;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.plugin.services.pluginmgr.SystemPathXmlPluginProvider;
import org.pentaho.platform.plugin.services.pluginmgr.servicemgr.DefaultServiceManager;
import org.pentaho.platform.repository2.unified.fs.FileSystemBackedUnifiedRepository;
import org.pentaho.reporting.platform.plugin.cache.NullReportCache;
import org.pentaho.reporting.platform.plugin.cache.ReportCache;
import org.pentaho.reporting.platform.plugin.output.DefaultReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.repository.PentahoNameGenerator;
import org.pentaho.reporting.platform.plugin.repository.TempDirectoryNameGenerator;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.springframework.security.core.userdetails.UserDetailsService;

public class MicroPlatformFactory
{
  public static MicroPlatform create()
  {
    MicroPlatform microPlatform = new MicroPlatform( "target/test/resource/solution" ); //$NON-NLS-1$
    microPlatform.define( ISolutionEngine.class, SolutionEngine.class );
    microPlatform.define( IUnifiedRepository.class, FileSystemBackedUnifiedRepository.class );
    microPlatform.define( IPluginProvider.class, SystemPathXmlPluginProvider.class );
    microPlatform.define( IServiceManager.class, DefaultServiceManager.class, IPentahoDefinableObjectFactory.Scope.GLOBAL );
    microPlatform.define( PentahoNameGenerator.class, TempDirectoryNameGenerator.class, IPentahoDefinableObjectFactory.Scope.GLOBAL );
    microPlatform.define( IUserRoleListService.class, MockUserRoleListService.class );
    microPlatform.define( UserDetailsService.class, MockUserDetailsService.class );
    microPlatform.define( ReportOutputHandlerFactory.class, DefaultReportOutputHandlerFactory.class );
    microPlatform.define( ReportCache.class, NullReportCache.class );
    return microPlatform;
  }
}
