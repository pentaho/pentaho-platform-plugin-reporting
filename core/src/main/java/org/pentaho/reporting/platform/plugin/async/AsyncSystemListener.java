/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */


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
