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
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.ReportCreator;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith( PowerMockRunner.class )
@PrepareForTest( ReportCreator.class )
@PowerMockIgnore( { "javax.swing.*", "jdk.internal.reflect.*" } )
public class ExecutionRestartTest {

  @Test
  public void testFork() throws ResourceException, IOException {
    PowerMockito.mockStatic( ReportCreator.class );
    final MasterReport report = mock( MasterReport.class );
    when( ReportCreator.createReportByName( anyString() ) ).thenReturn( report );
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
