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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemorizeSchedulingLocationListenerTest {

  @Test
  public void onSchedulingCompleted() throws Exception {
    final MemorizeSchedulingLocationListener memorizeSchedulingLocationListener =
      new MemorizeSchedulingLocationListener();
    final PentahoAsyncExecutor.CompositeKey mock = mock( PentahoAsyncExecutor.CompositeKey.class );
    final String path = "test.prpt";
    memorizeSchedulingLocationListener.recordOutputFile( mock, path );
    assertEquals( path, memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
  }

  @Test
  public void testOnLogout() throws Exception {
    final MemorizeSchedulingLocationListener memorizeSchedulingLocationListener =
      new MemorizeSchedulingLocationListener();
    final PentahoAsyncExecutor.CompositeKey mock = mock( PentahoAsyncExecutor.CompositeKey.class );
    when( mock.isSameSession( "another" ) ).thenReturn( false );
    when( mock.isSameSession( "same" ) ).thenReturn( true );
    final String path = "test.prpt";
    memorizeSchedulingLocationListener.recordOutputFile( mock, path );
    assertEquals( path, memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
    memorizeSchedulingLocationListener.onLogout( "another" );
    assertEquals( path, memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
    memorizeSchedulingLocationListener.onLogout( "same" );
    assertNull( memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
  }

  @Test
  public void testShutdown() throws Exception {
    final MemorizeSchedulingLocationListener memorizeSchedulingLocationListener =
      new MemorizeSchedulingLocationListener();
    final PentahoAsyncExecutor.CompositeKey mock = mock( PentahoAsyncExecutor.CompositeKey.class );
    final String path = "test.prpt";
    memorizeSchedulingLocationListener.recordOutputFile( mock, path );
    assertEquals( path, memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
    memorizeSchedulingLocationListener.shutdown();
    assertNull( memorizeSchedulingLocationListener.lookupOutputFile( mock ) );
  }
}
