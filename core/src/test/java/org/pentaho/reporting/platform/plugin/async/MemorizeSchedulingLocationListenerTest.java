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
