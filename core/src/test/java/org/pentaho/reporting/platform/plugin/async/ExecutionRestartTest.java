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
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.ReportCreator;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith( MockitoJUnitRunner.class )
public class ExecutionRestartTest {

  @Test
  public void testFork() throws ResourceException, IOException {
    try ( MockedStatic<ReportCreator> reportCreatorMockedStatic = Mockito.mockStatic( ReportCreator.class ) ) {
      final MasterReport report = mock( MasterReport.class );
      reportCreatorMockedStatic.when( () -> ReportCreator.createReportByName( ArgumentMatchers.anyString() ) ).thenReturn( report );
      final AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );
      final SimpleReportingComponent component = mock( SimpleReportingComponent.class );
      final PentahoAsyncReportExecution old =
        new PentahoAsyncReportExecution( "junit-path", component, handler, mock( IPentahoSession.class ), "id",
          AuditWrapper.NULL );
      old.notifyTaskQueued( UUID.randomUUID(), Collections.singletonList( mock( IAsyncReportListener.class ) ) );

      new PentahoAsyncReportExecution( old, handler );
      verify( component, times( 1 ) ).setReport( report );
    }
  }


}
