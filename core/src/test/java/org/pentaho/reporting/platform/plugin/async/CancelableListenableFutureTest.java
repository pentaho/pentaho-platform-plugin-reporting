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

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( Parameterized.class )
public class CancelableListenableFutureTest {

  private Boolean interruptable;
  private AsyncExecutionStatus target;

  public CancelableListenableFutureTest( final Boolean interruptable,
                                         final AsyncExecutionStatus target ) {
    this.interruptable = interruptable;
    this.target = target;
  }

  @Parameterized.Parameters
  public static Collection primeNumbers() {
    return Arrays.asList( new Object[][] {
      { true, AsyncExecutionStatus.CANCELED },
      { false, AsyncExecutionStatus.QUEUED }
    } );
  }

  @Test
  public void cancel() throws Exception {
    final SimpleReportingComponent reportingComponent = mock( SimpleReportingComponent.class );
    when( reportingComponent.getMimeType() ).thenReturn( "text/html" );
    final PentahoAsyncReportExecution pentahoAsyncReportExecution =
      new PentahoAsyncReportExecution( "some url", reportingComponent, mock( AsyncJobFileStagingHandler.class ), mock(
        IPentahoSession.class ), "not null", AuditWrapper.NULL );
    final ListenableFuture mock = mock( ListenableFuture.class );
    final ListenableFuture delegate = pentahoAsyncReportExecution.delegate( mock );
    assertTrue( delegate instanceof AbstractAsyncReportExecution.CancelableListenableFuture );
    pentahoAsyncReportExecution.notifyTaskQueued( UUID.randomUUID(), Collections.<ReportProgressListener>emptyList() );
    final AsyncReportStatusListener listener = pentahoAsyncReportExecution.getListener();
    assertEquals( AsyncExecutionStatus.QUEUED, listener.getState().getStatus() );
    delegate.cancel( interruptable );
    assertEquals( target, listener.getState().getStatus() );
  }

}
