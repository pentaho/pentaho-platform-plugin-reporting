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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.security.SecurityHelper;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.File;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PentahoAsyncExecutor<TReportState extends IAsyncReportState>
        implements ILogoutListener, IPentahoSystemListener, IPentahoAsyncExecutor<TReportState> {

  public static final String BEAN_NAME = "IPentahoAsyncExecutor";

  private static final Log log = LogFactory.getLog( PentahoAsyncExecutor.class );
  public static final String UNABLE_DELETE_TEMP_FILES_ON_SESSION_LOGOUT = "Unable delete temp files on session logout.";

  private Map<CompositeKey, ListenableFuture<IFixedSizeStreamingContent>> futures = new ConcurrentHashMap<>();
  private Map<CompositeKey, IAsyncReportExecution<TReportState>> tasks = new ConcurrentHashMap<>();

  private ListeningExecutorService executorService;

  /**
   * @param capacity
   */
  public PentahoAsyncExecutor( final int capacity ) {
    log.info( "Initialized reporting  async execution fixed thread pool with capacity: " + capacity );
    executorService =
      new DelegatedListenableExecutor( new ThreadPoolExecutor( capacity, capacity, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>() ) );
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

  @Override
  public void schedule( final UUID id, final IPentahoSession session ) {
    validateParams( id, session );
    final CompositeKey compositeKey = new CompositeKey( session, id );
    final IAsyncReportExecution<TReportState> runningTask = tasks.get( compositeKey );
    if ( runningTask != null ) {
      if ( runningTask.schedule() ) {
        final ListenableFuture<IFixedSizeStreamingContent> future = futures.get( compositeKey );
        Futures.addCallback( future, new FutureCallback<IFixedSizeStreamingContent>() {
          @Override
          public void onSuccess( final IFixedSizeStreamingContent result ) {
            try {
              if ( session != null && !StringUtil.isEmpty( session.getName() ) ) {
                SecurityHelper.getInstance().runAsUser( session.getName(),
                        new WriteToJcrTask( id, session, runningTask, result.getStream() ) );
              }

            } catch ( final Exception e ) {
              log.error( "Can't execute callback. : ", e );
            }
          }

          @Override public void onFailure( final Throwable t ) {
            log.error( "Can't execute callback. Parent task failed: ", t );
          }
        } );
      }
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

    boolean hasScheduled = false;

    for ( final Map.Entry<CompositeKey, ListenableFuture<IFixedSizeStreamingContent>> entry : futures.entrySet() ) {
      if ( ObjectUtils.equals( entry.getKey().getSessionId(), session.getId() ) ) {

        final IAsyncReportExecution<TReportState> task = tasks.get( entry.getKey() );

        final ListenableFuture<IFixedSizeStreamingContent> value = entry.getValue();

        if ( task != null && task.getState() != null && AsyncExecutionStatus.SCHEDULED
          .equals( task.getState().getStatus() ) ) {
          hasScheduled = true;
          //Don't remove scheduled task
          continue;
        }

        //Close tmp files for completed futures
        Futures.addCallback( value, new FutureCallback<IFixedSizeStreamingContent>() {
          @Override public void onSuccess( final IFixedSizeStreamingContent result ) {
            if ( result != null ) {
              IOUtils.closeQuietly( result.getStream() );
            }
          }

          @Override public void onFailure( final Throwable ignored ) {
            //we don't care here anymore
          }
        } );

        // attempt to cancel running task
        value.cancel( true );

        // remove all links to release GC
        futures.remove( entry.getKey() );
        tasks.remove( entry.getKey() );
      }
    }

    //Don't try to remove directory if you have scheduled jobs, try to take care of it when jobs finishs
    if ( !hasScheduled ) {
      AsyncJobFileStagingHandler.cleanSession( session );
    }
  }

  private void tryCleanStagingDir( final IPentahoSession iPentahoSession ) {
    final File sessionDir = getStagingDir( iPentahoSession );
    if ( sessionDir != null && sessionDir.isDirectory() && sessionDir.list().length == 0 ) {
      try {
        FileUtils.deleteDirectory( sessionDir );
      } catch ( final IOException e ) {
        log.debug( UNABLE_DELETE_TEMP_FILES_ON_SESSION_LOGOUT );
      }
    }
  }

  private File getStagingDir( final IPentahoSession iPentahoSession ) {

    // do it generic way according to staging handler was used?
    Path stagingSessionDir = AsyncJobFileStagingHandler.getStagingDirPath();
    if ( stagingSessionDir == null ) {
      //never been initialized
      return null;
    }
    stagingSessionDir = stagingSessionDir.resolve( iPentahoSession.getId() );
    return stagingSessionDir.toFile();
  }

  @Override public boolean startup( final IPentahoSession iPentahoSession ) {
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


  private class WriteToJcrTask implements Callable<Object> {

    static final String FORMAT = "%s(%d)%s";
    private static final String TXT = ".txt";
    private final UUID id;
    private final IPentahoSession safeSession;
    private final IAsyncReportExecution<TReportState> parentTask;
    private final InputStream inputStream;

    WriteToJcrTask( final UUID id, final IPentahoSession session,
                    final IAsyncReportExecution<TReportState> parentTask,
                    final InputStream inputStream ) {
      this.id = id;
      this.safeSession = session;
      this.parentTask = parentTask;

      this.inputStream = inputStream;
    }


    @Override public Object call() throws Exception {

      try {

        final IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );


        final org.pentaho.reporting.libraries.base.util.IOUtils utils = org.pentaho.reporting.libraries
          .base.util.IOUtils.getInstance();


        final ISchedulingDirectoryStrategy directoryStrategy = PentahoSystem.get( ISchedulingDirectoryStrategy.class );

        final RepositoryFile outputFolder = directoryStrategy.getSchedulingDir( repo );

        final ReportContentRepository repository = new ReportContentRepository( outputFolder );
        final ContentLocation dataLocation = repository.getRoot();


        final IAsyncReportState state = parentTask.getState();

        final String extension = MimeHelper.getExtension( state.getMimeType() );
        final String targetExt = extension != null ? extension : TXT;
        final String fullPath = state.getPath();
        String cleanFileName = utils.stripFileExtension( utils.getFileName( fullPath ) );
        if ( cleanFileName == null || cleanFileName.isEmpty() ) {
          cleanFileName = "content";
        }

        String targetName = cleanFileName + targetExt;

        int copy = 1;

        while ( dataLocation.exists( targetName ) ) {
          targetName = String.format( FORMAT, cleanFileName, copy, targetExt );
          copy++;
        }

        try ( final OutputStream outputStream = dataLocation.createItem( targetName ).getOutputStream() ) {
          IOUtils.copy( inputStream, outputStream );
          outputStream.flush();
        }

      } catch ( final Exception e ) {
        log.error( "Cant't persist report: ", e );
      } finally {
        IOUtils.closeQuietly( inputStream );
        cleanFuture( id, safeSession );
        tryCleanStagingDir( safeSession );
      }

      return null;
    }

  }
}
