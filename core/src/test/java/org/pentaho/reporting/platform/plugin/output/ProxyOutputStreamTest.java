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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProxyOutputStreamTest extends TestCase {
  ProxyOutputStream stream;

  @Before
  public void setUp() {
    stream = new ProxyOutputStream();
  }

  @Test
  public void testSetParent() throws Exception {
    OutputStream outputStream = mock( OutputStream.class );
    stream.setParent( outputStream );
    assertEquals( outputStream, stream.getParent() );
  }

  @Test
  public void testWrite() throws Exception {
    OutputStream outputStream = mock( OutputStream.class );
    stream.setParent( outputStream );

    stream.write( 1 );
    stream.write( new byte[] { 1 } );
    stream.write( new byte[] { 1 }, 0, 3 );

    verify( outputStream, times( 1 ) ).write( anyInt() );
    verify( outputStream, times( 1 ) ).write( any( byte[].class ) );
    verify( outputStream, times( 1 ) ).write( any( byte[].class ), anyInt(), anyInt() );
  }

  @Test
  public void testFlush() throws Exception {
    OutputStream outputStream = mock( OutputStream.class );
    stream.setParent( outputStream );

    stream.flush();
    verify( outputStream, times( 1 ) ).flush();
  }

  @Test
  public void testClose() throws Exception {
    OutputStream outputStream = mock( OutputStream.class );
    stream.setParent( outputStream );

    stream.close();
    verify( outputStream, times( 1 ) ).close();
  }
}
