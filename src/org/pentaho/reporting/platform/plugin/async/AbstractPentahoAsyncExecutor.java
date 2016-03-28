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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by dima.prokopenko@gmail.com on 3/28/2016.
 */
public abstract class AbstractPentahoAsyncExecutor<K extends IAsyncReportExecution<S, V>, V extends IAsyncReportState, S>
  implements ILogoutListener, IPentahoSystemListener {

  private static final Log log = LogFactory.getLog( AbstractPentahoAsyncExecutor.class );

  private final Map<CompositeKey, Future<S>> futures = new ConcurrentHashMap<>();
  private final Map<CompositeKey, K> tasks = new ConcurrentHashMap<>();

  private final ExecutorService executorService;

  protected AbstractPentahoAsyncExecutor( int capacity ) {
    log.info( "Initialized reporting  async execution fixed thread pool with capacity: " + capacity );
    executorService =
      new PentahoAsyncCancellingExecutor( capacity, capacity, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>() );
    PentahoSystem.addLogoutListener( this );
  }

  protected UUID addTask( final K task, final IPentahoSession session ) {
    final UUID id = UUID.randomUUID();
    task.notifyTaskQueued( id );

    final CompositeKey key = AbstractPentahoAsyncExecutor.getCompositeKey( session, id );
    final Future<S> result = executorService.submit( task );

    futures.put( key, result );
    tasks.put( key, task );

    return id;
  }

  public void cleanFuture( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    final CompositeKey key = AbstractPentahoAsyncExecutor.getCompositeKey( session, id );

    Future<S> future = futures.remove( key );
    if ( future != null && ( !future.isDone() ) ) {
      future.cancel( true );
    }
    tasks.remove( key );
  }

  public V getReportState( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    final CompositeKey key = AbstractPentahoAsyncExecutor.getCompositeKey( session, id );
    final K runningTask = tasks.get( key );
    return runningTask == null ? null : runningTask.getState();
  }


  public Future<S> getFuture( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    final CompositeKey key = AbstractPentahoAsyncExecutor.getCompositeKey( session, id );
    return futures.get( key );
  }

  public void requestPage( UUID id, IPentahoSession session, int page ) {
    validateParams( id, session );
    final K runningTask = tasks.get( new CompositeKey( session, id ) );
    if ( runningTask != null ) {
      runningTask.requestPage( page );
    }
  }

  protected void validateParams( final UUID id, final IPentahoSession session ) {
    ArgumentNullException.validate( "uuid", id );
    ArgumentNullException.validate( "session", session );
  }

  @Override public void onLogout( final IPentahoSession iPentahoSession ) {
    if ( log.isDebugEnabled() ) {
      // don't expose full session id.
      log.debug( "killing async report execution cache for user: " + iPentahoSession.getName() );
    }
    int i = 0;
    for ( final Map.Entry<CompositeKey, Future<S>> entry : futures.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), iPentahoSession.getId() ) ) {
        // attempt to cancel running task
        entry.getValue().cancel( true );
        // remove all links to release GC
        futures.remove( entry.getKey() );
        i++;
      }
    }
    log.debug( "Removed " + i + "futures from user's session for name: " + iPentahoSession.getName() );

    i = 0;
    for ( final Map.Entry<CompositeKey, K> entry : tasks.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), iPentahoSession.getId() ) ) {
        tasks.remove( entry.getKey() );
        i++;
      }
    }
    log.debug( "Removed " + i + "registered tasks from user's session for name: " + iPentahoSession.getName() );
  }

  @Override public boolean startup( final IPentahoSession iPentahoSession ) {
    // don't see any useful actions now
    // may be register some supervisor here later?
    return true;
  }

  @Override public void shutdown() {
    // attempt to stop all
    for ( final Map.Entry<CompositeKey, Future<S>> entry : futures.entrySet() ) {
      entry.getValue().cancel( true );
    }
    // forget all
    this.futures.clear();
    this.tasks.clear();
  }

  public static CompositeKey getCompositeKey( final IPentahoSession session, UUID id ) {
    return new CompositeKey( session, id );
  }

  public static final class CompositeKey {

    private String sessionId;
    private String uuid;

    private CompositeKey( final IPentahoSession session, final UUID id ) {
      this.uuid = id.toString();
      this.sessionId = session.getId();
    }

    public String getSessionId() {
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
}
