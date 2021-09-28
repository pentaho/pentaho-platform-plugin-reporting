/*
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
 * Copyright (c) 2002-2021 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.cache;

import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.powermock.reflect.Whitebox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class FileSystemCacheBackendTest {

  private static FileSystemCacheBackend fileSystemCacheBackend;

  @After
  public void clearLatchesAfterEachTest() {
    readStartedSignal = null;
    writeStartedSignal = null;
  }

  private static CountDownLatch readStartedSignal;
  private static CountDownLatch writeStartedSignal;

  @BeforeClass
  public static void setUp() {
    PentahoSessionHolder.setSession( new StandaloneSession() );
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
  }

  @AfterClass
  public static void tearDown() {
    assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
  }

  private static final String directoryKey = "id344324";
  private static final String key = "file1.html";
  private static final String value = "SerializableObject";

  @Test
  public void testWriteRead() throws Exception {
    assertTrue(
      fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value, new HashMap<String, Serializable>() ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
  }

  @Test
  public void testPurge() throws Exception {
    assertTrue(
      fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value, new HashMap<String, Serializable>() ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Arrays.asList( directoryKey, key ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }

  @Test
  public void testPurgeDir() throws Exception {
    assertTrue(
      fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value, new HashMap<String, Serializable>() ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( directoryKey ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }

  @Test
  public void testPurgeSyncMapCleaning() {
    List<String> randomKey = Arrays.asList( UUID.randomUUID().toString() );
    Map<List<String>, ReentrantReadWriteLock> syncMap = new HashMap<List<String>, ReentrantReadWriteLock>();
    Whitebox.setInternalState( fileSystemCacheBackend, "syncMap", syncMap );
    fileSystemCacheBackend.purge( randomKey );
    assertFalse( ((Map<List<String>, ReentrantReadWriteLock>) Whitebox.getInternalState( fileSystemCacheBackend, "syncMap" )).containsKey( randomKey ) );
  }

  @Test
  public void testReadWhileWriting() throws Exception {

    readStartedSignal = new CountDownLatch( 1 );
    writeStartedSignal = new CountDownLatch( 1 );

    final String val = "ShinySecretValue";
    final ExecutorService executor = Executors.newFixedThreadPool( 2 );
    final Future<Boolean> write = executor.submit( new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        return fileSystemCacheBackend
          .write( Arrays.asList( directoryKey, key ), new LongWrite( val ),
            new HashMap<String, Serializable>() );
      }
    } );

    final Future<LongWrite> read = executor.submit( new Callable<LongWrite>() {
      @Override public LongWrite call() throws Exception {
        writeStartedSignal.await();
        // unblock the writer ...
        readStartedSignal.countDown();
        return (LongWrite) fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) );
      }
    } );
    assertTrue( write.get() );
    assertEquals( read.get().getValue(), val );
  }


  @Test
  public void testWriteWhileReading() throws Exception {
    final String val = "ShinySecretValue";
    final String val2 = "ShinySecretValue2";
    final ExecutorService executor = Executors.newFixedThreadPool( 2 );
    readStartedSignal = null;
    writeStartedSignal = null;
    // setup writes without any signals ...
    fileSystemCacheBackend
      .write( Arrays.asList( directoryKey, key ), new LongRead( val ), new HashMap<String, Serializable>() );
    // now we can setup the signals ..
    readStartedSignal = new CountDownLatch( 1 );
    writeStartedSignal = new CountDownLatch( 1 );

    final Future<LongRead> read = executor.submit( new Callable<LongRead>() {
      @Override public LongRead call() throws Exception {
        return (LongRead) fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) );
      }
    } );
    final Future<Boolean> write = executor.submit( new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        readStartedSignal.await();
        // unblock the reader ...
        writeStartedSignal.countDown();
        return fileSystemCacheBackend
          .write( Arrays.asList( directoryKey, key ), new LongRead( val2 ), new HashMap<String, Serializable>() );
      }
    } );
    assertTrue( write.get() );
    assertEquals( read.get().getValue(), val );
  }

  @Test
  public void testSystemTmp() {
    final String property = System.getProperty( "java.io.tmpdir" );
    final char c = property.charAt( property.length() - 1 );
    if ( ( c == '/' ) || ( c == '\\' ) ) {
      System.setProperty( "java.io.tmpdir", property.substring( 0, property.length() - 1 ) ); //$NON-NLS-1$//$NON-NLS-2$
    }
    final String systemTmp = fileSystemCacheBackend.getSystemTmp();
    Assert.assertEquals( property, systemTmp );
  }

  @Test
  public void testCorners() throws Exception {
    Assert.assertNull( fileSystemCacheBackend.read( null ) );
    Assert.assertTrue( fileSystemCacheBackend.purge( null ) );
    Assert.assertTrue( fileSystemCacheBackend.write( null, "someval", new HashMap<String, Serializable>() ) );
    Assert.assertEquals( "someval", fileSystemCacheBackend.read( null ) );
    Assert.assertTrue( fileSystemCacheBackend.write( null, null, new HashMap<String, Serializable>() ) );
    Assert.assertNull( fileSystemCacheBackend.read( null ) );
    Assert.assertTrue( fileSystemCacheBackend.write( null, null, null ) );
    Assert.assertFalse( fileSystemCacheBackend.write( null, new FailWrite(), new HashMap<String, Serializable>() {
      {
        put( "fail", new FailWrite() );
      }
    } ) );

    Assert.assertTrue( fileSystemCacheBackend.write( null, new FailRead(), new HashMap<String, Serializable>() {
      {
        put( "fail", new FailRead() );
      }
    } ) );

    Assert.assertNull( fileSystemCacheBackend.read( null ) );
  }

  private class LongWrite implements Serializable {

    private String value;

    public LongWrite( final String value ) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    private void writeObject( final ObjectOutputStream oos ) throws IOException {
      try {
        if ( writeStartedSignal != null ) {
          // unlock the reader ...
          writeStartedSignal.countDown();
        }
        if ( readStartedSignal != null ) {
          // wait for the reader to start working ..
          readStartedSignal.await();
        }
        // now that small time frame should be enough, as we guarantee that the writer is
        // ready to enter the file-system write method as soon as it sees the latch go.
        Thread.sleep( 100 );
        oos.writeObject( value );
      } catch ( final InterruptedException e ) {
        e.printStackTrace();
      }

    }

    private void readObject( final ObjectInputStream ois ) throws ClassNotFoundException, IOException {
      final Object o = ois.readObject();
      this.value = (String) o;
    }
  }

  private class LongRead implements Serializable {

    private String value;

    public LongRead( final String value ) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    private void writeObject( final ObjectOutputStream oos ) throws IOException {
      oos.writeObject( value );
    }

    private void readObject( final ObjectInputStream ois ) throws ClassNotFoundException, IOException {
      try {
        if ( readStartedSignal != null ) {
          // unlock the writer ...
          readStartedSignal.countDown();
        }
        if ( writeStartedSignal != null ) {
          // wait for the writer to start working ..
          writeStartedSignal.await();
        }
        // now that small time frame should be enough, as we guarantee that the writer is
        // ready to enter the file-system write method as soon as it sees the latch go.
        Thread.sleep( 100 );
        final Object o = ois.readObject();
        this.value = (String) o;
      } catch ( final InterruptedException e ) {
        e.printStackTrace();
      }

    }
  }

  private class FailWrite implements Serializable {
    private void writeObject( final ObjectOutputStream oos ) throws IOException {
      throw new IOException( "C’est la vie" );
    }
  }

  private class FailRead implements Serializable {

    String in = "in";

    private void readObject( final ObjectOutputStream oos ) throws IOException {
      throw new IOException( "C’est la vie" );
    }

    private void writeObject( final ObjectOutputStream oos ) throws IOException {
      oos.writeObject( in );
    }
  }
}
