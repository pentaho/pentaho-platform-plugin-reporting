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


package org.pentaho.reporting.platform.plugin.cache;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.event.CacheEntryListener;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

/**
 * This cache stores the report-output-handler on a map inside the user's session.
 * 
 * @author Thomas Morgner.
 */
public class DefaultReportCache implements ReportCache {
  private static String SESSION_ATTRIBUTE = DefaultReportCache.class.getName() + "-Cache";
  private static String LISTENER_ADDED_ATTRIBUTE = DefaultReportCache.class.getName() + "-ListenerAdded";
  private static final String CACHE_NAME = "report-output-handlers";
  private static final Log logger = LogFactory.getLog( DefaultReportCache.class );

  private static class LogoutHandler implements ILogoutListener {
    private LogoutHandler() {
    }

    public void onLogout( final IPentahoSession session ) {
      logger.debug( "Shutting down session " + session.getId() );
      final Object attribute = session.getAttribute( SESSION_ATTRIBUTE );
      if ( attribute instanceof CacheManager == false ) {
        logger.debug( "Shutting down session " + session.getId() + ": No cache manager found" );
        return;
      }

      final CacheManager manager = (CacheManager) attribute;
      if ( manager.getCache( CACHE_NAME ) != null ) {
        final Cache<Object,Object> cache = manager.getCache( CACHE_NAME );
        // noinspection unchecked
        cache.removeAll();
      }

      logger.debug( "Shutting down session " + session.getId() + ": Closing cache manager." );
      manager.close();
    }
  }

  private static class CacheHolder {
    private ReportCacheKey realKey;
    private ReportOutputHandler outputHandler;
    private boolean closed;
    private boolean reportInUse;
    private boolean reportInCache;

    private CacheHolder( final ReportCacheKey realKey, final ReportOutputHandler outputHandler ) {
      if ( outputHandler == null ) {
        throw new NullPointerException();
      }
      if ( realKey == null ) {
        throw new NullPointerException();
      }
      this.reportInCache = true;
      this.realKey = realKey;
      this.outputHandler = outputHandler;
    }

    public synchronized void markEvicted() {
      reportInCache = false;
    }

    public boolean isReportInCache() {
      return reportInCache;
    }

    public ReportCacheKey getRealKey() {
      return realKey;
    }

    public ReportOutputHandler getOutputHandler() {
      return outputHandler;
    }

    public synchronized void close() {
      if ( reportInUse && reportInCache ) {
        return;
      }

      if ( closed == false ) {
        outputHandler.close();
        closed = true;
      }
    }

    public synchronized void setReportInUse( final boolean reportInUse ) {
      this.reportInUse = reportInUse;
    }
  }

  private static class CacheEvictionHandler implements CacheEntryListener {
    private CacheEvictionHandler() {
    }

    public void notifyElementRemoved( final Cache cache, final Object element ) throws CacheException {
      final Object o = element;
      if ( o instanceof CacheHolder == false ) {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.close();
    }

    public void notifyElementExpired( final Cache cache, final Object element ) {
      final Object o = element;
      if ( o instanceof CacheHolder == false ) {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      logger.debug( "Shutting down report on element-expired event " + cacheHolder.getRealKey().getSessionId() );
      cacheHolder.close();
    }
  }

  private static class CachedReportOutputHandler implements ReportOutputHandler {
    private CacheHolder parent;

    private CachedReportOutputHandler( final CacheHolder parent ) {
      this.parent = parent;
      this.parent.setReportInUse( true );
    }

    public int paginate( final MasterReport report, final int yieldRate ) throws ReportProcessingException,
      IOException, ContentIOException {
      return parent.getOutputHandler().paginate( report, yieldRate );
    }

    public int generate( final MasterReport report, final int acceptedPage, final OutputStream outputStream,
        final int yieldRate ) throws ReportProcessingException, IOException, ContentIOException {
      return parent.getOutputHandler().generate( report, acceptedPage, outputStream, yieldRate );
    }

    public boolean supportsPagination() {
      return parent.getOutputHandler().supportsPagination();
    }

    public void close() {
      this.parent.setReportInUse( false );
      if ( this.parent.isReportInCache() == false ) {
        this.parent.close();
      }
    }

    public Object getReportLock() {
      return parent.getOutputHandler().getReportLock();
    }
  }

  public DefaultReportCache() {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    synchronized ( session ) {
      if ( Boolean.TRUE.equals( session.getAttribute( LISTENER_ADDED_ATTRIBUTE ) ) ) {
        return;
      }
      PentahoSystem.addLogoutListener( new LogoutHandler() );
      session.setAttribute( LISTENER_ADDED_ATTRIBUTE, Boolean.TRUE );
    }
  }

  public ReportOutputHandler get( final ReportCacheKey key ) {
    if ( key.getSessionId() == null ) {
      return null;
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();
    logger.debug( "id: " + session.getId() + " - Cache.get(..) started" );
    synchronized ( session ) {
      final Object attribute = session.getAttribute( SESSION_ATTRIBUTE );
      if ( attribute instanceof CacheManager == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.get(..): No cache manager" );
        return null;
      }

      final CacheManager manager = (CacheManager) attribute;
      if ( manager.getCache( CACHE_NAME ) == null ) {
        logger.debug( "id: " + session.getId() + " - Cache.get(..): No cache registered" );
        return null;
      }

      final Cache cache = manager.getCache( CACHE_NAME );
      final Object element = cache.get( key.getSessionId() );
      if ( element == null ) {
        logger
            .debug( "id: " + session.getId() + " - Cache.get(..): No element in cache for key: " + key.getSessionId() );
        return null;
      }

      final Object o = element;
      if ( o instanceof CacheHolder == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.get(..): No valid element in cache for key: "
            + key.getSessionId() );
        return null;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      if ( cacheHolder.getRealKey().equals( key ) == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.get(..): remove stale report after parameter changed: "
            + key.getSessionId() );
        cache.remove( key.getSessionId() );
        return null;
      }
      logger.debug( "id: " + session.getId() + " - Cache.get(..): Returning cached instance for key: "
          + key.getSessionId() );
      return new CachedReportOutputHandler( cacheHolder );
    }
  }

  public ReportOutputHandler put( final ReportCacheKey key, final ReportOutputHandler report ) {
    if ( key.getSessionId() == null ) {
      return report;
    }

    if ( report instanceof CachedReportOutputHandler ) {
      throw new IllegalStateException();
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();
    logger.debug( "id: " + session.getId() + " - Cache.put(..) started" );
    synchronized ( session ) {
      final Object attribute = session.getAttribute( SESSION_ATTRIBUTE );
      final CacheManager manager;
      if ( attribute instanceof CacheManager == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.put(..): No cache manager; creating one" );
        CachingProvider cachingProvider = Caching.getCachingProvider();
          try {
              manager = cachingProvider.getCacheManager( getClass().getResource( "/ehcache.xml" ).toURI(), getClass().getClassLoader() );
          } catch ( URISyntaxException e) {
              throw new RuntimeException( e );
          }
          session.setAttribute( SESSION_ATTRIBUTE, manager );
      } else {
        manager = (CacheManager) attribute;
      }

      if ( manager.getCache( CACHE_NAME ) == null ) {
        logger.debug( "id: " + session.getId() + " - Cache.put(..): No cache registered with manager; creating one" );
        MutableConfiguration configuration = new MutableConfiguration().setStoreByValue( false ).addCacheEntryListenerConfiguration( new MutableCacheEntryListenerConfiguration( FactoryBuilder.factoryOf( CacheEvictionHandler.class ), null, false, true ) );
        manager.createCache( CACHE_NAME ,configuration );
      }

      final Cache cache = manager.getCache( CACHE_NAME );
      final Object element = cache.get( key.getSessionId() );
      if ( element != null ) {
        final Object o = element;
        if ( o instanceof CacheHolder ) {
          final CacheHolder cacheHolder = (CacheHolder) o;
          if ( cacheHolder.getRealKey().equals( key ) == false ) {
            logger.debug( "id: " + session.getId() + " - Cache.put(..): Removing stale object." );
            cache.remove( key.getSessionId() );
          } else {
            // otherwise: Keep the element in the cache and the next put will perform an "update" operation
            // on it. This will not close the report object.
            logger.debug( "id: " + session.getId()
                + " - Cache.put(..): keeping existing object, no cache operation done." );
            return new CachedReportOutputHandler( cacheHolder );
          }
        }
      }

      final CacheHolder cacheHolder = new CacheHolder( key, report );
      cache.put( key.getSessionId(), cacheHolder );
      logger.debug( "id: " + session.getId() + " - Cache.put(..): storing new report for key " + key.getSessionId() );
      return new CachedReportOutputHandler( cacheHolder );
    }
  }

}
