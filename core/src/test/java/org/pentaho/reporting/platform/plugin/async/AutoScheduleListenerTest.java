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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.mockito.Mockito.*;

@RunWith( Parameterized.class )
public class AutoScheduleListenerTest {

  public AutoScheduleListenerTest( final int threshold,
                                   final ReportProgressEvent event, final int times ) {
    this.threshold = threshold;
    this.event = event;
    this.times = times;
  }

  private int threshold;
  private ReportProgressEvent event;
  private int times;

  @Parameterized.Parameters
  public static Collection params() {
    final ReportProgressEvent less = mock( ReportProgressEvent.class );
    final ReportProgressEvent more = mock( ReportProgressEvent.class );
    when( less.getMaximumRow() ).thenReturn( 0 );
    when( more.getMaximumRow() ).thenReturn( Integer.MAX_VALUE );
    return Arrays.asList( new Object[][] {
      { 0, less, 0 },
      { 0, more, 0 },
      { 0, null, 0 },
      { 1, less, 0 },
      { 1, more, 1 },
      { 1, null, 0 }
    } );

  }

  @Test
  public void testAutoSchedule() {

    final UUID id = UUID.randomUUID();
    final IPentahoSession session = mock( IPentahoSession.class );
    final IPentahoAsyncExecutor executor = mock( IPentahoAsyncExecutor.class );
    final AutoScheduleListener listener = new AutoScheduleListener( id, session, threshold, executor );
    listener.reportProcessingStarted( event );
    listener.reportProcessingUpdate( event );
    listener.reportProcessingFinished( event );
    verify( executor, times( times ) ).preSchedule( id, session );

  }


}

