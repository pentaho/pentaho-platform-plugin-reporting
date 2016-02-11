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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

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
 * - do not keep link to passed output stream not write to output stream passed in constructor.
 * - live between requests.
 * - require to re-set output stream for ready-to-fetch-request to fetch ready to use data.
 *
 *
 *
 * Created by dima.prokopenko@gmail.com on 2/10/2016.
 */
public class AsyncJobFileStagingHandler extends AbstractStagingHandler {

  private static String POSTFIX = ".tmp";

  public static final String STAGING_DIR_ATTR = "asyncstaging";

  private static final Log logger = LogFactory.getLog( AsyncJobFileStagingHandler.class );

  private TrackingOutputStream fileTrackingStream;
  // package private for testing purpose
  File tmpFile;

  private static String stringParentDir = ".";

  public AsyncJobFileStagingHandler( OutputStream outputStream, IPentahoSession userSession, String stringParentDir ) throws IOException {
    // do not write to output stream passed in constructor,
    // do not keep link to parent output stream, just forget it.
    // do not to leak.
    super( null, userSession );
    this.stringParentDir = stringParentDir;
  }

  @Override
  protected void initialize() throws IOException {
    logger.trace( "Staging mode set - TEMP_FILE, async report generation" );
    if ( stringParentDir == null ) {
      throw new IOException( "can't find system/tmp dir, asycn staging not possible." );
    }

    // /system/tmp/<STAGING_DIR_ATTR>/<session-id>/tmpFile
    Path stagingExecutionFolder = getStagingExecutionFolder( userSession );
    if ( !stagingExecutionFolder.toFile().exists() ) {
      if ( !stagingExecutionFolder.toFile().mkdirs() ) {
        throw new IOException( "Unable to create staging async directory" );
      }
    }
    Path tempFilePath = stagingExecutionFolder.resolve( UUIDUtil.getUUIDAsString() + POSTFIX );
    tmpFile = tempFilePath.toFile();

    final Object fileDeleterObj = userSession.getAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE );
    if ( fileDeleterObj instanceof ITempFileDeleter ) {
      ITempFileDeleter fileDeleter = ITempFileDeleter.class.cast( fileDeleterObj );
      fileDeleter.trackTempFile( tmpFile );
    } else {
      logger.debug( "Not found: " + ITempFileDeleter.class.getCanonicalName() );
      tmpFile.deleteOnExit();
    }
    fileTrackingStream = new TrackingOutputStream( new BufferedOutputStream( new FileOutputStream( tmpFile ) ) );
  }

  private Path getStagingExecutionFolder( IPentahoSession userSession ) {
    return getStagingDirPath().resolve( userSession.getId() );
  }

  public static Path getStagingDirPath() {
    return Paths.get( stringParentDir ).resolve( STAGING_DIR_ATTR );
  }

  @Override
  public StagingMode getStagingMode() {
    return StagingMode.TMPFILE;
  }

  @Override
  public boolean isFullyBuffered() {
    return true;
  }

  @Override
  public boolean canSendHeaders() {
    return true;
  }

  @Override
  public OutputStream getStagingOutputStream() {
    return fileTrackingStream;
  }

  @Override
  public void complete() throws IOException {
    logger.debug(  "Call NO-OP complete: " + this.getClass().getName() );
  }

  public InputStream getStagingContent() throws FileNotFoundException {
    return new StagingInputStream( new FileInputStream( tmpFile ) );
  }

  @Override
  public void close() {
    this.closeQuietly();
  }

  @Override
  public int getWrittenByteCount() {
    return fileTrackingStream.getTrackingSize();
  }

  private void closeQuietly() {
    IOUtils.closeQuietly( fileTrackingStream );
    if ( tmpFile != null && tmpFile.exists() ) {
      try {
        boolean deleted = tmpFile.delete();
        if ( !deleted ) {
          logger.debug( "Unable to delete temp file for user: " + userSession.getName() );
        }
      } catch ( Exception ignored ) {
        logger.debug( "Exception when try to delete temp file for user: " + userSession.getName() );
      }
    }
  }

  /**
   * Inner class to be able to close staging resources
   * after closing of InputStream.
   *
   * Holding a link to this particular InputStream object will prevent
   * GC to collect staging handler.
   */
  private class StagingInputStream extends BufferedInputStream {

    public StagingInputStream( InputStream in ) {
      super( in );
    }

    @Override
    public void close() throws IOException {
      try {
        closeQuietly();
      } catch ( Exception e ) {
        logger.debug( "Attempt to close quietly is not quietly" );
      } finally {
        super.close();
      }
    }
  }
}
