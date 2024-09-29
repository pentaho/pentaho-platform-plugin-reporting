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
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by dima.prokopenko@gmail.com on 2/4/2016.
 */
public class ThruStagingHandlerTest {

  private static String TEST = UUID.randomUUID().toString();
  private IPentahoSession session = mock( IPentahoSession.class );

  @Test
  public void testComplete() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ThruStagingHandler handler = new ThruStagingHandler( baos, session );

    handler.getStagingOutputStream().write( TEST.getBytes() );

    handler.complete();

    // trim string from tail 00 bytes...
    assertEquals( "Write output only after complete", TEST, baos.toString().trim() );
  }

  @Test
  public void testCanSendHeaders() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ThruStagingHandler handler = new ThruStagingHandler( baos, session );

    assertTrue( handler.canSendHeaders() );

    handler.getStagingOutputStream().write( TEST.getBytes() );

    assertFalse( "Can't send headers after start writing to THRU", handler.canSendHeaders() );
  }

  @Test
  public void testFullyBuffered() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ThruStagingHandler handler = new ThruStagingHandler( baos, session );

    assertFalse( handler.isFullyBuffered() );
  }

  @Test
  public void testStagingMode() throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ThruStagingHandler handler = new ThruStagingHandler( baos, session );

    assertEquals( StagingMode.THRU, handler.getStagingMode() );
  }
}
