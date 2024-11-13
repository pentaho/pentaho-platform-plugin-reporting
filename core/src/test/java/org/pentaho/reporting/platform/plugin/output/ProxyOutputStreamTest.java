/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.output;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
