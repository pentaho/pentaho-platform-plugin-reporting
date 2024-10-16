/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2024 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
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
