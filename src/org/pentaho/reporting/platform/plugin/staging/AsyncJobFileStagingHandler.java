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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;

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

  private static final String POSTFIX = ".tmp";
  static final String STAGING_DIR_ATTR = "asyncstaging";

  private static final Log logger = LogFactory.getLog( AsyncJobFileStagingHandler.class );
  private static final String SYSTEM_TMP = "system/tmp";
  private static final String UNABLE_TO_DELETE_STAGING_DIR =
    "Unable to delete async staging content. Directory: ";
  private static final String UNABLE_TO_DELETE_SESSION_DIR = "Unable delete temp  session files.";
  private static final String STAGING_MODE_SET = "Staging mode set - TEMP_FILE, async report generation";

  private String sessionId;

  private OutputStream fileTrackingStream;

  // package private for testing purpose
  private File tmpFile;

  private String stringParentDir = null;

  public AsyncJobFileStagingHandler( final IPentahoSession userSession ) throws IOException {
    this.sessionId = userSession.getId();

    final IApplicationContext context = PentahoSystem.getApplicationContext();
    stringParentDir = context == null ? null : context.getSolutionPath( SYSTEM_TMP );

    this.initialize();
  }

  protected void initialize() throws IOException {
    logger.trace( STAGING_MODE_SET );

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
    final String solutionTempFolder = context == null ? null : context.getSolutionPath( SYSTEM_TMP );
    return solutionTempFolder == null ? Paths.get( FileUtils.getTempDirectoryPath() ) : Paths.get( solutionTempFolder ).resolve( STAGING_DIR_ATTR );
  }

  public OutputStream getStagingOutputStream() {
    return fileTrackingStream;
  }

  public IFixedSizeStreamingContent getStagingContent() throws FileNotFoundException {
    return new FixedSizeStagingContent( tmpFile );
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
      if ( sessionStagingContent != null && sessionStagingContent.isDirectory()
        && sessionStagingContent.list().length == 0 ) {
        FileUtils.deleteDirectory( sessionStagingContent );
      }
    } catch ( final IOException e ) {
      logger.debug( UNABLE_TO_DELETE_SESSION_DIR );
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
        logger.debug( UNABLE_TO_DELETE_STAGING_DIR + stagingDirFile.getName(), e );
      }
    }
  }

  public static final class FixedSizeStagingContent implements IFixedSizeStreamingContent {


    static final String STAGING_FILE_NOT_FOUND = "staging file not found: ";
    private long size;
    File tmpFile;

    public FixedSizeStagingContent( final File tmpFile ) {
      this.size = tmpFile.length();
      this.tmpFile = tmpFile;
    }

    @Override public InputStream getStream() {
      FileInputStream stagingInputStream = null;
      try {
        stagingInputStream = new FileInputStream( tmpFile );
      } catch ( final FileNotFoundException e ) {
        logger.error( STAGING_FILE_NOT_FOUND + tmpFile.toPath().toString() );
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
