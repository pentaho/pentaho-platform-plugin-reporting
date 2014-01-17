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
import org.springframework.security.userdetails.UserDetailsService;

public class MicroPlatformFactory
{
  public static MicroPlatform create()
  {
    MicroPlatform microPlatform = new MicroPlatform( "./resource/solution" ); //$NON-NLS-1$
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
