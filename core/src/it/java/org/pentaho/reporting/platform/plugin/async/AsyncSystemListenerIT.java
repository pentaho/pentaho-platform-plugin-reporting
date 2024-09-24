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

package org.pentaho.reporting.platform.plugin.async;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.cache.DeleteOldOnAccessCache;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.IReportContent;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.reporting.platform.plugin.cache.ReportContentImpl;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith( Parameterized.class )
public class AsyncSystemListenerIT {

  private static final IReportContent SOME_VALUE =
    new ReportContentImpl( 100, (Map<Integer, byte[]>) Collections.singletonMap( 1, new byte[] { 1, 3, 4, 5 } ) );

  private static final FileSystemCacheBackend cacheBackend = new FileSystemCacheBackend();
  {
    cacheBackend.setCachePath( "/tmp/" );
  }


  public AsyncSystemListenerIT( final IReportContentCache cache,
                                final boolean isNull ) {
    this.cache = cache;
    this.isNull = isNull;
  }

  private IReportContentCache cache;
  private boolean isNull;


  @Parameterized.Parameters
  public static Collection primeNumbers() {
    final DeleteOldOnAccessCache deleteOldOnAccessCache = new DeleteOldOnAccessCache( cacheBackend );
    deleteOldOnAccessCache.setDaysToLive( 1 );
    return Arrays.asList( new Object[][] {
      { new PluginSessionCache( cacheBackend ), true },
      { deleteOldOnAccessCache, false }
    } );
  }

  @AfterClass
  public static void cleanup() {
    assertTrue( cacheBackend.purge( Collections.singletonList( "." ) ) );
  }

  @BeforeClass
  public static void setUp(){
    System.setProperty( "java.io.tmpdir", "target/test" );
  }


  /**
   * Session cache should purge all session files Delete old on access should not delete files
   *
   * @throws Exception
   */
  @Test
  public void lifecycle() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.define( "IPentahoAsyncExecutor", new PentahoAsyncExecutor( 10, 0 ) );


    microPlatform.define( "IReportContentCache",
      cache );
    final AsyncSystemListener asyncSystemListener = spy( new AsyncSystemListener() );
    microPlatform.addLifecycleListener( asyncSystemListener );
    microPlatform.start();
    PentahoSessionHolder.setSession( new StandaloneSession( "test" ) );
    verify( asyncSystemListener, times( 1 ) ).startup( any( IPentahoSession.class ) );
    cache.put( "test", SOME_VALUE );
    assertNotNull( cache.get( "test" ) );
    microPlatform.stop();
    microPlatform = null;

    assertEquals( cache.get( "test" ) == null, isNull );
  }


}
