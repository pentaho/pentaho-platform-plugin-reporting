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


package org.pentaho.reporting.platform.plugin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;

import java.io.IOException;
import java.util.UUID;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


@RunWith( MockitoJUnitRunner.class )
public class JobManagerContextTest {

  private IAsyncReportState state;
  private MasterReport report;
  private UUID uuid;
  private IPentahoSession session;

  @Before
  public void before() throws ResourceException, IOException {
    session = mock( IPentahoSession.class );
    uuid = UUID.randomUUID();
    final IPentahoAsyncExecutor executor = mock( IPentahoAsyncExecutor.class );
    state = mock( IAsyncReportState.class );
    report = mock( MasterReport.class );
    when( executor.getReportState( uuid, session ) ).thenReturn( state );
    PentahoSystem.registerObject( executor, IPentahoAsyncExecutor.class );
  }

  @After
  public void after() {
    state = null;
    report = null;
    uuid = null;
    session = null;
    PentahoSystem.clearObjectFactory();
  }


  @Test
  public void testNeedRecalculateFinished() throws ResourceException, IOException, JobManager.ContextFailedException {
    try ( MockedStatic<ReportCreator> reportCreatorMockedStatic = mockStatic( ReportCreator.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = mockStatic(
            PentahoSessionHolder.class )
    ) {
      reportCreatorMockedStatic.when( () -> ReportCreator.createReportByName( any() ) ).thenReturn( report );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession ).thenReturn( session );
      when( state.getStatus() ).thenReturn( AsyncExecutionStatus.FINISHED );
      final JobManager jobManager = new JobManager();
      final JobManager.ExecutionContext executionContext = jobManager.getContext( uuid.toString() );
      assertTrue( executionContext.needRecalculation( Boolean.TRUE ) );
    }
  }

  @Test
  public void testNoNeedRecalculateFinished() throws ResourceException, IOException, JobManager.ContextFailedException {
    try ( MockedStatic<ReportCreator> reportCreatorMockedStatic = Mockito.mockStatic( ReportCreator.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = Mockito.mockStatic(
            PentahoSessionHolder.class )
    ) {
      reportCreatorMockedStatic.when( () -> ReportCreator.createReportByName( any() ) ).thenReturn( report );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession ).thenReturn( session );
      when( state.getStatus() ).thenReturn( AsyncExecutionStatus.FINISHED );
      final JobManager jobManager = new JobManager();
      final JobManager.ExecutionContext executionContext = jobManager.getContext( uuid.toString() );
      assertFalse( executionContext.needRecalculation( Boolean.FALSE ) );
    }
  }

  @Test
  public void testNeedRecalculateReportLevelLimit()
    throws ResourceException, IOException, JobManager.ContextFailedException {
    try ( MockedStatic<ReportCreator> reportCreatorMockedStatic = mockStatic( ReportCreator.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = mockStatic(
            PentahoSessionHolder.class )
    ) {
      reportCreatorMockedStatic.when( () -> ReportCreator.createReportByName( any() ) ).thenReturn( report );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession ).thenReturn( session );
      when( state.getStatus() ).thenReturn( AsyncExecutionStatus.FINISHED );
      final JobManager jobManager = new JobManager();
      final JobManager.ExecutionContext executionContext = jobManager.getContext( uuid.toString() );
      when( report.getQueryLimit() ).thenReturn( 100 );
      assertTrue( executionContext.needRecalculation( Boolean.FALSE ) );
    }
  }


  @Test
  public void testNeedRecalculateReportLimitReached()
    throws ResourceException, IOException, JobManager.ContextFailedException {
    try ( MockedStatic<ReportCreator> reportCreatorMockedStatic = mockStatic( ReportCreator.class );
          MockedStatic<PentahoSessionHolder> pentahoSessionHolderMockedStatic = mockStatic(
            PentahoSessionHolder.class )
    ) {
      reportCreatorMockedStatic.when( () -> ReportCreator.createReportByName( any() ) ).thenReturn( report );
      pentahoSessionHolderMockedStatic.when( PentahoSessionHolder::getSession ).thenReturn( session );
      when( state.getStatus() ).thenReturn( AsyncExecutionStatus.FINISHED );
      when( state.getIsQueryLimitReached() ).thenReturn( true );
      final JobManager jobManager = new JobManager();
      final JobManager.ExecutionContext executionContext = jobManager.getContext( uuid.toString() );
      assertTrue( executionContext.needRecalculation( Boolean.FALSE ) );
    }
  }


}
