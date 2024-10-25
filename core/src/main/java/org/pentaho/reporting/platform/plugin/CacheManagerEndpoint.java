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


import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheFactory;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path( "/reporting/api/cache" )
public class CacheManagerEndpoint {


  @POST @Path( "clear" )
  public Response clear() {
    try {
      final IPluginCacheManager iPluginCacheManager = PentahoSystem.get( IPluginCacheManager.class );
      final IReportContentCache cache = iPluginCacheManager.getCache();
      cache.cleanupCurrentSession();

      final ICacheManager cacheManager = PentahoSystem.get( ICacheManager.class );

      cacheManager.clearRegionCache( "report-output-handlers" );
      cacheManager.clearRegionCache( "report-dataset-cache" );

      DataCacheFactory.getCache().getCacheManager().clearAll();

      return Response.ok().build();
    } catch ( final Exception e ) {
      return Response.serverError().build();
    }
  }

}
