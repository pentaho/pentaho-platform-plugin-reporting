/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

public class ProxyOutputStream extends OutputStream {
  private OutputStream parent;

  public ProxyOutputStream() {
  }

  public OutputStream getParent() {
    return parent;
  }

  public void setParent( final OutputStream parent ) {
    this.parent = parent;
  }

  public void write( final int b ) throws IOException {
    if ( parent != null ) {
      parent.write( b );
    }
  }

  public void write( final byte[] b ) throws IOException {
    if ( parent != null ) {
      parent.write( b );
    }
  }

  public void write( final byte[] b, final int off, final int len ) throws IOException {
    if ( parent != null ) {
      parent.write( b, off, len );
    }
  }

  public void flush() throws IOException {
    if ( parent != null ) {
      parent.flush();
    }
  }

  public void close() throws IOException {
    if ( parent != null ) {
      parent.close();
    }
  }
}
