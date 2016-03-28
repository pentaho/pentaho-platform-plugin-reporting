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

package org.pentaho.reporting.platform.plugin.staging;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;

import java.io.BufferedInputStream;
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
 * Async stage handler.
 * Write to TEMP file but:
 * - live between requests.
 * - require to re-set output stream for ready-to-fetch-request to fetch ready to use data.
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
    tmpFile.deleteOnExit();

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
    // hold a reference to object ot be able to close and release staging file.
    StagingInputStream stagingInputStream = new StagingInputStream( new FileInputStream( tmpFile ) );
    return new FixedSizeStagingContent( stagingInputStream, tmpFile.length() );
  }

  public static void cleanSession( final IPentahoSession iPentahoSession ) {
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
        logger.debug( "Unable to delete async staging content on shutdown. Directory: " + stagingDirFile.getName() );
      }
    }
  }

  public void close() {
    this.closeQuietly();
  }

  private void closeQuietly() {
    IOUtils.closeQuietly( fileTrackingStream );
    if ( tmpFile != null && tmpFile.exists() ) {
      try {
        boolean deleted = tmpFile.delete();
        if ( !deleted ) {
          logger.debug( "Unable to delete temp file: " + tmpFile.getName() );
        }
      } catch ( Exception ignored ) {
        logger.debug( "Exception when try to delete temp file" + tmpFile.getName() );
      }
    }
  }

  /**
   * Inner class to be able to close staging resources
   * after closing of InputStream.
   * <p/>
   * Holding a link to this particular InputStream object will prevent
   * GC to collect staging handler.
   */
  private class StagingInputStream extends BufferedInputStream {

    public StagingInputStream( final InputStream in ) {
      super( in );
    }

    @Override public void close() throws IOException {
      try {
        super.close();
        closeQuietly();
      } catch ( Exception e ) {
        logger.debug( "Attempt to close quietly is not quietly" );
      }
    }
  }

  public static final class FixedSizeStagingContent implements IFixedSizeStreamingContent {

    private InputStream in;
    private long size;

    public FixedSizeStagingContent ( InputStream in, long size ) {
      this.in = in;
      this.size = size;
    }

    @Override public InputStream getStream() {
      return in;
    }

    @Override public long getContentSize() {
      return size;
    }
  }
}
