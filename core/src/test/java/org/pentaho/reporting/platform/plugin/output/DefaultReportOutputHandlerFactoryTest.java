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
 * Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.output;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.platform.plugin.SimpleReportingAction;
import org.pentaho.reporting.platform.plugin.messages.Messages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DefaultReportOutputHandlerFactoryTest {

  DefaultReportOutputHandlerFactory roh;

  @Before
  public void setUp() {
    roh = new DefaultReportOutputHandlerFactory();
  }

  @Test
  public void testSetHtmlStreamVisible() throws Exception {
    assertTrue( roh.isHtmlStreamVisible() );
    roh.setHtmlStreamVisible( false );
    assertFalse( roh.isHtmlStreamVisible() );
  }

  @Test
  public void testSetHtmlPageVisible() throws Exception {
    assertTrue( roh.isHtmlPageVisible() );
    roh.setHtmlPageVisible( false );
    assertFalse( roh.isHtmlPageVisible() );
  }

  @Test
  public void testSetXlsVisible() throws Exception {
    assertTrue( roh.isXlsVisible() );
    roh.setXlsVisible( false );
    assertFalse( roh.isXlsVisible() );
  }

  @Test
  public void testSetXlsxVisible() throws Exception {
    assertTrue( roh.isXlsxVisible() );
    roh.setXlsxVisible( false );
    assertFalse( roh.isXlsxVisible() );
  }

  @Test
  public void testSetCsvVisible() throws Exception {
    assertTrue( roh.isCsvVisible() );
    roh.setCsvVisible( false );
    assertFalse( roh.isCsvVisible() );
  }

  @Test
  public void testSetRtfVisible() throws Exception {
    assertTrue( roh.isRtfVisible() );
    roh.setRtfVisible( false );
    assertFalse( roh.isRtfVisible() );
  }

  @Test
  public void testSetPdfVisible() throws Exception {
    assertTrue( roh.isPdfVisible() );
    roh.setPdfVisible( false );
    assertFalse( roh.isPdfVisible() );
  }

  @Test
  public void testSetTextVisible() throws Exception {
    assertTrue( roh.isTextVisible() );
    roh.setTextVisible( false );
    assertFalse( roh.isTextVisible() );
  }

  @Test
  public void testSetMailVisible() throws Exception {
    assertTrue( roh.isMailVisible() );
    roh.setMailVisible( false );
    assertFalse( roh.isMailVisible() );
  }

  @Test
  public void testSetXmlTableVisible() throws Exception {
    assertTrue( roh.isXmlTableVisible() );
    roh.setXmlTableVisible( false );
    assertFalse( roh.isXmlTableVisible() );
  }

  @Test
  public void testSetXmlPageVisible() throws Exception {
    assertTrue( roh.isXmlPageVisible() );
    roh.setXmlPageVisible( false );
    assertFalse( roh.isXmlPageVisible() );
  }

  @Test
  public void testSetPngVisible() throws Exception {
    assertTrue( roh.isPngVisible() );
    roh.setPngVisible( false );
    assertFalse( roh.isPngVisible() );
  }

  @Test
  public void testSetHtmlStreamAvailable() throws Exception {
    assertTrue( roh.isHtmlStreamAvailable() );
    roh.setHtmlStreamAvailable( false );
    assertFalse( roh.isHtmlStreamAvailable() );
  }

  @Test
  public void testSetHtmlPageAvailable() throws Exception {
    assertTrue( roh.isHtmlPageAvailable() );
    roh.setHtmlPageAvailable( false );
    assertFalse( roh.isHtmlPageAvailable() );
  }

  @Test
  public void testSetXlsAvailable() throws Exception {
    assertTrue( roh.isXlsAvailable() );
    roh.setXlsAvailable( false );
    assertFalse( roh.isXlsAvailable() );
  }

  @Test
  public void testSetXlsxAvailable() throws Exception {
    assertTrue( roh.isXlsxAvailable() );
    roh.setXlsxAvailable( false );
    assertFalse( roh.isXlsxAvailable() );
  }

  @Test
  public void testSetCsvAvailable() throws Exception {
    assertTrue( roh.isCsvAvailable() );
    roh.setCsvAvailable( false );
    assertFalse( roh.isCsvAvailable() );
  }

  @Test
  public void testSetRtfAvailable() throws Exception {
    assertTrue( roh.isRtfAvailable() );
    roh.setRtfAvailable( false );
    assertFalse( roh.isRtfAvailable() );
  }

  @Test
  public void testSetPdfAvailable() throws Exception {
    assertTrue( roh.isPdfAvailable() );
    roh.setPdfAvailable( false );
    assertFalse( roh.isPdfAvailable() );
  }

  @Test
  public void testSetTextAvailable() throws Exception {
    assertTrue( roh.isTextAvailable() );
    roh.setTextAvailable( false );
    assertFalse( roh.isTextAvailable() );
  }

  @Test
  public void testSetMailAvailable() throws Exception {
    assertTrue( roh.isMailAvailable() );
    roh.setMailAvailable( false );
    assertFalse( roh.isMailAvailable() );
  }

  @Test
  public void testSetXmlTableAvailable() throws Exception {
    assertTrue( roh.isXmlTableAvailable() );
    roh.setXmlTableAvailable( false );
    assertFalse( roh.isXmlTableAvailable() );
  }

  @Test
  public void testSetXmlPageAvailable() throws Exception {
    assertTrue( roh.isXmlPageAvailable() );
    roh.setXmlPageAvailable( false );
    assertFalse( roh.isXmlPageAvailable() );
  }

  @Test
  public void testSetPngAvailable() throws Exception {
    assertTrue( roh.isPngAvailable() );
    roh.setPngAvailable( false );
    assertFalse( roh.isPngAvailable() );
  }

  @Test
  public void testGetMimeType() throws Exception {
    MockOutputHandlerSelector selector = new MockOutputHandlerSelector();

    HashMap<String, String> outputTypes = new HashMap<String, String>();
    outputTypes.put( "table/html;page-mode=stream", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "table/html;page-mode=page", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "table/excel;page-mode=flow", "application/vnd.ms-excel" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;page-mode=flow",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "table/csv;page-mode=stream", "text/csv" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "table/rtf;page-mode=flow", "application/rtf" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "pageable/pdf", "application/pdf" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "pageable/text", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "mime-message/text/html", "mime-message/text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "table/xml", "application/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "pageable/xml", "application/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "pageable/X-AWT-Graphics;image-type=png", "image/png" ); //$NON-NLS-1$ //$NON-NLS-2$
    outputTypes.put( "", "application/octet-stream" ); //$NON-NLS-1$ //$NON-NLS-2$

    for ( Map.Entry<String, String> outputType : outputTypes.entrySet() ) {
      selector.setOutputType( outputType.getKey() );
      assertEquals( outputType.getValue(), roh.getMimeType( selector ) );
    }
  }

  @Test
  public void testGetSupportedOutputTypes() throws Exception {
    List<String> expectations = new ArrayList<String>();
    expectations.add( "table/html;page-mode=page" );
    expectations.add( "table/html;page-mode=stream" );
    expectations.add( "pageable/pdf" );
    expectations.add( "table/excel;page-mode=flow" );
    expectations.add( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;page-mode=flow" );
    expectations.add( "table/csv;page-mode=stream" );
    expectations.add( "table/rtf;page-mode=flow" );
    expectations.add( "pageable/text" );

    Set<Map.Entry<String, String>> outputTypes = roh.getSupportedOutputTypes();
    List<Object> outputs = Arrays.asList( outputTypes.toArray() );
    List<String> expectedOutputs = new ArrayList<String>();

    for ( int i = 0; i < outputTypes.size(); i++ ) {
      expectedOutputs.add( ( (Map.Entry) outputs.get( i ) ).getKey().toString() );
    }

    for ( String expectation : expectations ) {
      assertTrue( expectedOutputs.contains( expectation ) );
    }
  }

  @Test
  public void testCreateOutputHandlerForOutputType() throws Exception {
    MockOutputHandlerSelector selector = new MockOutputHandlerSelector();

    selector.setOutputType( "pageable/X-AWT-Graphics;image-type=png" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof PNGOutput );
    roh.setPngAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "pageable/xml" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof XmlPageableOutput );
    roh.setXmlPageAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "table/xml" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof XmlTableOutput );
    roh.setXmlTableAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "pageable/pdf" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof PDFOutput );
    roh.setPdfAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "table/excel;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof XLSOutput );
    roh.setXlsAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType(
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof XLSXOutput );
    roh.setXlsxAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "table/csv;page-mode=stream" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof CSVOutput );
    roh.setCsvAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "table/rtf;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof RTFOutput );
    roh.setRtfAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "mime-message/text/html" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof EmailOutput );
    roh.setMailAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "pageable/text" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType( selector ) instanceof PlainTextOutput );
    roh.setTextAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType( selector ) );

    selector.setOutputType( "" ); //$NON-NLS-1$
    assertNull( roh.createOutputHandlerForOutputType( selector ) );
  }

  @Test
  public void htmlNotAvailable() {
    roh.setHtmlPageAvailable( false );
    roh.setHtmlStreamAvailable( false );
    assertNull( roh.createHtmlPageOutput( null ) );
    assertNull( roh.createHtmlStreamOutput( null ) );
  }

  @Test
  public void htmlCachingJcrOrTemp() {
    ReportOutputHandlerSelector mockSelector = mock( ReportOutputHandlerSelector.class );

    //Test for pageable HTML reports writing to JCR
    when( mockSelector.isUseJcrOutput() ).thenReturn( true );
    when( mockSelector.getJcrOutputPath() ).thenReturn( "test/path/for/jcr/output" );
    DefaultReportOutputHandlerFactory mockRoh = mock( DefaultReportOutputHandlerFactory.class, CALLS_REAL_METHODS );
    doReturn( true ).when( mockRoh ).isHtmlPageAvailable();
    doReturn( "http://localhost:8080/pentaho/api/repo/files/{0}/inline" ).when( mockRoh ).computeContentHandlerPattern( mockSelector );
    doReturn( true ).when( mockRoh ).isCachePageableHtmlContentEnabled( any() );

    ReportOutputHandler out = mockRoh.createHtmlPageOutput( mockSelector );
    assertNotNull( ( ( CachingPageableHTMLOutput ) out ).getJcrOutputPath() );
    assertTrue( ( ( CachingPageableHTMLOutput ) out ).isJcrImagesAndCss() );

    //Test for pageable HTML reports not writing to JCR
    when( mockSelector.isUseJcrOutput() ).thenReturn( false );
    when( mockSelector.getJcrOutputPath() ).thenReturn( "test/path/for/tmp/output" );
    DefaultReportOutputHandlerFactory mockRoh2 = mock( DefaultReportOutputHandlerFactory.class, CALLS_REAL_METHODS );
    doReturn( true ).when( mockRoh2 ).isHtmlPageAvailable();
    doReturn( "/pentaho/getImage?image={0}" ).when( mockRoh2 ).computeContentHandlerPattern( mockSelector );
    doReturn( true ).when( mockRoh2 ).isCachePageableHtmlContentEnabled( any() );

    ReportOutputHandler out2 = mockRoh2.createHtmlPageOutput( mockSelector );
    assertNull( ( ( CachingPageableHTMLOutput ) out2 ).getJcrOutputPath() );
    assertFalse( ( ( CachingPageableHTMLOutput ) out2 ).isJcrImagesAndCss() );
  }

  @Test
  public void emailNotAvailable() {
    roh.setMailAvailable( false );
    roh.setMailVisible( false );
    final Messages m = Messages.getInstance();
    final Map.Entry<String, String> o = new Map.Entry<String, String>() {
      @Override public String getKey() {
        return SimpleReportingAction.MIME_TYPE_EMAIL;
      }

      @Override public String getValue() {
        return m.getString( "ReportPlugin.outputEmail" );
      }

      @Override public String setValue( String value ) {
        return null;
      }
    };
    assertFalse( roh.getSupportedOutputTypes().contains( o ) );
    roh.setMailAvailable( true );
    assertFalse( roh.getSupportedOutputTypes().contains( o ) );
    roh.setMailAvailable( false );
    roh.setMailVisible( true );
    assertFalse( roh.getSupportedOutputTypes().contains( o ) );
    roh.setMailAvailable( true );
    roh.setMailVisible( true );
    assertTrue( roh.getSupportedOutputTypes().contains( o ) );
  }
}
