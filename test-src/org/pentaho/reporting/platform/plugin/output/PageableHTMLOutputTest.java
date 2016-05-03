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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportListener;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;

import static org.mockito.Mockito.*;

public class PageableHTMLOutputTest {
  PageableHTMLOutput pageableHTMLOutput;
  private IAsyncReportListener listener;

  @Before public void setUp() {
    pageableHTMLOutput = new PageableHTMLOutput();
    listener = mock( IAsyncReportListener.class );
    ReportListenerThreadHolder.setListener( listener );
  }

  @After public void tearDown() {
    ReportListenerThreadHolder.clear();
    listener = null;
  }

  @Test public void testGetReportLock() throws Exception {
    Assert.assertEquals( pageableHTMLOutput, pageableHTMLOutput.getReportLock() );
  }

  @Test public void testSetContentHandlerPattern() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getContentHandlerPattern() );
    pageableHTMLOutput.setContentHandlerPattern( "pattern" ); //$NON-NLS-1$
    Assert.assertEquals( "pattern", pageableHTMLOutput.getContentHandlerPattern() ); //$NON-NLS-1$
  }

  @Test public void testSetProxyOutputStream() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getProxyOutputStream() );
    ProxyOutputStream mockStream = mock( ProxyOutputStream.class );
    pageableHTMLOutput.setProxyOutputStream( mockStream );
    Assert.assertEquals( mockStream, pageableHTMLOutput.getProxyOutputStream() );
  }

  @Test public void testSetPrinter() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getPrinter() );
    AllItemsHtmlPrinter mockPrinter = mock( AllItemsHtmlPrinter.class );
    pageableHTMLOutput.setPrinter( mockPrinter );
    Assert.assertEquals( mockPrinter, pageableHTMLOutput.getPrinter() );
  }

  @Test public void testSetReportProcessor() throws Exception {
    Assert.assertNull( pageableHTMLOutput.getReportProcessor() );
    PageableReportProcessor mockProcessor = mock( PageableReportProcessor.class );
    pageableHTMLOutput.setReportProcessor( mockProcessor );
    Assert.assertEquals( mockProcessor, pageableHTMLOutput.getReportProcessor() );
  }

  @Test public void testSupportsPagination() throws Exception {
    Assert.assertEquals( true, pageableHTMLOutput.supportsPagination() );
  }

  @Test public void testPaginate() throws Exception {
    PageableHTMLOutput output = mock( PageableHTMLOutput.class, CALLS_REAL_METHODS );
    PageableReportProcessor processor = mock( PageableReportProcessor.class );
    doNothing().when( output ).reinitOutputTarget();
    doReturn( true ).when( processor ).isPaginated();
    MasterReport report = mock( MasterReport.class );
    AllItemsHtmlPrinter printer = mock( AllItemsHtmlPrinter.class );
    doNothing().when( printer ).setContentWriter( null, null );
    doNothing().when( printer ).setDataWriter( null, null );

    output.setReportProcessor( processor );
    output.setPrinter( printer );
    Assert.assertEquals( 0, output.paginate( report, 0 ) );

    doReturn( false ).when( processor ).isPaginated();
    output.setReportProcessor( processor );
    Assert.assertEquals( 0, output.paginate( report, 0 ) );
  }

  @Test
  public void testGenerateListener() throws Exception {
    ClassicEngineBoot.getInstance().start();
    try ( ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
      pageableHTMLOutput.generate( new MasterReport(), 1, stream, 1 );
    }
    verify( listener, times( 1 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 1 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, atLeastOnce() ).reportProcessingUpdate( any( ReportProgressEvent.class ) );


  }

  @Test
  public void testGenerate() throws Exception {
    ClassicEngineBoot.getInstance().start();
    ReportListenerThreadHolder.clear();
    try ( ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
      pageableHTMLOutput.generate( new MasterReport(), 1, stream, 1 );
    }

    verify( listener, times( 0 ) ).reportProcessingStarted( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingFinished( any( ReportProgressEvent.class ) );
    verify( listener, times( 0 ) ).reportProcessingUpdate( any( ReportProgressEvent.class ) );
  }
}
