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
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

public class PentahoAsyncExecutor<TReportState extends IAsyncReportState>
  implements ILogoutListener, IPentahoAsyncExecutor<TReportState> {

  public static final String BEAN_NAME = "IPentahoAsyncExecutor";

  private static final Log log = LogFactory.getLog( PentahoAsyncExecutor.class );

  private Map<CompositeKey, ListenableFuture<IFixedSizeStreamingContent>> futures = new ConcurrentHashMap<>();
  private Map<CompositeKey, IAsyncReportExecution<TReportState>> tasks = new ConcurrentHashMap<>();

  private ListeningExecutorService executorService;

  private final int autoSchedulerThreshold;
  private final MemorizeSchedulingLocationListener schedulingLocationListener;
  private Map<CompositeKey, ISchedulingListener> writeToJcrListeners;

  /**
   * @param capacity               thread pool capacity
   * @param autoSchedulerThreshold quantity of rows after which reports are automatically scheduled
   */
  public PentahoAsyncExecutor( final int capacity, final int autoSchedulerThreshold ) {
    this.autoSchedulerThreshold = autoSchedulerThreshold;
    log.info( "Initialized reporting async execution fixed thread pool with capacity: " + capacity );
    executorService =
      new DelegatedListenableExecutor( new ThreadPoolExecutor( capacity, capacity, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(), new ThreadFactory() {
          @Override
          public Thread newThread( Runnable r ) {
            Thread thread = Executors.defaultThreadFactory().newThread( r );
            thread.setDaemon( true );
            thread.setName( "PentahoAsyncExecutor Thread Pool" );
            return thread;
          }
        } ) );
    PentahoSystem.addLogoutListener( this );
    this.writeToJcrListeners = new ConcurrentHashMap<>();
    this.schedulingLocationListener = new MemorizeSchedulingLocationListener();
  }


  @Deprecated
  public PentahoAsyncExecutor( final int capacity ) {
    this( capacity, 0 );
  }

  /**
   * This executor stores jobs (identified by their id) in a separate partition for each user (identified by the
   * session-id). We don't let others access our session or job-id, but need to match against the session-id for
   * onLogout clean-ups.
   */
  public static class CompositeKey {

    private String sessionId;
    private String uuid;

    // default visibility for testing purpose
    CompositeKey( final IPentahoSession session, final UUID id ) {
      this.uuid = id.toString();
      this.sessionId = session.getId();
    }

    public boolean isSameSession( final String sessionId ) {
      return StringUtils.equals( sessionId, this.sessionId );
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
    return addTask( task, session, UUID.randomUUID() );
  }

  @Override
  public UUID addTask( final IAsyncReportExecution<TReportState> task, final IPentahoSession session, final UUID id ) {
    final CompositeKey key = new CompositeKey( session, id );

    task.notifyTaskQueued( id,
      Collections.singletonList( new AutoScheduleListener( id, session, autoSchedulerThreshold, this ) ) );

    log.debug( "register async execution for task: " + task.toString() );

    final ListenableFuture<IFixedSizeStreamingContent> result = executorService.submit( task );
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

  @Override public boolean preSchedule( final UUID uuid, final IPentahoSession session ) {
    validateParams( uuid, session );
    final CompositeKey compositeKey = new CompositeKey( session, uuid );
    final IAsyncReportExecution<? extends TReportState> runningTask = tasks.get( compositeKey );
    if ( runningTask != null ) {
      return runningTask.preSchedule();
    }
    return false;
  }

  @SuppressWarnings( "unchecked" )
  @Override public UUID recalculate( final UUID uuid, final IPentahoSession session ) {
    validateParams( uuid, session );
    final CompositeKey compositeKey = new CompositeKey( session, uuid );
    final IAsyncReportExecution<? extends TReportState> runningTask = tasks.get( compositeKey );

    if ( runningTask == null ) {
      throw new IllegalStateException( "We must have a task at this point." );
    }

    try {
      final IAsyncReportExecution<TReportState> recalcTask =
        (IAsyncReportExecution<TReportState>) new PentahoAsyncReportExecution( (PentahoAsyncReportExecution) runningTask,
          new AsyncJobFileStagingHandler( session ) );

      return addTask( recalcTask, session );

    } catch ( final Exception e ) {
      log.error( "Can't recalculate task: ", e );
    }

    return null;

  }

  @Override
  public boolean schedule( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    final CompositeKey compositeKey = new CompositeKey( session, id );
    final IAsyncReportExecution<TReportState> runningTask = tasks.get( compositeKey );
    final ListenableFuture<IFixedSizeStreamingContent> future = futures.get( compositeKey );

    if ( runningTask == null || future == null ) {
      // As long as we have a task, we should have a future-object, but checking both does not hurt.
      throw new IllegalStateException( "We must have a task and a future at this point." );
    }

    final String userId = session.getName();
    final String sessionId = session.getId();

    if ( !StringUtils.isEmpty( userId ) ) {
      if ( runningTask.schedule() ) {
        Futures.addCallback( future,
          new TriggerScheduledContentWritingHandler( userId, sessionId, runningTask, compositeKey ), executorService );
        return true;
      }
    }
    return false;
  }

  @Override
  public void updateSchedulingLocation( final UUID id, final IPentahoSession session, final Serializable folderId,
                                        final String newName ) {
    validateParams( id, session );
    final CompositeKey key = new CompositeKey( session, id );

    final IAsyncReportExecution<TReportState> runningTask = tasks.get( key );

    if ( runningTask == null ) {
      throw new IllegalStateException( "We must have a task at this point." );
    }

    final UpdateSchedulingLocationListener listener = getUpdateSchedulingLocationListener( folderId, newName );


    try {
      this.schedulingLocationListener.lock();
      final Serializable fileId = schedulingLocationListener.lookupOutputFile( key );
      if ( fileId != null ) {
        //Report is already finished and saved to default scheduling directory.
        // move it to a new location. This operation may move the file multiple times, as the file-id is independent
        // of the location.
        listener.onSchedulingCompleted( fileId );
      } else {
        //Report is not finished yet. Update the listener list within this synchronized block so that
        writeToJcrListeners.put( key, listener );
      }
    } finally {
      this.schedulingLocationListener.unlock();
    }


  }

  protected UpdateSchedulingLocationListener getUpdateSchedulingLocationListener( final Serializable folderId,
                                                                                  final String newName ) {
    return new UpdateSchedulingLocationListener( folderId, newName );
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

  @Override
  public void onLogout( final IPentahoSession session ) {
    if ( log.isDebugEnabled() ) {
      // don't expose full session id.
      log.debug( "killing async report execution cache for user: " + session.getName() );
    }

    for ( final Map.Entry<CompositeKey, ListenableFuture<IFixedSizeStreamingContent>> entry : futures.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), session.getId() ) ) {

        final IAsyncReportExecution<TReportState> task = tasks.get( entry.getKey() );

        final ListenableFuture<IFixedSizeStreamingContent> value = entry.getValue();

        if ( task != null && task.getState() != null && AsyncExecutionStatus.SCHEDULED
          .equals( task.getState().getStatus() ) ) {
          //After the session end nobody can poll status, we can remove task
          //Keep future to have content in place
          tasks.remove( entry.getKey() );
          continue;
        }

        // attempt to cancel running task
        value.cancel( true );

        // remove all links to release GC
        futures.remove( entry.getKey() );
        tasks.remove( entry.getKey() );
      }
    }

    //User can't update scheduling directory after logout, so we can clean location locationMap
    try {
      this.schedulingLocationListener.lock();
      this.schedulingLocationListener.onLogout( session.getId() );
    } finally {
      this.schedulingLocationListener.unlock();
    }

    //If some files are still open directory won't be removed
    AsyncJobFileStagingHandler.cleanSession( session );

  }

  @Override
  public void shutdown() {
    // attempt to stop all
    for ( final Future<IFixedSizeStreamingContent> entry : futures.values() ) {
      entry.cancel( true );
    }
    // forget all
    this.futures.clear();
    this.tasks.clear();
    this.writeToJcrListeners.clear();
    this.executorService.shutdown();
    try {
      this.schedulingLocationListener.lock();
      this.schedulingLocationListener.shutdown();
    } finally {
      this.schedulingLocationListener.unlock();
    }

    AsyncJobFileStagingHandler.cleanStagingDir();
  }

  protected Callable<Serializable> getWriteToJcrTask( final IFixedSizeStreamingContent result,
                                                      final IAsyncReportExecution<? extends IAsyncReportState>
                                                        runningTask ) {
    return new WriteToJcrTask( runningTask, result.getStream() );
  }

  /**
   * This class is responsible for writing the content first to a pre-computed location (as specified by the
   * ISchedulingDirectoryStrategy implementation, and then optionally moves the content to a location specified by the
   * user (via the UI).
   */
  class TriggerScheduledContentWritingHandler implements FutureCallback<IFixedSizeStreamingContent> {
    private final IAsyncReportExecution<TReportState> runningTask;
    private final CompositeKey compositeKey;
    private final String user;
    private final String sessionId;

    TriggerScheduledContentWritingHandler( final String user, final String sessionId,
                                           final IAsyncReportExecution<TReportState> runningTask,
                                           final CompositeKey compositeKey ) {
      this.user = user;
      this.sessionId = sessionId;
      this.runningTask = runningTask;
      this.compositeKey = compositeKey;
    }

    protected IFixedSizeStreamingContent notifyListeners( final IFixedSizeStreamingContent result ) throws Exception {
      final Serializable writtenTo = getWriteToJcrTask( result, runningTask ).call();
      if ( writtenTo == null ) {
        log.debug( "Unable to move scheduled content, due to error while creating content in default location." );
        return null;
      }
      try {
        PentahoAsyncExecutor.this.schedulingLocationListener.lock();
        PentahoAsyncExecutor.this.schedulingLocationListener.recordOutputFile( compositeKey, writtenTo );
        notifyListeners( writtenTo );
      } finally {
        PentahoAsyncExecutor.this.schedulingLocationListener.unlock();
      }

      return null;
    }

    protected void notifyListeners( final Serializable writtenTo ) {
      //We can be sure it succeed here and are ready to notify writeToJcrListeners
      final ISchedulingListener iSchedulingListener = writeToJcrListeners.get( compositeKey );

      if ( iSchedulingListener != null ) {
        iSchedulingListener.onSchedulingCompleted( writtenTo );
        writeToJcrListeners.remove( compositeKey );
      }
    }

    @Override
    public void onSuccess( final IFixedSizeStreamingContent result ) {
      try {
        if ( user != null && !StringUtil.isEmpty( user ) ) {
          SecurityHelper.getInstance().runAsUser( user, () -> notifyListeners( result ) );
        }
      } catch ( final Exception e ) {
        log.error( "Can't execute callback. : ", e );
      } finally {
        //Time to remove future - nobody will ask for content at this moment
        //We need to keep task because status polling may still occur ( or it already has been removed on logout )
        //Also we can try to remove directory
        futures.remove( compositeKey );
        result.cleanContent();
        AsyncJobFileStagingHandler.cleanSession( sessionId );
      }
    }

    @Override public void onFailure( final Throwable t ) {
      log.error( "Can't execute callback. Parent task failed: ", t );
      futures.remove( compositeKey );
      AsyncJobFileStagingHandler.cleanSession( sessionId );
    }
  }
}
