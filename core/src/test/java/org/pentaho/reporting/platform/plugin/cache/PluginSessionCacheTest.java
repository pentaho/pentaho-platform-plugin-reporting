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

import static org.junit.Assert.*;

/**
 * Test Session cache
 */
public class PluginSessionCacheTest {

  private static final String SOME_KEY = "some_key";
  private static final IReportContent SOME_VALUE =
    new ReportContentImpl( 100, (Map<Integer, byte[]>) Collections.singletonMap( 1, new byte[] { 1, 3, 4, 5 } ) );
  private static FileSystemCacheBackend fileSystemCacheBackend;

  @BeforeClass
  public static void setUp() {
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
  }

  @AfterClass
  public static void tearDown() {
    Assert.assertTrue( fileSystemCacheBackend.purge( Collections.singletonList( "" ) ) );
    PentahoSessionHolder.removeSession();
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
    final Map<Integer, byte[]> map = new HashMap<>();
    map.put( 0, new byte[] { 1 } );
    map.put( 1, new byte[] { 1, 2 } );
    cache.put( SOME_KEY, new ReportContentImpl( 100, map ) );
    final IReportContent iReportContent = cache.get( SOME_KEY );
    assertNotNull( iReportContent );
    assertTrue( iReportContent.getPageData( 0 ).length == 1 );
    assertTrue( iReportContent.getPageData( 1 ).length == 2 );
  }


  @Test
  public void testCleanup() throws Exception {
    final StandaloneSession session = new StandaloneSession( "test", "100500" );
    PentahoSessionHolder.setSession( session );

    final IReportContentCache cache = new PluginSessionCache( fileSystemCacheBackend );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    cache.cleanup();
    assertNull( cache.get( SOME_KEY ) );
  }


}
