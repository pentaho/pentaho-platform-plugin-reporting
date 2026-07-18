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

public class PNGOutputTest extends TestCase {
  PNGOutput pngOutput;

  protected void setUp() {
    pngOutput = new PNGOutput();
  }

  public void testPaginate() throws Exception {
    assertEquals( 0, pngOutput.paginate( null, 0 ) );
  }

  public void testSupportsPagination() throws Exception {
    assertEquals( false, pngOutput.supportsPagination() );
  }

  public void testGetReportLock() throws Exception {
    assertEquals( pngOutput, pngOutput.getReportLock() );
  }
}

