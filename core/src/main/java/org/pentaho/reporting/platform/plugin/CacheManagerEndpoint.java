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
 * Copyright 2006 - 2019 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;


import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheFactory;
import org.pentaho.reporting.platform.plugin.cache.IPluginCacheManager;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

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
