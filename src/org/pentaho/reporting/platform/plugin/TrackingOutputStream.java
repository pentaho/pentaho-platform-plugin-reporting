package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 15:53:55
 *
 * @author Thomas Morgner.
 */
public class TrackingOutputStream extends OutputStream
{
  private int trackingSize;
  private OutputStream wrappedStream;

  public TrackingOutputStream(final OutputStream wrapped)
  {
    this.wrappedStream = wrapped;
  }

  public void write(final int b) throws IOException
  {
    wrappedStream.write(b);
    trackingSize++;
  }

  public void write(final byte[] b) throws IOException
  {
    wrappedStream.write(b);
    trackingSize += b.length;
  }

  public void write(final byte[] b, final int off, final int len) throws IOException
  {
    wrappedStream.write(b, off, len);
    trackingSize += len;
  }

  public void flush() throws IOException
  {
    wrappedStream.flush();
  }

  public void close() throws IOException
  {
    wrappedStream.close();
  }

  public OutputStream getWrappedStream()
  {
    return wrappedStream;
  }

  public int getTrackingSize()
  {
    return trackingSize;
  }
}
