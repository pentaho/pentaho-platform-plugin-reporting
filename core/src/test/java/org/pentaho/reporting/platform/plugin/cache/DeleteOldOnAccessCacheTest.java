/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.cache;

import junit.framework.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class DeleteOldOnAccessCacheTest {

  private static final String SOME_KEY = "some_key";
  private static final IReportContent SOME_VALUE =
    new ReportContentImpl( 100, Collections.singletonMap( 1, new byte[] { 1, 3, 4, 5 } ) );
  private static FileSystemCacheBackend fileSystemCacheBackend;

  @BeforeClass
  public static void setUp() {
    PentahoSessionHolder.setSession( new StandaloneSession() );
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
    cache.setDaysToLive( 1L );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    cache.setMillisToLive( 0 );
    Thread.sleep( 10 );
    assertNull( cache.get( SOME_KEY ) );
  }

  @Test
  public void testCleanup() throws Exception {
    final DeleteOldOnAccessCache cache = new DeleteOldOnAccessCache( fileSystemCacheBackend );
    cache.setDaysToLive( 1L );
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    cache.setMillisToLive( 0 );
    cache.cleanup();
    Thread.sleep( 10 );
    assertNull( cache.get( SOME_KEY ) );
  }


  @Test
  public void testCleanupCurrentSession() throws Exception {
    //We have two users
    final StandaloneSession bill = new StandaloneSession( "bill" );
    final StandaloneSession steve = new StandaloneSession( "steve" );
    PentahoSessionHolder.setSession( bill );
    final DeleteOldOnAccessCache cache = new DeleteOldOnAccessCache( fileSystemCacheBackend );
    cache.setDaysToLive( 1L );
    //The first one creates cache
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    PentahoSessionHolder.setSession( steve );
    //The second one doesn't have cache
    assertNull( cache.get( SOME_KEY ) );
    //The second one creates cache
    cache.put( SOME_KEY, SOME_VALUE );
    assertNotNull( cache.get( SOME_KEY ) );
    //The second one cleans cache
    cache.cleanupCurrentSession();
    assertNull( cache.get( SOME_KEY ) );
    //The first one still has his data
    PentahoSessionHolder.setSession( bill );
    assertNotNull( cache.get( SOME_KEY ) );
    //The first one cleans cache
    cache.cleanupCurrentSession();
    assertNull( cache.get( SOME_KEY ) );
  }
}
