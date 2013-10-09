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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.cache;

import java.util.Set;

import javax.swing.table.TableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.cache.CachableTableModel;
import org.pentaho.reporting.engine.classic.core.cache.DataCache;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheKey;
import org.pentaho.reporting.engine.classic.core.cache.DataCacheManager;

/**
 * A simple data cache that wraps around the plain in-memory data-cache. That cache is stored on the user's session and
 * shared across all reports run by that user in that session.
 * 
 * @author Thomas Morgner.
 */
public class PentahoDataCache implements DataCache, ILogoutListener {

  private static final Log log = LogFactory.getLog( PentahoDataCache.class );

  private static final String CACHE_NAME = "report-dataset-cache";

  /**
   * this as a public class so that if necessary someone can get access to a session key and clear the cache in their
   * own way via javascript rule / etc
   */
  public static class CompositeKey {
    public String sessionId;
    public DataCacheKey dataCacheKey;

    public CompositeKey( String sessionId, DataCacheKey dataCacheKey ) {
      this.sessionId = sessionId;
      this.dataCacheKey = dataCacheKey;
    }

    public boolean equals( Object o ) {
      if ( this == o ) {
        return true;
      }
      if ( o == null || getClass() != o.getClass() ) {
        return false;
      }
      final CompositeKey that = (CompositeKey) o;

      if ( !sessionId.equals( that.sessionId ) ) {
        return false;
      }
      if ( !dataCacheKey.equals( that.dataCacheKey ) ) {
        return false;
      }

      return true;
    }

    public int hashCode() {
      int result = sessionId.hashCode();
      result = 31 * result + dataCacheKey.hashCode();
      return result;
    }
  }

  private static class PentahoDataCacheManager implements DataCacheManager {
    private ICacheManager cacheManager;

    private PentahoDataCacheManager() {
      cacheManager = PentahoSystem.getCacheManager( null ); // cache manager gets loaded just once...
    }

    public void clearAll() {
      if ( cacheManager != null ) {
        cacheManager.clearRegionCache( CACHE_NAME );
      }
    }

    public void shutdown() {
      if ( cacheManager != null ) {
        cacheManager.removeRegionCache( CACHE_NAME );
      }
    }

    @SuppressWarnings( "unchecked" )
    public void killSessionCache( IPentahoSession session ) {
      if ( cacheManager != null ) {
        try {
          Set cachedObjects = cacheManager.getAllKeysFromRegionCache( CACHE_NAME );
          if ( cachedObjects != null ) {
            for ( Object k : cachedObjects ) {
              if ( k instanceof CompositeKey ) {
                CompositeKey key = (CompositeKey) k;
                if ( key.sessionId.equals( session.getId() ) ) {
                  cacheManager.removeFromRegionCache( CACHE_NAME, key );
                }
              }
            }
          }
        } catch ( Throwable e ) {
          // due to a known issue in hibernate cache
          // the getAll* methods of ICacheManager throw a NullPointerException if
          // cache values are null (this can happen due to cache object timeouts)
          // please see: http://opensource.atlassian.com/projects/hibernate/browse/HHH-3248
          if ( log.isDebugEnabled() ) {
            log.debug( "", e );
          }
        }
      }
    }
  }

  private PentahoDataCacheManager manager;
  private ICacheManager cacheManager;
  private int maximumRows;

  public PentahoDataCache() {
    if ( log.isDebugEnabled() ) {
      log.debug( "Initializing" );
    }
    maximumRows =
        ClassicEngineBoot.getInstance().getExtendedConfig().getIntProperty(
            "org.pentaho.reporting.platform.plugin.cache.PentahoDataCache.CachableRowLimit" );

    if ( log.isDebugEnabled() ) {
      log.debug( "Maximum Rows: " + maximumRows );
    }

    cacheManager = PentahoSystem.getCacheManager( null ); // cache manager gets loaded just once...
    manager = new PentahoDataCacheManager();
    if ( cacheManager != null ) {
      if ( !cacheManager.cacheEnabled( CACHE_NAME ) ) {
        if ( !cacheManager.addCacheRegion( CACHE_NAME ) ) {
          cacheManager = null;
          manager.cacheManager = null;
          throw new IllegalStateException( "PentahoDataCache (" + CACHE_NAME + ") cannot be initialized" );
        }
      }
    }

    PentahoSystem.addLogoutListener( this ); // So you can remove a users' region when their session disappears
  }

  public synchronized TableModel get( final DataCacheKey key ) {
    if ( cacheManager == null ) {
      return null;
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();

    if ( log.isDebugEnabled() ) {
      log.debug( "looking up key for session " + session.getId() );
    }

    return (TableModel) cacheManager.getFromRegionCache( CACHE_NAME, new CompositeKey( session.getId(), key ) );
  }

  public synchronized TableModel put( final DataCacheKey key, final TableModel model ) {
    if ( log.isDebugEnabled() ) {
      log.debug( "put() called" );
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();
    if ( cacheManager != null ) {
      if ( model.getRowCount() > maximumRows ) {

        if ( log.isDebugEnabled() ) {
          log.debug( "too many rows (" + model.getRowCount() + " > " + maximumRows + ") not caching." );
        }
        return model;
      }

      // Only copy if safe to do so. Check for whitelist of good column types ..
      if ( CachableTableModel.isSafeToCache( model ) == false ) {
        if ( log.isDebugEnabled() ) {
          log.debug( "model is not safe to cache. not caching." );
        }
        return model;
      }

      if ( log.isDebugEnabled() ) {
        log.debug( "placing model in cache for session " + session.getId() + " (rows=" + model.getColumnCount() + ")" );
      }
      final TableModel cacheModel = new CachableTableModel( model );
      cacheManager.putInRegionCache( CACHE_NAME, new CompositeKey( session.getId(), key ), cacheModel );
    }
    return model;
  }

  public DataCacheManager getCacheManager() {
    return manager;
  }

  @Override
  public void onLogout( IPentahoSession session ) {

    if ( log.isDebugEnabled() ) {
      log.debug( "killing session cache for " + session.getId() );
    }
    manager.killSessionCache( session );

  }
}
