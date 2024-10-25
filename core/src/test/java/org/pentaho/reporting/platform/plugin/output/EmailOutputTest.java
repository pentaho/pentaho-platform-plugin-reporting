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
import org.junit.Ignore;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportListener;
import org.pentaho.reporting.engine.classic.core.event.async.ReportListenerThreadHolder;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;

import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


public class EmailOutputTest {
  EmailOutput emailOutput;
  private IAsyncReportListener listener;


  YieldReportListener yieldReportListener;

  @Before public void setUp() {
    yieldReportListener = mock( YieldReportListener.class );
    emailOutput = new EmailOutput() {
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
    assertEquals( 0, emailOutput.paginate( null, 0 ) );
  }

  @Test public void testSupportsPagination() throws Exception {
    assertEquals( false, emailOutput.supportsPagination() );
  }

  @Test public void testGetReportLock() throws Exception {
    assertEquals( emailOutput, emailOutput.getReportLock() );
  }

  @Ignore
  @Test
  public void testGenerateListener() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    ClassicEngineBoot.getInstance().start();
    try {
      emailOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

      verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }

  }

  @Ignore
  @Test
  public void testGenerate() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    try {
      emailOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 );

      verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
      verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
      verify( yieldReportListener, atLeast( 1 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }

  @Test
  public void testInlineStyles() throws PlatformInitializationException {
    final Map<Object, Object> configMap = new HashMap<>();
    final MasterReport report = mock( MasterReport.class );
    final ModifiableConfiguration configuration = mock( ModifiableConfiguration.class );

    doAnswer( i -> configMap.put( i.getArguments()[ 0 ], i.getArguments()[ 1 ] ) ).when( configuration )
      .setConfigProperty( any(), any() );

    when( report.getConfiguration() ).thenReturn( configuration );

    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    try {
      emailOutput.generate( report, 1, new ByteArrayOutputStream(), 1 );
    } catch ( final Exception ignored ) {
    } finally {
      assertEquals( configMap.get( HtmlTableModule.INLINE_STYLE ),
        "true" );
      assertEquals(
        configMap.get( HtmlTableModule.EXTERNALIZE_STYLE ), "false" );
      assertEquals(
        configMap.get( "org.pentaho.reporting.engine.classic.core.modules.output.table.html.RotatedTextAsImages" ),
        "true" );
      microPlatform.stop();
      microPlatform = null;
    }

  }


  @Test
  public void testGenerateNoContext() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    try {
      microPlatform.start();
      ClassicEngineBoot.getInstance().start();
      PentahoSystem.setApplicationContext( null );
      assertEquals( -1, emailOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 1 ) );
    } finally {
      microPlatform.stop();
      microPlatform = null;
    }
  }

  @Ignore
  @Test
  public void testGenerateYield0() throws Exception {
    MicroPlatform microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    try {
      emailOutput.generate( new MasterReport(), 1, new ByteArrayOutputStream(), 0 );
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

