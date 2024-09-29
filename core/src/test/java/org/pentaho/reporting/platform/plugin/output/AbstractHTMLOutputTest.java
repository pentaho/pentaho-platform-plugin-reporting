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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

public class AbstractHTMLOutputTest {

  private AbstractHtmlOutput htmlOutput;

  @Before public void setUp() {
    htmlOutput = new AbstractHtmlOutput( "test" ) {

    };
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
  }

  @Test
  public void testDefaults() throws ContentIOException, ReportProcessingException, IOException {
    assertEquals( htmlOutput.paginate( null, 1 ), 0 );
    assertEquals( htmlOutput.generate( null, 1, null, 1 ), 0 );
    assertFalse( htmlOutput.supportsPagination() );
    assertEquals( "test", htmlOutput.getContentHandlerPattern() );
  }

  @Test( expected = IllegalStateException.class )
  public void invalidConfig() {
    htmlOutput.createPentahoNameGenerator();
  }

  @Test
  public void testStream() throws ContentIOException, ReportProcessingException, IOException {
    ClassicEngineBoot.getInstance().start();
    final StreamHtmlOutput streamHtmlOutput = new StreamHtmlOutput();
    streamHtmlOutput.generate( new MasterReport(), 1, mock( OutputStream.class ), 1 );
  }

}
