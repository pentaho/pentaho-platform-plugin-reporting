/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin.staging;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

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
    tmpFile = appCtx.createTempFile( userSession, PREFIX, POSTFIX, true );

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
