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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Thru staging handler represents 'thru' handler
 * where output stream is managed outside the handler.
 *
 * Typical example is web server response output stream.
 *
 *
 * Created by dima.prokopenko@gmail.com on 2/3/2016.
 */
public class ThruStagingHandler extends AbstractStagingHandler {

  private static final Log logger = LogFactory.getLog( ThruStagingHandler.class );

  private TrackingOutputStream thruTrackingStream;

  public ThruStagingHandler( final OutputStream outputStream, final IPentahoSession userSession ) throws IOException {
    super( outputStream, userSession );
  }

  @Override
  protected void initialize() {
    logger.trace( "Staging mode set - THRU" );
    this.thruTrackingStream = new TrackingOutputStream( super.outputStream );
  }

  @Override
  public void complete() {
    // Nothing to do for THRU - the output already has it's stuff
  }

  @Override
  public void close() {
    // Will be closed by server app?
  }

  @Override
  public int getWrittenByteCount() {
    return thruTrackingStream.getTrackingSize();
  }

  /**
   * Typically this handler operates with direct response.output stream
   * after something already got written into,
   * we can't write headers.
   *
   * @return number of byte buckets
   */
  @Override
  public boolean canSendHeaders() {
    return getWrittenByteCount() > 0 ? false : true;
  }

  @Override
  public OutputStream getStagingOutputStream() {
    return thruTrackingStream;
  }

  @Override
  public boolean isFullyBuffered() {
    return false;
  }

  @Override
  public StagingMode getStagingMode() {
    return StagingMode.THRU;
  }
}
