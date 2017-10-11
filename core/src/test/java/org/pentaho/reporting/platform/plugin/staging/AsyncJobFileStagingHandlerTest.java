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
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.staging;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by dima.prokopenko@gmail.com on 2/25/2016.
 */
public class AsyncJobFileStagingHandlerTest {

  private IPentahoSession session;
  private String SESSION_ID = "junit_id";
  private Random rand = new Random();
  private static String parentDir = System.getProperty( "java.io.tmpdir" );

  private static final byte[] data = new byte[524288];

  @BeforeClass public static void beforeClass() throws IOException {
    assertNotNull( parentDir );
    // some clean up necessary
    Path temp = Paths.get( parentDir ).resolve( AsyncJobFileStagingHandler.STAGING_DIR_ATTR );
    if ( temp.toFile().exists() ) {
      FileUtils.deleteDirectory( temp.toFile() );
    }

    // temp staging directory is determined according to app context.
    IApplicationContext context = mock( IApplicationContext.class );
    when( context.getSolutionPath( anyString() ) ).thenReturn( parentDir );
    PentahoSystem.setApplicationContext( context );
  }

  @Before public void before() {
    session = mock( IPentahoSession.class );
    when( session.getId() ).thenReturn( SESSION_ID );
    rand.nextBytes( data );

    AsyncJobFileStagingHandler.cleanStagingDir();
  }


  @Test public void testWriteToCorrectDestinationTest() throws IOException {
    AsyncJobFileStagingHandler handler = new AsyncJobFileStagingHandler( session );

    // -  tempDir/asyncstaging/session_id
    Path
        tempDir =
        Paths.get( parentDir ).resolve( AsyncJobFileStagingHandler.STAGING_DIR_ATTR ).resolve( session.getId() );

    assertTrue( "Temp staging dir is created. (Or existed used).", tempDir.toFile().exists() );
    assertTrue( "Temp staging dir is directory.", tempDir.toFile().isDirectory() );

    // also created one empty file ready to write to
    File[] fileList = tempDir.toFile().listFiles();

    // this is temp file for report
    File tempFile = fileList[0];
    assertEquals( "Temp file created immediately and 0 size", 0, tempFile.length() );

    // simulate async file write between requests
    OutputStream out = handler.getStagingOutputStream();
    IOUtils.copy( new ByteArrayInputStream( data, 0, data.length ), out );

    out.close();

    assertEquals( "After async execution all data written to this file.", data.length, tempFile.length() );

    // this simulate when result is response output stream
    // so we just copy data from staged file into response stream
    IFixedSizeStreamingContent input = handler.getStagingContent();
    InputStream in = input.getStream();

    byte[] result = IOUtils.toByteArray( in );

    IOUtils.closeQuietly( in );

    assertArrayEquals( "byte wise read an writes", data, result );

    assertTrue( "File not get deleted after input stream get closed", tempFile.exists() );
    assertTrue( "temp dir is not deleted", tempDir.toFile().exists() );

    assertTrue( input.cleanContent() );

    assertFalse( "File got deleted explicitly", tempFile.exists() );
  }

  @Test public void testStagingDirNotGetDeletedBetweenExecutions() throws Exception {
    CountDownLatch startSignal = new CountDownLatch( 0 );

    int count = 30;
    ExecutorService service = Executors.newFixedThreadPool( count );

    List<AsyncJobFileStagingHandler> handlers = new ArrayList<>();

    for ( int i = 0 ; i < count; i++ ) {
      AsyncJobFileStagingHandler handler = new AsyncJobFileStagingHandler( session );
      handlers.add( handler );
      service.submit( new AsyncStagingReadWrite( startSignal, handler ) );
    }

    startSignal.countDown();
    service.shutdown();
    service.awaitTermination( 5, TimeUnit.SECONDS );

    Path stagingDir = AsyncJobFileStagingHandler.getStagingDirPath();
    File[] fileList = stagingDir.toFile().listFiles();

    File sessionFolder = fileList[0];
    assertTrue( sessionFolder.isDirectory() );

    assertEquals( "Folder is named by session id", session.getId(), sessionFolder.getName() );

    assertEquals( "Folder is NOT empty after a BACKLOG-7598 fix", 30, sessionFolder.list().length );

    for ( AsyncJobFileStagingHandler handler : handlers ) {
      assertTrue( handler.getStagingContent().cleanContent() );
    }

    assertEquals("Staging folder empty now", 0, sessionFolder.list().length );
  }

  static class AsyncStagingReadWrite implements Runnable {

    private final CountDownLatch startSignal;
    private final AsyncJobFileStagingHandler handler;

    AsyncStagingReadWrite( CountDownLatch startSignal, AsyncJobFileStagingHandler handler ) {
      this.startSignal = startSignal;
      this.handler = handler;
    }

    /**
     * Leak of Input/Output stream will prevent test
     * from success results since as far as streams is
     * open file can't be deleted successfully.
     *
     */
    @Override public void run() {
      OutputStream out = null;
      InputStream in = null;

      try {
        startSignal.await();
        // write to
        out = handler.getStagingOutputStream();
        IOUtils.copy( new ByteArrayInputStream( data, 0, data.length ), out );
        out.flush();
        out.close();

        // read from
        IFixedSizeStreamingContent input = handler.getStagingContent();
        in = input.getStream();
        byte[] result = IOUtils.toByteArray( in );

        assertArrayEquals( "byte wise read an writes", data, result );

      } catch ( Exception e ) {
        fail( "Unexpected exception: " + e.getClass().getName() );
      } finally {
        IOUtils.closeQuietly( out );
        IOUtils.closeQuietly( in );
      }
    }
  }

  @AfterClass public static void afterClass() {
    AsyncJobFileStagingHandler.cleanStagingDir();
    PentahoSystem.shutdown();
  }
}
