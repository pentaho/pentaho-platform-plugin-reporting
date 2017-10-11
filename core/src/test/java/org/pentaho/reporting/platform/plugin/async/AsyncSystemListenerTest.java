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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

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
