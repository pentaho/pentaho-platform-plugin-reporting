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


package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;

import java.io.OutputStream;

import static org.mockito.Mockito.mock;

public class DownloadReportContentHandlerTest extends TestCase {

  DownloadReportContentHandler handler;
  IPentahoSession session;
  IParameterProvider provider;

  @Before
  protected void setUp() {
    session = mock( IPentahoSession.class );
    provider = mock( IParameterProvider.class );
    handler = new DownloadReportContentHandler( session, provider );
  }

  @Test
  public void testConstructor() throws Exception {
    try {
      DownloadReportContentHandler handler = new DownloadReportContentHandler( null, null );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
    try {
      DownloadReportContentHandler handler =
          new DownloadReportContentHandler( mock( IPentahoSession.class ), null );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
  }
}
