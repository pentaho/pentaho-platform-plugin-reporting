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


package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.cache.IReportContentCache;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.mockito.Mockito.*;

public class AsyncSystemListenerTest {


  @Test
  public void startup() throws Exception {
    new AsyncSystemListener().startup( new StandaloneSession() );
  }

  @Test
  public void shutdown() throws Exception {

    MicroPlatform microPlatform = MicroPlatformFactory.create();
    final IPentahoAsyncExecutor asyncExecutor = mock( IPentahoAsyncExecutor.class );
    final IReportContentCache cache = mock( IReportContentCache.class );
    final AsyncSystemListener asyncSystemListener = new AsyncSystemListener();
    microPlatform.define( "IPentahoAsyncExecutor", asyncExecutor );
    microPlatform.define( "IReportContentCache", cache );
    microPlatform.addLifecycleListener( asyncSystemListener );
    microPlatform.start();
    microPlatform.stop();
    verify( asyncExecutor, times( 1 ) ).shutdown();
    verify( cache, times( 1 ) ).cleanup();
    microPlatform = null;
  }


  @Test
  public void dontFailIfNoBeans() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    final AsyncSystemListener asyncSystemListener = new AsyncSystemListener();
    microPlatform.addLifecycleListener( asyncSystemListener );
    microPlatform.start();
    microPlatform.stop();
    microPlatform = null;
  }

}
