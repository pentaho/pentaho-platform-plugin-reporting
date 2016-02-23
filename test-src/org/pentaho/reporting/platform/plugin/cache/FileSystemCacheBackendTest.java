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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FileSystemCacheBackendTest {

  private static FileSystemCacheBackend fileSystemCacheBackend;


  @BeforeClass
  public static void setUp() {
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
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
  }

  @Test
  public void testPurge() throws Exception {
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Arrays.asList( directoryKey, key ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }

  @Test
  public void testPurgeDir() throws Exception {
    assertTrue( fileSystemCacheBackend.write( Arrays.asList( directoryKey, key ), value ) );
    assertEquals( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ), value );
    assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( directoryKey ) ) );
    assertNull( fileSystemCacheBackend.read( Arrays.asList( directoryKey, key ) ) );
  }

  @Test
  public void testlistKeys() throws Exception {
    final Set<String> resultSet = new HashSet<String>();

    for ( int i = 0; i < 10; i++ ) {
      final String filename = "file" + i + ".html";
      fileSystemCacheBackend.write( Arrays.asList( directoryKey, filename ), value );
      resultSet.add( filename );
    }

    assertEquals( resultSet, fileSystemCacheBackend.listKeys( Collections.singletonList( directoryKey ) ) );
  }


}