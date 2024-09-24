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
 * Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
 */
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
