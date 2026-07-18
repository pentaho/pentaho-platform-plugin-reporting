/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/




package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;

public class AsyncSystemListener implements IPentahoSystemListener {
  private IPentahoAsyncExecutor asyncExecutor;
  private IReportContentCache cache;

  @Override public boolean startup( final IPentahoSession iPentahoSession ) {
    asyncExecutor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    cache = PentahoSystem.get( IReportContentCache.class );
    return true;
  }

  @Override public void shutdown() {
    if ( null != asyncExecutor ) {
      asyncExecutor.shutdown();
    }
    if ( null != cache ) {
      cache.cleanup();
    }
  }
}
