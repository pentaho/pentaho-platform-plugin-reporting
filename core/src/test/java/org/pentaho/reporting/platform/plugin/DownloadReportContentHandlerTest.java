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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

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
