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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class FastXLSOutputTest {
  FastXLSOutput fastXLSOutput;
  private IAsyncReportListener listener;


  @Before public void setUp() {
    fastXLSOutput = new FastXLSOutput();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testPaginate() throws Exception {
    Assert.assertEquals( 0, fastXLSOutput.paginate( null, 0 ) );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( false, fastXLSOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( fastXLSOutput, fastXLSOutput.getReportLock() );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    fastXLSOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerate() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    fastXLSOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
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
      fastXLSOutput.generate( report, 1, new ByteArrayOutputStream(), 1 );

      verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    } finally {
      PentahoSessionHolder.removeSession();
    }
  }
}

