package org.pentaho.reporting.platform.plugin.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by dima.prokopenko@gmail.com on 2/2/2016.
 */
public class PentahoAsyncCache implements ILogoutListener {

  private static final Log log = LogFactory.getLog( PentahoAsyncCache.class );
  private static final String CACHE_NAME = "pentaho-async-task-cache";

  private boolean initialized = false;

  private Map<UUID, Future<?>> tasks = new ConcurrentHashMap<UUID, Future<?>>();

  private ICacheManager cacheManager;
  private PentahoAsyncCacheManager manager;
  private ExecutorService executorService;

  /**
   * Spring supports beans with private constructors
   *
   * @param capacity
   */
  private PentahoAsyncCache ( int capacity ) {
    log.info( "Initialized reporting  async execution fixed thread pool with capacity: " + capacity );
    executorService = Executors.newFixedThreadPool( capacity );
  }

  //TODO review!
  private static class CompositeKey {

    private String sessionId;
    private String uuid;

    CompositeKey( IPentahoSession session, UUID id ) {
      this.uuid = id.toString();
      this.sessionId = session.getId();
    }

    private String getSessionId() {
      return sessionId;
    }

    @Override
    public boolean equals( Object o ) {
      if ( this == o )
        return true;
      if ( o == null || getClass() != o.getClass() )
        return false;
      CompositeKey that = (CompositeKey) o;
      return Objects.equals( sessionId, that.sessionId ) && Objects.equals( uuid, that.uuid );
    }

    @Override
    public int hashCode() {
      return Objects.hash( sessionId, uuid );
    }
  }

  private static class PentahoAsyncCacheManager {
    private ICacheManager cacheManager;

    private PentahoAsyncCacheManager( ICacheManager cacheManager ) {
      //TODO get for session?
      this.cacheManager = cacheManager;
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

    public void killSessionCache( IPentahoSession session ) {
      if ( cacheManager != null ) {
        Set cachedObjects = cacheManager.getAllKeysFromRegionCache( CACHE_NAME );
        // wait for stream api available to rewrite this
        if ( cachedObjects != null ) {
          for ( Object k : cachedObjects ) {
            if ( k instanceof CompositeKey ) {
              CompositeKey key = CompositeKey.class.cast( k );
              if ( key.getSessionId().equals( session.getId() ) ) {
                //TODO get future, cancel running task, then remove from cache

                cacheManager.removeFromRegionCache( CACHE_NAME, key );
              }
            }
          }
        }
      }
    }
  }

  public PentahoAsyncCache() {
    if ( log.isDebugEnabled() ) {
      log.debug( "Initializing" );
    }
    //TODO correct cache? SimpleMapCacheManager?
    cacheManager = PentahoSystem.getCacheManager( null );
    manager = new PentahoAsyncCacheManager( cacheManager );

    if ( cacheManager != null ) {
      if ( !cacheManager.cacheEnabled( CACHE_NAME ) ) {
        if ( !cacheManager.addCacheRegion( CACHE_NAME ) ) {
          cacheManager = null;
          manager.cacheManager = null;
          throw new IllegalStateException( "PentahoAsyncCache (" + CACHE_NAME + ") cannot be initialized" );
        }
      }
    }

    PentahoSystem.addLogoutListener( this );
  }


  public UUID addTask( PentahoAsyncReportExecution task ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    UUID id = UUID.randomUUID();
    cacheManager.putInRegionCache( CACHE_NAME, new CompositeKey(session, id), task );

    //TODO log remove?
    log.debug("register async execution...");

    Future<InputStream> result = executorService.submit( task );



    return id;
  }

  public PentahoAsyncReportExecution getTask( UUID id ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    Object obj = cacheManager.getFromRegionCache( CACHE_NAME, new CompositeKey(session, id) );
    if (obj instanceof PentahoAsyncReportExecution){
      return PentahoAsyncReportExecution.class.cast( obj );
    } else {
      //TODO log
      log.debug( "not found execution for uuid: " + id.toString() );
    }
    return null;
  }

  // do we need this?
  public List<Future<InputStream>> getRunningTasks() {

    return null;
  }

  @Override public void onLogout( IPentahoSession iPentahoSession ) {
    if ( log.isDebugEnabled() ) {
      log.debug( "killing async report execution cache for " + iPentahoSession.getId() );
    }
    //TODO implement shutdown
  }
}
