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
    super(outputStream, userSession);
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
