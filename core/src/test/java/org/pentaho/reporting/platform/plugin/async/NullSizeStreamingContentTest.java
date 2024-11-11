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


package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NullSizeStreamingContentTest {
  @Test
  public void getStream() throws Exception {
    assertTrue( new AbstractAsyncReportExecution.NullSizeStreamingContent().getStream() instanceof NullInputStream );
  }

  @Test
  public void getContentSize() throws Exception {
    assertEquals( 0L, new AbstractAsyncReportExecution.NullSizeStreamingContent().getContentSize() );
  }

  @Test
  public void clean() throws Exception {
    assertTrue( new AbstractAsyncReportExecution.NullSizeStreamingContent().cleanContent() );
  }

}
