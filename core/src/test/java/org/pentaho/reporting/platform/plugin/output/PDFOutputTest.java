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
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;


public class PDFOutputTest {
  PDFOutput pdfOutput;
  private IAsyncReportListener listener;

  YieldReportListener yieldReportListener;

  @Before public void setUp() {
    yieldReportListener = mock( YieldReportListener.class );
    pdfOutput = new PDFOutput() {
      @Override public YieldReportListener getYieldListener( final int yieldRate ) {
        return yieldReportListener;
      }
    };
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testPaginate() throws Exception {
    Assert.assertEquals( 0, pdfOutput.paginate( null, 0 ) );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( false, pdfOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( pdfOutput, pdfOutput.getReportLock() );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    pdfOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerate() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    pdfOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    verify( yieldReportListener, atLeast( 1 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerateYield0() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    try {
      pdfOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 0 );
      verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
      verifyNoInteractions( yieldReportListener );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }
}

