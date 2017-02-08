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

import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TempFileStagingTest {

  private static String TEST = UUID.randomUUID().toString();
  private IPentahoSession session = mock( IPentahoSession.class );
  private IApplicationContext appContext = mock( IApplicationContext.class );

  @Before
  public void before() {
    when( session.getId() ).thenReturn( "test" );
    PentahoSystem.init();

    // mock solution temp folder path
    when( appContext.getSolutionPath( anyString() ) ).thenReturn( "target/test/resource/solution" );
    PentahoSystem.setApplicationContext( appContext );
  }

  @Test
  public void testComplete() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );

    handler.getStagingOutputStream().write( TEST.getBytes() );

    assertTrue( "complete was not called", new String( baos.toByteArray() ).isEmpty() );

    handler.complete();

    // trim string from tail 00 bytes...
    assertEquals( "Write output only after complete", TEST, baos.toString().trim() );
  }


  @Test
  public void testClose() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );

    assertTrue( handler.tmpFile.exists() );

    handler.close();

    assertFalse( handler.tmpFile.exists() );
  }

  @Test
  public void testFullyBuffered() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );

    assertTrue( handler.isFullyBuffered() );
  }

  @Test
  public void testStagingMode() throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );

    assertEquals( StagingMode.TMPFILE, handler.getStagingMode() );
  }

  @Test
  public void testCanWriteHeaders() throws Exception {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );

    assertTrue( handler.canSendHeaders() );
  }

  @Test
  public void testBytesWritten() throws Exception {
    try ( final ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
      final TempFileStagingHandler handler = new TempFileStagingHandler( baos, session );
      try {

        assertEquals( 0, handler.getWrittenByteCount() );

        final OutputStream stagingOutputStream = handler.getStagingOutputStream();
        stagingOutputStream.write( 1 );
        handler.complete();

        assertEquals( 1, handler.getWrittenByteCount() );
      } finally {
        handler.close();
      }
    }
  }

  @Test
  public void testLongSessionId() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    try ( final ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
      new TempFileStagingHandler( baos, new StandaloneSession( "verylongsessionname" ) );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }
}
