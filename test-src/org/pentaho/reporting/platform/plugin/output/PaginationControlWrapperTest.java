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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.cache.ReportContentImpl;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PaginationControlWrapperTest {


  private static MicroPlatform microPlatform;

  @BeforeClass
  public static void setUp() throws IOException, PlatformInitializationException {
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }

  @AfterClass
  public static void tearDown() {
    microPlatform.stop();
    microPlatform = null;
  }

  @Test
  public void writeConcurrently() throws Exception {

    final Map<Integer, byte[]> pages = new HashMap<>();

    final byte[] bytes = new byte[ 1000 ];
    final byte b = 0;
    Arrays.fill( bytes, b );
    pages.put( 0, bytes );


    final CountDownLatch latch = new CountDownLatch( 2 );

    final ExecutorService executorService = Executors.newFixedThreadPool( 2 );


    final Future<byte[]> future1 = executorService.submit( new TestTask( latch, pages ) );
    final Future<byte[]> future2 = executorService.submit( new TestTask( latch, pages ) );
    latch.countDown();
    latch.countDown();

    final byte[] bytes1 = future1.get();
    final byte[] bytes2 = future2.get();

    assertTrue( Arrays.equals( bytes1, bytes2 ) );

  }

  private class TestTask implements Callable<byte[]> {

    private final CountDownLatch latch;
    private final Map<Integer, byte[]> pages;

    private TestTask( final CountDownLatch latch, final Map<Integer, byte[]> pages ) {
      this.latch = latch;
      this.pages = pages;
    }

    @Override public byte[] call() throws Exception {
      latch.await();
      try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
        PaginationControlWrapper.write( baos, new ReportContentImpl( 1, pages ) );
        return baos.toByteArray();
      }
    }
  }

  @Test
  public void embedCss() throws Exception {
    final String solutionPath = PentahoSystem.getApplicationContext().getSolutionPath( "system/tmp/test.css" );
    final File file = new File( solutionPath );
    try {
      if ( !file.exists() ) {
        file.createNewFile();
      }
      final Map<Integer, byte[]> pages = new HashMap<>();
      pages
        .put( 0, "<link type=\"text/css\" rel=\"stylesheet\" href=\"/pentaho/getImage?image=test.css\">".getBytes() );
      pages
        .put( 1, "<link type=\"text/css\" rel=\"stylesheet\" href=\"/pentaho/getImage?image=nofile.css\">".getBytes() );
      final String res;
      try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
        PaginationControlWrapper.write( baos, new ReportContentImpl( 1, pages ) );
        res = new String( baos.toByteArray(), "UTF-8" );
      }
      assertFalse( res.contains( "link" ) );
      assertTrue( res.contains( "style" ) );
    } finally {
      file.delete();
    }

  }
}
