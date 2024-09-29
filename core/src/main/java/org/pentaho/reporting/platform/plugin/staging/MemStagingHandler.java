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
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.libraries.base.util.MemoryByteArrayOutputStream;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Memory staging stream, write content directly into memory.
 *
 * Created by dima.prokopenko@gmail.com on 2/3/2016.
 */
public class MemStagingHandler extends AbstractStagingHandler {

  private static final Log logger = LogFactory.getLog( MemStagingHandler.class );

  // visible for testing
  TrackingOutputStream memoryTrackingStream;

  public MemStagingHandler( final OutputStream outputStream, final IPentahoSession userSession ) throws IOException {
    super( outputStream, userSession );
  }

  @Override
  protected void initialize() throws IOException {
    logger.trace( "Staging mode set - MEM" );
    memoryTrackingStream = new TrackingOutputStream( new MemoryByteArrayOutputStream() );
  }

  /**
   * write from memory source to output stream
   * passed in constructor.
   *
   * @throws IOException
   */
  @Override
  public void complete() throws IOException {
    final MemoryByteArrayOutputStream stream = (MemoryByteArrayOutputStream) memoryTrackingStream.getWrappedStream();
    final ByteArrayInputStream inputStream = new ByteArrayInputStream( stream.getRaw() );
    IOUtils.copy( inputStream, outputStream );
  }

  @Override
  public void close() {
    if ( memoryTrackingStream != null ) {
      try {
        memoryTrackingStream.close();
      } catch ( final IOException e ) {
        logger.debug( "Unable to close memory stream? (never happens)" );
      }
    }
  }

  @Override public int getWrittenByteCount() {
    return memoryTrackingStream.getTrackingSize();
  }

  @Override
  public boolean canSendHeaders() {
    return true;
  }

  @Override
  public OutputStream getStagingOutputStream() {
    return memoryTrackingStream;
  }

  @Override
  public boolean isFullyBuffered() {
    return true;
  }

  @Override
  public StagingMode getStagingMode() {
    return StagingMode.MEMORY;
  }
}
