/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/
package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;

import static org.mockito.Mockito.*;

public class ClearCacheActionTest {


  @Test
  public void runNoBean() {
    new ClearCacheAction().run();
  }

  @Test
  public void run() {
    final IReportContentCache mock = mock( IReportContentCache.class );
    PentahoSystem.registerObject( new PluginCacheManagerImpl( mock ), IPluginCacheManager.class );
    new ClearCacheAction().run();
    verify( mock, times( 1 ) ).cleanup();
    PentahoSystem.shutdown();
  }

}
