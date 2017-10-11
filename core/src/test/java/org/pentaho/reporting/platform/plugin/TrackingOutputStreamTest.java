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

package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.any;

public class TrackingOutputStreamTest extends TestCase {
  TrackingOutputStream stream, streamSpy;
  OutputStream outputStream;

  @Before
  public void setUp() {
    outputStream = mock( OutputStream.class );
    stream = new TrackingOutputStream( outputStream );
    streamSpy = spy( stream );
  }

  @Test
  public void testWrite() throws Exception {
    streamSpy.write( 1 );
    verify( streamSpy, times( 1 ) ).write( anyInt() );
    assertEquals( 1, streamSpy.getTrackingSize() );

    streamSpy.write( new byte[] { 1 } );
    verify( streamSpy, times( 1 ) ).write( any( byte[].class ) );
    assertEquals( 2, streamSpy.getTrackingSize() );

    streamSpy.write( new byte[] { 1 }, 0, 3 );
    verify( streamSpy, times( 1 ) ).write( any( byte[].class ), anyInt(), anyInt() );
    assertEquals( 5, streamSpy.getTrackingSize() );
  }

  @Test
  public void testGetStream() throws Exception {
    assertEquals( outputStream, streamSpy.getWrappedStream() );
  }

  @Test
  public void testFlush() throws Exception {
    streamSpy.flush();
    verify( streamSpy, times( 1 ) ).flush();
  }

  @Test
  public void testClose() throws Exception {
    streamSpy.close();
    verify( streamSpy, times( 1 ) ).close();
  }
}
