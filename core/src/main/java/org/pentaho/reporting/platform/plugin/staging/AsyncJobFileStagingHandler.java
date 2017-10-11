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

package org.pentaho.reporting.platform.plugin.staging;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.platform.api.util.ITempFileDeleter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Async stage handler. Write to TEMP file but: - live between requests. - require to re-set output stream for
 * ready-to-fetch-request to fetch ready to use data.
 * <p/>
 * Created by dima.prokopenko@gmail.com on 2/10/2016.
 */
public class AsyncJobFileStagingHandler {

  public static final String POSTFIX = ".tmp";
  public static final String STAGING_DIR_ATTR = "asyncstaging";

  private static final Log logger = LogFactory.getLog( AsyncJobFileStagingHandler.class );

  private String sessionId;

  private OutputStream fileTrackingStream;

  // package private for testing purpose
  File tmpFile;

  private String stringParentDir = null;

  public AsyncJobFileStagingHandler( final IPentahoSession userSession ) throws IOException {
    this.sessionId = userSession.getId();

    final IApplicationContext context = PentahoSystem.getApplicationContext();
    stringParentDir = context == null ? null : context.getSolutionPath( "system/tmp" );

    this.initialize();
  }

  protected void initialize() throws IOException {
    logger.trace( "Staging mode set - TEMP_FILE, async report generation" );

    if ( stringParentDir == null ) {
      throw new IOException( "can't find system/tmp dir, asycn staging not possible." );
    }

    // /system/tmp/<STAGING_DIR_ATTR>/<session-id>/tmpFile
    final Path stagingExecutionFolder = getStagingExecutionFolder( sessionId );
    if ( !stagingExecutionFolder.toFile().exists() ) {
      if ( !stagingExecutionFolder.toFile().mkdirs() ) {
        throw new IOException( "Unable to create staging async directory" );
      }
    }
    final Path tempFilePath = stagingExecutionFolder.resolve( UUIDUtil.getUUIDAsString() + POSTFIX );
    tmpFile = tempFilePath.toFile();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    ITempFileDeleter deleter = null;
    if ( session != null ) {
      deleter = (ITempFileDeleter) session.getAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE );
    }
    if ( deleter != null ) {
      deleter.trackTempFile( tmpFile );
    } else {
      tmpFile.deleteOnExit();
    }

    fileTrackingStream = new BufferedOutputStream( new FileOutputStream( tmpFile ) );
  }

  private Path getStagingExecutionFolder( final String userSession ) {
    return getStagingDirPath().resolve( userSession );
  }

  public static Path getStagingDirPath() {
    final IApplicationContext context = PentahoSystem.getApplicationContext();
    final String solutionTempFolder = context == null ? null : context.getSolutionPath( "system/tmp" );
    return solutionTempFolder == null ? null : Paths.get( solutionTempFolder ).resolve( STAGING_DIR_ATTR );
  }

  public OutputStream getStagingOutputStream() {
    return fileTrackingStream;
  }

  public IFixedSizeStreamingContent getStagingContent() throws FileNotFoundException {
    return new FixedSizeStagingContent( tmpFile );
  }

  public static void cleanSession( final IPentahoSession session ) {
    ArgumentNullException.validate( "session", session );  //NON-NLS
    cleanSession( session.getId() );
  }

  public static void cleanSession( final String sessionId ) {
    // do it generic way according to staging handler was used?
    Path stagingSessionDir = AsyncJobFileStagingHandler.getStagingDirPath();
    if ( stagingSessionDir == null ) {
      //never been initialized
      return;
    }
    stagingSessionDir = stagingSessionDir.resolve( sessionId );
    final File sessionStagingContent = stagingSessionDir.toFile();

    // some lib can do it for me?
    try {
      if ( sessionStagingContent != null && sessionStagingContent.isDirectory()
        && sessionStagingContent.list().length == 0 ) {
        FileUtils.deleteDirectory( sessionStagingContent );
      }
    } catch ( final IOException e ) {
      logger.debug( "Unable delete temp files on session logout." );
    }
  }

  public static void cleanStagingDir() {
    // delete all staging dir
    final Path stagingDir = AsyncJobFileStagingHandler.getStagingDirPath();
    final File stagingDirFile;
    if ( stagingDir != null ) {
      stagingDirFile = stagingDir.toFile();
      try {
        FileUtils.deleteDirectory( stagingDirFile );
      } catch ( final IOException e ) {
        logger.debug( "Unable to delete async staging content on shutdown. Directory: " + stagingDirFile.getName(), e );
      }
    }
  }

  public static final class FixedSizeStagingContent implements IFixedSizeStreamingContent {

    private InputStream in;
    private long size;
    File tmpFile;

    public FixedSizeStagingContent( File tmpFile ) {
      this.size = tmpFile.length();
      this.tmpFile = tmpFile;
    }

    @Override public InputStream getStream() {
      FileInputStream stagingInputStream = null;
      try {
        stagingInputStream = new FileInputStream( tmpFile );
      } catch ( FileNotFoundException e ) {
        logger.error( "staging file not found: " + tmpFile.toPath().toString() );
      }
      return stagingInputStream;
    }

    @Override public long getContentSize() {
      return size;
    }

    @Override public boolean cleanContent() {
      return tmpFile.delete();
    }
  }
}
