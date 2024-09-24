/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

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
