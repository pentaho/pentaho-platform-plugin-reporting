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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.cache;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileSystemCacheBackendTest {

  private static FileSystemCacheBackend fileSystemCacheBackend;

  @BeforeClass
  public static void setUp() {
    PentahoSessionHolder.setSession( new StandaloneSession(  ) );
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
  }

  @AfterClass
  public static void tearDown() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
  }

  private static final String directoryKey = "id344324";
  private static final String key = "file1.html";
  private static final String value = "SerializableObject";

  @Test
  public void testWriteRead() throws Exception {
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value, new HashMap<String, Serializable>()) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
  }

  @Test
  public void testPurge() throws Exception {
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value, new HashMap<String, Serializable>() ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Arrays.asList( directoryKey, key ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }

  @Test
  public void testPurgeDir() throws Exception {
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value , new HashMap<String, Serializable>()) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( directoryKey ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }


  @Test
  public void testReadWhileWriting() throws Exception {
    final String val = "ShinySecretValue";
    final ExecutorService executor = Executors.newFixedThreadPool( 2 );
    final Future<Boolean> write = executor.submit( new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        return fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), new LongWrite( val ) , new HashMap<String, Serializable>());
      }
    } );
    final Future<LongWrite> read = executor.submit( new Callable<LongWrite>() {
      @Override public LongWrite call() throws Exception {
        Thread.sleep( 1000 );
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
    fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), new LongRead( val ) , new HashMap<String, Serializable>());
    final Future<LongRead> read = executor.submit( new Callable<LongRead>() {
      @Override public LongRead call() throws Exception {
        return (LongRead) fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) );
      }
    } );
    final Future<Boolean> write = executor.submit( new Callable<Boolean>() {
      @Override public Boolean call() throws Exception {
        return fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), new LongRead( val2 ), new HashMap<String, Serializable>() );
      }
    } );
    assertTrue( write.get() );
    assertEquals( read.get().getValue(), val );
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
        Thread.sleep( 3000L );
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
        Thread.sleep( 3000L );
        final Object o = ois.readObject();
        this.value = (String) o;
      } catch ( final InterruptedException e ) {
        e.printStackTrace();
      }

    }
  }
}
