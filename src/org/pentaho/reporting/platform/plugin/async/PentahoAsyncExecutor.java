package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PentahoAsyncExecutor<TReportState extends IAsyncReportState>
        implements ILogoutListener, IPentahoSystemListener, IPentahoAsyncExecutor<TReportState> {

  public static final String BEAN_NAME = "IPentahoAsyncExecutor";

  private static final Log log = LogFactory.getLog( PentahoAsyncExecutor.class );

  private Map<CompositeKey, Future<IFixedSizeStreamingContent>> futures = new ConcurrentHashMap<>();
  private Map<CompositeKey, IAsyncReportExecution<TReportState>> tasks = new ConcurrentHashMap<>();

  private ExecutorService executorService;

  /**
   * @param capacity
   */
  public PentahoAsyncExecutor( final int capacity ) {
    log.info( "Initialized reporting  async execution fixed thread pool with capacity: " + capacity );
    executorService =
            new PentahoAsyncCancellingExecutor( capacity, capacity, 0L, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<Runnable>() );
    PentahoSystem.addLogoutListener( this );
  }

  // default visibility for testing purpose
  protected static class CompositeKey {

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

  @Override public UUID addTask( final IAsyncReportExecution<TReportState> task, final IPentahoSession session ) {

    final UUID id = UUID.randomUUID();
    final CompositeKey key = new CompositeKey( session, id );

    task.notifyTaskQueued( id );

    log.debug( "register async execution for task: " + task.toString() );

    final Future<IFixedSizeStreamingContent> result = executorService.submit( task );
    futures.put( key, result );
    tasks.put( key, task );
    return id;
  }

  @Override public Future<IFixedSizeStreamingContent> getFuture( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    return futures.get( new CompositeKey( session, id ) );
  }

  @Override public void cleanFuture( final UUID id, final IPentahoSession session ) {
    final CompositeKey key = new CompositeKey( session, id );
    futures.remove( key );
    tasks.remove( key );
  }

  @Override public void requestPage( final UUID id, final IPentahoSession session, final int page ) {
    validateParams( id, session );
    final IAsyncReportExecution<TReportState> runningTask = tasks.get( new CompositeKey( session, id ) );
    if ( runningTask != null ) {
      runningTask.requestPage( page );
    }
  }

  @Override public TReportState getReportState( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    // link to running task
    final IAsyncReportExecution<TReportState> runningTask = tasks.get( new CompositeKey( session, id ) );
    return runningTask == null ? null : runningTask.getState();
  }

  protected void validateParams( final UUID id, final IPentahoSession session ) {
    ArgumentNullException.validate( "uuid", id );
    ArgumentNullException.validate( "session", session );
  }

  @Override public void onLogout( final IPentahoSession session ) {
    if ( log.isDebugEnabled() ) {
      // don't expose full session id.
      log.debug( "killing async report execution cache for user: " + session.getName() );
    }
    for ( final Map.Entry<CompositeKey, Future<IFixedSizeStreamingContent>> entry : futures.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), session.getId() ) ) {
        // attempt to cancel running task
        entry.getValue().cancel( true );
        // remove all links to release GC
        futures.remove( entry.getKey() );
        tasks.remove( entry.getKey() );
      }
    }

    AsyncJobFileStagingHandler.cleanSession( session );
    // do it generic way according to staging handler was used?
    Path stagingSessionDir = AsyncJobFileStagingHandler.getStagingDirPath();
    if ( stagingSessionDir == null ) {
      //never been initialized
      return;
    }
    stagingSessionDir = stagingSessionDir.resolve( session.getId() );
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
    for ( final Future<IFixedSizeStreamingContent> entry : futures.values() ) {
      entry.cancel( true );
    }
    // forget all
    this.futures.clear();
    this.tasks.clear();

    AsyncJobFileStagingHandler.cleanStagingDir();
  }
}
