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

package org.pentaho.reporting.platform.plugin.output;

import junit.framework.TestCase;
import org.mockito.Mockito;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import java.io.OutputStream;

public class PageableHTMLOutputTest extends TestCase {
  PageableHTMLOutput pageableHTMLOutput;

  protected void setUp() {
    pageableHTMLOutput = new PageableHTMLOutput();
  }

  public void testGetReportLock() throws Exception {
    assertEquals( pageableHTMLOutput, pageableHTMLOutput.getReportLock() );
  }

  public void testSetContentHandlerPattern() throws Exception {
    assertNull( pageableHTMLOutput.getContentHandlerPattern() );
    pageableHTMLOutput.setContentHandlerPattern( "pattern" ); //$NON-NLS-1$
    assertEquals( "pattern", pageableHTMLOutput.getContentHandlerPattern() ); //$NON-NLS-1$
  }

  public void testSetProxyOutputStream() throws Exception {
    assertNull( pageableHTMLOutput.getProxyOutputStream() );
    ProxyOutputStream mockStream = Mockito.mock( ProxyOutputStream.class );
    pageableHTMLOutput.setProxyOutputStream( mockStream );
    assertEquals( mockStream, pageableHTMLOutput.getProxyOutputStream() );
  }

  public void testSetPrinter() throws Exception {
    assertNull( pageableHTMLOutput.getPrinter() );
    AllItemsHtmlPrinter mockPrinter = Mockito.mock( AllItemsHtmlPrinter.class );
    pageableHTMLOutput.setPrinter( mockPrinter );
    assertEquals( mockPrinter, pageableHTMLOutput.getPrinter() );
  }

  public void testSetReportProcessor() throws Exception {
    assertNull( pageableHTMLOutput.getReportProcessor() );
    PageableReportProcessor mockProcessor = Mockito.mock( PageableReportProcessor.class );
    pageableHTMLOutput.setReportProcessor( mockProcessor );
    assertEquals( mockProcessor, pageableHTMLOutput.getReportProcessor() );
  }

  public void testSupportsPagination() throws Exception {
    assertEquals( true, pageableHTMLOutput.supportsPagination() );
  }
}
