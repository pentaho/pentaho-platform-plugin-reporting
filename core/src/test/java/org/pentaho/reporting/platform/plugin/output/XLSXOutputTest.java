/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
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
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class XLSXOutputTest {
  XLSXOutput xlsxOutput;
  private IAsyncReportListener listener;

  @Before public void setUp() {
    xlsxOutput = new XLSXOutput();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testPaginate() throws Exception {
    Assert.assertEquals( 0, xlsxOutput.paginate( null, 0 ) );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( false, xlsxOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( xlsxOutput, xlsxOutput.getReportLock() );
  }

  @Test public void testSetTemplateData() throws Exception {
    Assert.assertNull( xlsxOutput.getTemplateData() );
    byte[] data = new byte[] { 5 };
    xlsxOutput.setTemplateData( data );
    Assert.assertEquals( data, xlsxOutput.getTemplateData() );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    xlsxOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }

  @Test
  public void testGenerate() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    xlsxOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }
}

