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
import org.pentaho.di.core.util.UUIDUtil;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.File;
import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
