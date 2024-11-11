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

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;

public class ClearCacheAction implements Runnable {
  @Override
  public void run() {
    final IPluginCacheManager iPluginCacheManager = PentahoSystem.get( IPluginCacheManager.class );
    if ( iPluginCacheManager != null ) {
      final IReportContentCache cache = iPluginCacheManager.getCache();
      if ( cache != null ) {
        cache.cleanup();
      }
    }
  }
}
