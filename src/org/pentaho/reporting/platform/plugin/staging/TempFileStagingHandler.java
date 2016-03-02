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
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

import java.io.*;

/**
 * Write data to FS temp file
 * On complete phase write from temp file to output streams
 *
 * Created by dima.prokopenko@gmail.com on 2/3/2016.
 */
public class TempFileStagingHandler extends AbstractStagingHandler {

  private static final Log logger = LogFactory.getLog( TempFileStagingHandler.class );

  private static String PREFIX = "repstg";
  private static String POSTFIX = ".tmp";

  private TrackingOutputStream fileTrackingStream;
  // same-package junit test access
  File tmpFile;


  public TempFileStagingHandler( final OutputStream outputStream, final IPentahoSession userSession )
      throws IOException {
    super( outputStream, userSession );
  }

  @Override
  protected void initialize() throws IOException {
    logger.trace( "Staging mode set - TEMP_FILE" );

    final IApplicationContext appCtx = PentahoSystem.getApplicationContext();
    // Use the deleter framework for safety...
    if ( userSession.getId().length() >= 10 ) {
      tmpFile = appCtx.createTempFile( userSession, PREFIX, POSTFIX, true );
    } else {
      // Workaround bug in appContext.createTempFile ... :-(
      final File parentDir = new File( appCtx.getSolutionPath( "system/tmp" ) ); //$NON-NLS-1$
      final ITempFileDeleter fileDeleter =
          (ITempFileDeleter) userSession.getAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE );
      final String newPrefix = PREFIX + UUIDUtil.getUUIDAsString().substring( 0, 10 ) + "-";
      tmpFile = File.createTempFile( newPrefix, POSTFIX, parentDir );
      if ( fileDeleter != null ) {
        fileDeleter.trackTempFile( tmpFile );
      } else {
        // There is no deleter, so cleanup on VM exit. (old behavior)
        tmpFile.deleteOnExit();
      }
    }

    fileTrackingStream = new TrackingOutputStream( new BufferedOutputStream( new FileOutputStream( tmpFile ) ) );
  }

  /**
   * Write from temp file to destination output stream
   *
   * @throws IOException
   */
  @Override
  public void complete() throws IOException {
    IOUtils.closeQuietly( fileTrackingStream );
    final BufferedInputStream bis = new BufferedInputStream( new FileInputStream( tmpFile ) );
    try {
      IOUtils.copy( bis, outputStream );
    } finally {
      IOUtils.closeQuietly( bis );
    }
  }

  /**
   * Close output stream if not already closed,
   * delete temporary file.
   *
   */
  @Override
  public void close() {
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

  @Override public int getWrittenByteCount() {
    return fileTrackingStream.getTrackingSize();
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
  public boolean isFullyBuffered() {
    return true;
  }

  @Override
  public StagingMode getStagingMode() {
    return StagingMode.TMPFILE;
  }
}
