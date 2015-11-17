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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class StagingHandlerTest extends TestCase {
  OutputStream outputStream;
  StagingMode stagingMode;
  IPentahoSession session;
  StagingHandler handler;

  @Before
  public void setUp() throws Exception {
    outputStream = mock( OutputStream.class );
    stagingMode = mock( StagingMode.class );
    session = mock( IPentahoSession.class );
    doReturn( "" ).when( session ).getId();

    handler = new StagingHandler( outputStream, stagingMode, session );
  }

  @Test
  public void testGetStagingMode() throws Exception {
    assertEquals( stagingMode, handler.getStagingMode() );
  }

  @Test
  public void testIsFullyBuffered() throws Exception {
    assertTrue( handler.isFullyBuffered() );
  }

  @Test
  public void testCanSendHeaders() throws Exception {
    assertTrue( handler.canSendHeaders() );
    OutputStream stream = handler.getStagingOutputStream();

    assertEquals( ( (TrackingOutputStream) stream ).getWrappedStream().hashCode(), outputStream.hashCode() );
    assertEquals( 0, handler.getWrittenByteCount() );
  }

  @Test
  public void testComplete() throws Exception {
    handler = new StagingHandler( outputStream, StagingMode.MEMORY, session );
    handler.complete();
    verify( outputStream, times( 1 ) ).write( any( byte[].class ), anyInt(), anyInt() );
    verify( outputStream, times( 1 ) ).flush();
  }
}
