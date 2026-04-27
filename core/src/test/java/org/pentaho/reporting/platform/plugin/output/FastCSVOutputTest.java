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
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.TableDataFactory;
import org.pentaho.reporting.engine.classic.core.elementfactory.TextFieldElementFactory;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import javax.swing.table.DefaultTableModel;
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
    Assert.assertFalse( fastCSVOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( fastCSVOutput, fastCSVOutput.getReportLock() );
  }

  /**
   * Creates a MasterReport with inline static data so that the fast CSV processor
   * can generate content without requiring an external datasource.
   */
  private MasterReport createTestReport() {
    final MasterReport report = new MasterReport();

    final int rowCount = 100;
    final Object[][] data = new Object[rowCount][2];
    for ( int i = 0; i < rowCount; i++ ) {
      data[i] = new Object[] { "r" + i + "c1", "r" + i + "c2" };
    }
    final DefaultTableModel tableModel = new DefaultTableModel( data, new Object[] { "Column1", "Column2" } );
    report.setDataFactory( new TableDataFactory( "default", tableModel ) );
    report.setQuery( "default" );

    final TextFieldElementFactory tf1 = new TextFieldElementFactory();
    tf1.setFieldname( "Column1" );
    tf1.setMinimumWidth( 100f );
    tf1.setMinimumHeight( 20f );
    tf1.setX( 0f );
    tf1.setY( 0f );
    report.getItemBand().addElement( tf1.createElement() );

    final TextFieldElementFactory tf2 = new TextFieldElementFactory();
    tf2.setFieldname( "Column2" );
    tf2.setMinimumWidth( 100f );
    tf2.setMinimumHeight( 20f );
    tf2.setX( 100f );
    tf2.setY( 0f );
    report.getItemBand().addElement( tf2.createElement() );

    return report;
  }

  /**
   * Verifies that the fast CSV path notifies the listener of processing start, finish and query-limit status.
   * Note: {@code FastCsvExportProcessor} processes all rows in a single batch and intentionally does
   * <b>not</b> fire {@code reportProcessingUpdate} events — unlike the non-fast
   * {@code CSVReportProcessor} which reports row-level progress.
   */
  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    final MasterReport report = createTestReport();
    try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
      fastCSVOutput.generate( report, 1, baos, 1 );
    }

    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    // FastCsvExportProcessor processes all rows in one batch; no per-row update events are fired
    verify( listener, never() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).setIsQueryLimitReached( false );
  }

  @Test
  public void testGenerateFast() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    final MasterReport report = createTestReport();
    try ( ByteArrayOutputStream baos = new ByteArrayOutputStream() ) {
      fastCSVOutput.generate( report, 1, baos, 1 );
      assertTrue( baos.size() > 0 );
    }

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

