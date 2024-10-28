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
 * Copyright 2006 - 2024 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.cache.DataCache;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheFactory;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;

import jakarta.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class CacheManagerEndpointTest {

  @Ignore
  @Test
  public void clear() throws Exception {
    try ( MockedStatic<DataCacheFactory> dataCacheFactoryMockedStatic = Mockito.mockStatic( DataCacheFactory.class ) ) {
      final DataCache dataCache = mock( DataCache.class );
      dataCacheFactoryMockedStatic.when( DataCacheFactory::getCache ).thenReturn( dataCache );
      final DataCacheManager dataCacheManager = mock( DataCacheManager.class );
      when( dataCache.getCacheManager() ).thenReturn( dataCacheManager );
      final IPluginCacheManager cacheManager = mock( IPluginCacheManager.class );
      final IReportContentCache contentCache = mock( IReportContentCache.class );
      when( cacheManager.getCache() ).thenReturn( contentCache );
      PentahoSystem.registerObject( cacheManager, IPluginCacheManager.class );
      final ICacheManager iCacheManager = mock( ICacheManager.class );
      PentahoSystem.registerObject( iCacheManager, ICacheManager.class );
      final Response clear = new CacheManagerEndpoint().clear();

      assertEquals( 200, clear.getStatus() );
      verify( contentCache, times( 1 ) ).cleanupCurrentSession();
      verify( iCacheManager, times( 1 ) ).clearRegionCache( "report-output-handlers" );
      verify( dataCacheManager, times( 1 ) ).clearAll();
    } finally {
      PentahoSystem.shutdown();
    }
  }

  @Ignore
  @Test
  public void clearError() throws Exception {
    final Response clear = new CacheManagerEndpoint().clear();
    assertEquals( 500, clear.getStatus() );
  }

}
