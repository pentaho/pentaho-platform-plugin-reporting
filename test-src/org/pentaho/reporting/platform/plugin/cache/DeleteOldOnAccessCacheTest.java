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

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class DeleteOldOnAccessCacheTest {

  private static final String SOME_KEY = "some_key";
  private static final IReportContent SOME_VALUE =
    new ReportContentImpl( 100, Collections.singletonMap(1, new byte[] { 1, 3, 4, 5 } ) );
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

  @Test
  public void testPutGet() throws Exception {

    final DeleteOldOnAccessCache cache = new DeleteOldOnAccessCache( fileSystemCacheBackend );
    cache.setMillisToLive( 1000 );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    Thread.sleep( 2000 );
    assertNull( cache.get( SOME_KEY ) );
  }

}