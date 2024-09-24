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

package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.OutputStream;

public class TrackingOutputStream extends OutputStream {
  private int trackingSize;
  private OutputStream wrappedStream;

  public TrackingOutputStream( final OutputStream wrapped ) {
    this.wrappedStream = wrapped;
  }

  public void write( final int b ) throws IOException {
    wrappedStream.write( b );
    trackingSize++;
  }

  public void write( final byte[] b ) throws IOException {
    wrappedStream.write( b );
    trackingSize += b.length;
  }

  public void write( final byte[] b, final int off, final int len ) throws IOException {
    wrappedStream.write( b, off, len );
    trackingSize += len;
  }

  public void flush() throws IOException {
    wrappedStream.flush();
  }

  public void close() throws IOException {
    wrappedStream.close();
  }

  public OutputStream getWrappedStream() {
    return wrappedStream;
  }

  public int getTrackingSize() {
    return trackingSize;
  }
}
