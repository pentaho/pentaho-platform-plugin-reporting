package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PentahoAsyncExecutor implements ILogoutListener, IPentahoSystemListener, IPentahoAsyncExecutor {

  public static final String BEAN_NAME = "IPentahoAsyncExecutor";

  private static final Log log = LogFactory.getLog( PentahoAsyncExecutor.class );

  private Map<CompositeKey, Future<InputStream>> tasks = new ConcurrentHashMap<>();
  private Map<CompositeKey, AsyncReportStatusListener> listeners = new ConcurrentHashMap<>();

  private ExecutorService executorService;

  /**
   * @param capacity
   */
  //package private visibility for testing purposes
  PentahoAsyncExecutor( final int capacity ) {
    log.info( "Initialized reporting  async execution fixed thread pool with capacity: " + capacity );
    executorService =
      new PentahoAsyncCancellingExecutor( capacity, capacity, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>() );
    PentahoSystem.addLogoutListener( this );
  }

  // default visibility for testing purpose
  static class CompositeKey {

    private String sessionId;
    private String uuid;

    CompositeKey( final IPentahoSession session, final UUID id ) {
      this.uuid = id.toString();
      this.sessionId = session.getId();
    }

    private String getSessionId() {
      return sessionId;
    }

    @Override public boolean equals( final Object o ) {
      if ( this == o ) {
        return true;
      }
      if ( o == null || getClass() != o.getClass() ) {
        return false;
      }
      final CompositeKey that = (CompositeKey) o;
      return Objects.equals( sessionId, that.sessionId ) && Objects.equals( uuid, that.uuid );
    }

    @Override public int hashCode() {
      return Objects.hash( sessionId, uuid );
    }
  }

  @Override public UUID addTask( final IAsyncReportExecution<InputStream> task, final IPentahoSession session ) {

    final UUID id = UUID.randomUUID();
    final CompositeKey key = new CompositeKey( session, id );

    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( task.getReportPath(), id, task.getMimeType() );
    task.setListener( listener );

    log.debug( "register async execution for task: " + task.toString() );

    final Future<InputStream> result = executorService.submit( task );
    tasks.put( key, result );
    listeners.put( key, listener );
    return id;
  }

  @Override public Future<InputStream> getFuture( final UUID id, final IPentahoSession session ) {
    if ( id == null ) {
      throw new NullPointerException( "uuid is null" );
    }
    if ( session == null ) {
      throw new NullPointerException( "Session is null" );
    }
    return tasks.get( new CompositeKey( session, id ) );
  }

  @Override public void cleanFuture( final UUID id, final IPentahoSession session ) {
    final CompositeKey key = new CompositeKey( session, id );
    tasks.remove( key );
    listeners.remove( key );
  }

  @Override public IAsyncReportState getReportState( final UUID id, final IPentahoSession session ) {
    if ( id == null ) {
      throw new NullPointerException( "uuid is null" );
    }
    if ( session == null ) {
      throw new NullPointerException( "session is null" );
    }
    // link to running task
    final AsyncReportStatusListener runningTask = listeners.get( new CompositeKey( session, id ) );

    return runningTask == null ? null : runningTask;
  }

  @Override public void onLogout( final IPentahoSession iPentahoSession ) {
    if ( log.isDebugEnabled() ) {
      // don't expose full session id.
      log.debug( "killing async report execution cache for user: " + iPentahoSession.getName() );
    }
    for ( final Map.Entry<CompositeKey, Future<InputStream>> entry : tasks.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), iPentahoSession.getId() ) ) {
        // attempt to cancel running task
        entry.getValue().cancel( true );

        // remove all links to release GC
        tasks.remove( entry.getKey() );
        listeners.remove( entry.getKey() );
      }
    }

    // do it generic way according to staging handler was used?
    Path stagingSessionDir = AsyncJobFileStagingHandler.getStagingDirPath();
    if ( stagingSessionDir == null ) {
      //never been initialized
      return;
    }
    stagingSessionDir = stagingSessionDir.resolve( iPentahoSession.getId() );
    final File sessionStagingContent = stagingSessionDir.toFile();

    // some lib can do it for me?
    try {
      FileUtils.deleteDirectory( sessionStagingContent );
    } catch ( final IOException e ) {
      log.debug( "Unable delete temp files on session logout." );
    }
  }

  @Override public boolean startup( final IPentahoSession iPentahoSession ) {
    // don't see any useful actions now
    // may be register some supervisor here later?
    return true;
  }

  @Override public void shutdown() {
    // attempt to stop all
    for ( final Map.Entry<CompositeKey, Future<InputStream>> entry : tasks.entrySet() ) {
      entry.getValue().cancel( true );
    }
    // forget all
    this.tasks.clear();
    this.listeners.clear();

    // delete all staging dir
    final Path stagingDir = AsyncJobFileStagingHandler.getStagingDirPath();
    final File stagingDirFile;
    if ( stagingDir != null ) {
      stagingDirFile = stagingDir.toFile();
      try {
        FileUtils.deleteDirectory( stagingDirFile );
      } catch ( final IOException e ) {
        log.debug( "Unable to delete async staging content on shutdown. Directory: " + stagingDirFile.getName() );
      }
    }

  }
}
