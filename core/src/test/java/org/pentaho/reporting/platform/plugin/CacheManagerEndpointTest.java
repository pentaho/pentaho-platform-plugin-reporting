/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


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
