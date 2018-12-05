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
 * Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.util.UUIDUtil;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.File;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

public class StagingHandlerTest extends TestCase {
  OutputStream outputStream;
  StagingMode stagingMode;
  IPentahoSession session;
  IApplicationContext appContext;
  StagingHandler handler;

  private static String PREFIX = "repstg";
  private static String POSTFIX = ".tmp";

  @Before
  public void setUp() throws Exception {
    outputStream = mock( OutputStream.class );
    stagingMode = mock( StagingMode.class );
    session = mock( IPentahoSession.class );
    appContext = mock( IApplicationContext.class );
    doReturn( "" ).when( session ).getId();

    handler = new StagingHandler( outputStream, stagingMode, session );

    // mock solution temp folder path
    when( appContext.getSolutionPath( anyString() ) ).thenReturn( "target/test/resource/solution" );

    final File parentDir = new File( appContext.getSolutionPath( "system/tmp" ) ); //$NON-NLS-1$
    final ITempFileDeleter fileDeleter =
            (ITempFileDeleter) session.getAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE );
    final String newPrefix =
            new StringBuilder()
                    .append( "repstg" ).append( UUIDUtil.getUUIDAsString().substring( 0, 10 ) ).append( '-' ).toString(); //$NON-NLS-1$
    File tmpFile = File.createTempFile( newPrefix, ".tmp", parentDir ); //$NON-NLS-1$

    when( appContext.createTempFile( session, PREFIX, POSTFIX, true ) ).thenReturn( tmpFile );
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
