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
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test Session cache
 */
public class PluginSessionCacheTest {

  private static final String SOME_KEY = "some_key";
  private static final IReportContent SOME_VALUE =
    new ReportContentImpl( 100, (Map<Integer, byte[]>) Collections.singletonMap(1, new byte[] { 1, 3, 4, 5 } ) );
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
    PentahoSessionHolder.setSession( new StandaloneSession( "test", "100500" ) );
    final IReportContentCache cache = new PluginSessionCache( fileSystemCacheBackend );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
  }

  @Test
  public void testEviction() throws Exception {
    final StandaloneSession session = new StandaloneSession( "test", "100500" );
    PentahoSessionHolder.setSession( session );

    final IReportContentCache cache = new PluginSessionCache( fileSystemCacheBackend );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );

    final StandaloneSession session1 = new StandaloneSession( "test1", "100501" );
    PentahoSessionHolder.setSession( session1 );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    PentahoSystem.invokeLogoutListeners( session );
    assertNotNull( cache.get( SOME_KEY ) );
    PentahoSessionHolder.setSession( session );
    assertNull( cache.get( SOME_KEY ) );
  }



  @Test
  public void testPutGetOrder() throws Exception {
    PentahoSessionHolder.setSession( new StandaloneSession( "test", "100500" ) );
    final IReportContentCache cache = new PluginSessionCache( fileSystemCacheBackend );
    final Map<Integer, byte[]> map = new HashMap<>(  );
    map.put( 0, new byte[] {1} );
    map.put( 1, new byte[] {1, 2} );
    cache.put( SOME_KEY,  new ReportContentImpl( 100, map) );
    final IReportContent iReportContent = cache.get( SOME_KEY );
    assertNotNull( iReportContent );
    assertTrue( iReportContent.getPageData( 0 ).length == 1 );
    assertTrue( iReportContent.getPageData( 1 ).length == 2 );
  }


}