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
  private StagingInputStream stagingInputStream;

  // package private for testing purpose
  File tmpFile;

  private static String stringParentDir = null;

  public AsyncJobFileStagingHandler( IPentahoSession userSession ) throws IOException {
    this.sessionId = userSession.getId();

    IApplicationContext context = PentahoSystem.getApplicationContext();
    stringParentDir = context == null ? null : context.getSolutionPath( "system/tmp" );

    this.initialize();
  }

  protected void initialize() throws IOException {
    logger.trace( "Staging mode set - TEMP_FILE, async report generation" );

    if ( stringParentDir == null ) {
      throw new IOException( "can't find system/tmp dir, asycn staging not possible." );
    }

    // /system/tmp/<STAGING_DIR_ATTR>/<session-id>/tmpFile
    Path stagingExecutionFolder = getStagingExecutionFolder( sessionId );
    if ( !stagingExecutionFolder.toFile().exists() ) {
      if ( !stagingExecutionFolder.toFile().mkdirs() ) {
        throw new IOException( "Unable to create staging async directory" );
      }
    }
    Path tempFilePath = stagingExecutionFolder.resolve( UUIDUtil.getUUIDAsString() + POSTFIX );
    tmpFile = tempFilePath.toFile();
    tmpFile.deleteOnExit();

    fileTrackingStream = new BufferedOutputStream( new FileOutputStream( tmpFile ) );
  }

  private Path getStagingExecutionFolder( String userSession ) {
    return getStagingDirPath().resolve( userSession );
  }

  public static Path getStagingDirPath() {
    return stringParentDir == null ? null : Paths.get( stringParentDir ).resolve( STAGING_DIR_ATTR );
  }

  public OutputStream getStagingOutputStream() {
    return fileTrackingStream;
  }

  public InputStream getStagingContent() throws FileNotFoundException {
    // hold a reference to object ot be able to close and release staging file.
    stagingInputStream = new StagingInputStream( new FileInputStream( tmpFile ) );
    return stagingInputStream;
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

    public StagingInputStream( InputStream in ) {
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
}
