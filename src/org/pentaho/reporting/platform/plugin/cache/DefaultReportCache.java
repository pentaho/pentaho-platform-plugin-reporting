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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;
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
      if ( manager.cacheExists( CACHE_NAME ) ) {
        final Cache cache = manager.getCache( CACHE_NAME );
        // noinspection unchecked
        final List<Object> keys = new ArrayList<Object>( cache.getKeys() );
        logger.debug( "Shutting down session " + session.getId() + ": Closing " + keys.size() + " open reports." );
        for ( final Object key : keys ) {
          // remove also closes the cache-holder and thus the report (if the report is no longer in use).
          cache.remove( key );
        }
      }

      logger.debug( "Shutting down session " + session.getId() + ": Closing cache manager." );
      manager.shutdown();
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

  private static class CacheEvictionHandler implements CacheEventListener {
    private CacheEvictionHandler() {
    }

    public void notifyElementRemoved( final Ehcache ehcache, final Element element ) throws CacheException {
      final Object o = element.getObjectValue();
      if ( o instanceof CacheHolder == false ) {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.close();
    }

    public void notifyElementPut( final Ehcache ehcache, final Element element ) throws CacheException {

    }

    public void notifyElementUpdated( final Ehcache ehcache, final Element element ) throws CacheException {

    }

    public void notifyElementExpired( final Ehcache ehcache, final Element element ) {
      final Object o = element.getObjectValue();
      if ( o instanceof CacheHolder == false ) {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      logger.debug( "Shutting down report on element-expired event " + cacheHolder.getRealKey().getSessionId() );
      cacheHolder.close();
    }

    /**
     * This method is called when a element is automatically removed from the cache. We then clear it here.
     * 
     * @param ehcache
     * @param element
     */
    public void notifyElementEvicted( final Ehcache ehcache, final Element element ) {
      final Object o = element.getObjectValue();
      if ( o instanceof CacheHolder == false ) {
        return;
      }

      final CacheHolder cacheHolder = (CacheHolder) o;
      cacheHolder.markEvicted();
      logger.debug( "Shutting down report on element-evicted event " + cacheHolder.getRealKey().getSessionId() );
      cacheHolder.close();
    }

    public void notifyRemoveAll( final Ehcache ehcache ) {
      // could be that we are to late already here, the javadoc is not very clear on this one ..
      // noinspection unchecked
      final List keys = new ArrayList( ehcache.getKeys() );
      for ( final Object key : keys ) {
        final Element element = ehcache.get( key );
        final Object o = element.getValue();
        if ( o instanceof CacheHolder ) {
          final CacheHolder cacheHolder = (CacheHolder) o;
          cacheHolder.markEvicted();
          logger.debug( "Shutting down report on remove-all event " + cacheHolder.getRealKey().getSessionId() );
          cacheHolder.close();
        }
      }
    }

    public Object clone() throws CloneNotSupportedException {
      return super.clone();
    }

    public void dispose() {

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
      if ( manager.cacheExists( CACHE_NAME ) == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.get(..): No cache registered" );
        return null;
      }

      final Cache cache = manager.getCache( CACHE_NAME );
      final Element element = cache.get( key.getSessionId() );
      if ( element == null ) {
        logger
            .debug( "id: " + session.getId() + " - Cache.get(..): No element in cache for key: " + key.getSessionId() );
        return null;
      }

      final Object o = element.getObjectValue();
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
        manager = new CacheManager();
        session.setAttribute( SESSION_ATTRIBUTE, manager );
      } else {
        manager = (CacheManager) attribute;
      }

      if ( manager.cacheExists( CACHE_NAME ) == false ) {
        logger.debug( "id: " + session.getId() + " - Cache.put(..): No cache registered with manager; creating one" );
        manager.addCache( CACHE_NAME );
        final Cache cache = manager.getCache( CACHE_NAME );
        cache.getCacheEventNotificationService().registerListener( new CacheEvictionHandler() );
      }

      final Cache cache = manager.getCache( CACHE_NAME );
      final Element element = cache.get( key.getSessionId() );
      if ( element != null ) {
        final Object o = element.getObjectValue();
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
      cache.put( new Element( key.getSessionId(), cacheHolder ) );
      logger.debug( "id: " + session.getId() + " - Cache.put(..): storing new report for key " + key.getSessionId() );
      return new CachedReportOutputHandler( cacheHolder );
    }
  }

}
