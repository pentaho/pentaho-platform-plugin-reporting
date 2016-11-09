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

package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.File;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class FastCSVOutputTest {
  FastCSVOutput fastCSVOutput;
  private IAsyncReportListener listener;

  @Before public void setUp() {
    fastCSVOutput = new FastCSVOutput();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testPaginate() throws Exception {
    Assert.assertEquals( 0, fastCSVOutput.paginate( null, 0 ) );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( false, fastCSVOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( fastCSVOutput, fastCSVOutput.getReportLock() );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    fastCSVOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerateFast() throws Exception {
    try {
      ClassicEngineBoot.getInstance().start();
      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.setSession( new StandaloneSession() );

      final File file = new File( "resource/solution/test/reporting/limit10.prpt" );
      final MasterReport report =
        (MasterReport) new ResourceManager().createDirectly( file.getPath(), MasterReport.class ).getResource();
      try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
        fastCSVOutput.generate( report, 1, baos, 1 );
        assertTrue( baos.size() > 0 );
      }

      verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    } finally {
      PentahoSessionHolder.removeSession();
    }
  }

  @Test
  public void testGenerateNotFast() throws Exception {
    try {
      ClassicEngineBoot.getInstance().start();
      ReportListenerThreadHolder.clear();
      PentahoSessionHolder.setSession( new StandaloneSession() );

      final File file = new File( "resource/solution/test/reporting/report.prpt" );
      final MasterReport report =
        (MasterReport) new ResourceManager().createDirectly( file.getPath(), MasterReport.class ).getResource();
      try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
        fastCSVOutput.generate( report, 1, baos, 1 );
        assertTrue( baos.size() > 0 );
      }

      verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    } finally {
      PentahoSessionHolder.removeSession();
    }
  }
}

