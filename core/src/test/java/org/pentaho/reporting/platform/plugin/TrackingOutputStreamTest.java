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
