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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncReportStatusListenerTest {

  public static final String STUB =
    "AsyncReportStatusListener{path='', uuid=370c1ff4-06de-4b32-b966-f7bd8c2da7b2, status=QUEUED, progress=0, page=0,"
      + " totalPages=0, generatedPage=0, activity='null', row=0, firstPageMode=true, mimeType='', "
      + "errorMessage='370c1ff4-06de-4b32-b966-f7bd8c2da7b2'}";
  public static final String STUV_UUID = "370c1ff4-06de-4b32-b966-f7bd8c2da7b2";

  @Test
  public void isFirstPageMode() throws Exception {
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );
    assertTrue( listener.isFirstPageMode() );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "false" );
    final AsyncReportStatusListener listener2 =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );
    assertFalse( listener2.isFirstPageMode() );
  }

  @Test
  public void isScheduled() throws Exception {
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );
    assertFalse( listener.isScheduled() );
    listener.setStatus( AsyncExecutionStatus.SCHEDULED );
    assertTrue( listener.isScheduled() );
  }

  @Test
  public void reportProcessingFinished() throws Exception {
    final ReportProgressListener progressListener = mock( ReportProgressListener.class );
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "",
        Collections.singletonList( progressListener ) );
    final ReportProgressEvent event = mock( ReportProgressEvent.class );
    listener.reportProcessingFinished( event );
    verify( progressListener, times( 1 ) ).reportProcessingFinished( event );
    //listener should not update status to finished
    assertFalse( AsyncExecutionStatus.FINISHED.equals( listener.getState().getStatus() ) );
  }

  @Test
  public void setStatus() throws Exception {
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );
    listener.setStatus( null );
    listener.setStatus( AsyncExecutionStatus.QUEUED );
    assertEquals( AsyncExecutionStatus.QUEUED, listener.getState().getStatus() );
    listener.setStatus( AsyncExecutionStatus.CANCELED );
    assertEquals( AsyncExecutionStatus.CANCELED, listener.getState().getStatus() );
    listener.setStatus( AsyncExecutionStatus.FINISHED );
    assertEquals( AsyncExecutionStatus.CANCELED, listener.getState().getStatus() );

  }

  @Test
  public void updateGenerationStatus() throws Exception {
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );
    listener.setRequestedPage( 500 );
    listener.updateGenerationStatus( 500 );
    assertEquals( 0, listener.getRequestedPage() );
  }

  @Test
  public void testErrorMessage() {
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );

    final String errorMessage = UUID.randomUUID().toString();
    listener.setErrorMessage( errorMessage );
    assertEquals( errorMessage, listener.getState().getErrorMessage() );
  }


  @Test
  public void testAllActivities() {
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.randomUUID(), "", Collections.<ReportProgressListener>emptyList() );

    final Map<Integer, String> activities = new HashMap<>();
    activities.put( ReportProgressEvent.COMPUTING_LAYOUT, "AsyncComputingLayoutTitle" );
    activities.put( ReportProgressEvent.PRECOMPUTING_VALUES, "AsyncPrecomputingValuesTitle" );
    activities.put( ReportProgressEvent.PAGINATING, "AsyncPaginatingTitle" );
    activities.put( ReportProgressEvent.GENERATING_CONTENT, "AsyncGeneratingContentTitle" );
    activities.put( 100500, "" );
    for ( final Map.Entry<Integer, String> entry : activities.entrySet() ) {
      listener.reportProcessingUpdate(
        new ReportProgressEvent( this, entry.getKey(), 0, 0, 0, 0, 0, 0 ) );
      assertEquals( entry.getValue(), listener.getState().getActivity() );
    }
  }

  @Test
  public void testTostring() {
    final ModifiableConfiguration edConf = ClassicEngineBoot.getInstance().getEditableConfig();
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "true" );
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.fromString( STUV_UUID ), "",
        Collections.<ReportProgressListener>emptyList() );
    listener.setErrorMessage( STUV_UUID );
    assertEquals( STUB, listener.toString() );
    edConf.setConfigProperty( "org.pentaho.reporting.platform.plugin.output.FirstPageMode", "false" );
  }

  @Test
  public void testCallbacks() {
    final ReportProgressListener mock = mock( ReportProgressListener.class );
    final AsyncReportStatusListener listener =
      new AsyncReportStatusListener( "", UUID.fromString( STUV_UUID ), "",
        Collections.singletonList( mock ) );
    final ReportProgressEvent event = mock( ReportProgressEvent.class );
    listener.reportProcessingStarted( event );
    listener.reportProcessingUpdate( event );
    listener.reportProcessingFinished( event );
    verify( mock, times( 1 ) ).reportProcessingStarted( event );
    verify( mock, times( 1 ) ).reportProcessingUpdate( event );
    verify( mock, times( 1 ) ).reportProcessingFinished( event );
  }
}
