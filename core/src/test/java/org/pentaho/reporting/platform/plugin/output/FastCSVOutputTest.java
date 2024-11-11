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
import static org.mockito.ArgumentMatchers.any;
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

      final File file = new File( "target/test/resource/solution/test/reporting/limit10.prpt" );
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

      final File file = new File( "target/test/resource/solution/test/reporting/report.prpt" );
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

