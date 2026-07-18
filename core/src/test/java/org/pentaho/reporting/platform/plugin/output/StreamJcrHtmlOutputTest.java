/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.reporting.platform.plugin.output;

import junit.framework.TestCase;

public class StreamJcrHtmlOutputTest extends TestCase {
  StreamJcrHtmlOutput streamJcrHtmlOutput;

  protected void setUp() {
    streamJcrHtmlOutput = new StreamJcrHtmlOutput();
  }

  public void testSetJcrOutputPath() throws Exception {
    assertNull( streamJcrHtmlOutput.getJcrOutputPath() );

    streamJcrHtmlOutput.setJcrOutputPath( "path" ); //$NON-NLS-1$
    assertEquals( "path", streamJcrHtmlOutput.getJcrOutputPath() ); //$NON-NLS-1$
  }
}
