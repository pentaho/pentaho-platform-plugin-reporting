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

package org.pentaho.reporting.platform.plugin.staging;

import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.platform.plugin.TrackingOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test main memory staging handler functionality.
 *
 * Created by dima.prokopenko@gmail.com on 2/4/2016.
 */
public class MemStagingHandlerTest {

  private static String TEST = UUID.randomUUID().toString();
  private IPentahoSession session = mock( IPentahoSession.class );

  @Test
  public void completeTest() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MemStagingHandler handler = new MemStagingHandler( baos, session );
    handler.getStagingOutputStream().write( TEST.getBytes() );

    assertTrue( "complete was not called", new String( baos.toByteArray() ).isEmpty() );

    handler.complete();

    // trim string from tail 00 bytes...
    assertEquals( "Write output only after complete", TEST, baos.toString().trim() );
  }

  @Test
  public void closeTest() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MemStagingHandler handler = new MemStagingHandler( baos, session );

    OutputStream spy = Mockito.spy( handler.getStagingOutputStream() );
    // to be able to spy on inner object
    handler.memoryTrackingStream = (TrackingOutputStream) spy;

    handler.close();
    verify( spy, times(1) ).close();
  }

  @Test
  public void testFullyBuffered() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MemStagingHandler handler = new MemStagingHandler( baos, session );

    assertTrue( handler.isFullyBuffered() );
  }

  @Test
  public void testStagingMode() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MemStagingHandler handler = new MemStagingHandler( baos, session );

    assertEquals( StagingMode.MEMORY, handler.getStagingMode() );
  }
}
