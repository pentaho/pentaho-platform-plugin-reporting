package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Todo: Document me!
 * <p/>
 * Date: 02.02.11
 * Time: 17:12
 *
 * @author Thomas Morgner.
 */
public class ProxyOutputStream extends OutputStream
{
  private OutputStream parent;

  public ProxyOutputStream()
  {
  }

  public OutputStream getParent()
  {
    return parent;
  }

  public void setParent(final OutputStream parent)
  {
    this.parent = parent;
  }

  public void write(final int b) throws IOException
  {
    if (parent != null)
    {
      parent.write(b);
    }
  }

  public void write(final byte[] b) throws IOException
  {
    if (parent != null)
    {
      parent.write(b);
    }
  }

  public void write(final byte[] b, final int off, final int len) throws IOException
  {
    if (parent != null)
    {
      parent.write(b, off, len);
    }
  }

  public void flush() throws IOException
  {
    if (parent != null)
    {
      parent.flush();
    }
  }

  public void close() throws IOException
  {
    if (parent != null)
    {
      parent.close();
    }
  }
}
