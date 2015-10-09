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

import java.util.*;

public class DefaultReportOutputHandlerFactoryTest extends TestCase {

  DefaultReportOutputHandlerFactory roh;

  protected void setUp() {
    roh = new DefaultReportOutputHandlerFactory();
  }

  public void testSetHtmlStreamVisible() throws Exception {
    assertTrue( roh.isHtmlStreamVisible() );
    roh.setHtmlStreamVisible( false );
    assertFalse( roh.isHtmlStreamVisible() );
  }

  public void testSetHtmlPageVisible() throws Exception {
    assertTrue( roh.isHtmlPageVisible() );
    roh.setHtmlPageVisible( false );
    assertFalse( roh.isHtmlPageVisible());
  }

  public void testSetXlsVisible() throws Exception {
    assertTrue( roh.isXlsVisible() );
    roh.setXlsVisible(false);
    assertFalse( roh.isXlsVisible());
  }

  public void testSetXlsxVisible() throws Exception {
    assertTrue( roh.isXlsxVisible() );
    roh.setXlsxVisible(false);
    assertFalse( roh.isXlsxVisible());
  }

  public void testSetCsvVisible() throws Exception {
    assertTrue( roh.isCsvVisible() );
    roh.setCsvVisible(false);
    assertFalse( roh.isCsvVisible());
  }

  public void testSetRtfVisible() throws Exception {
    assertTrue( roh.isRtfVisible() );
    roh.setRtfVisible(false);
    assertFalse( roh.isRtfVisible());
  }

  public void testSetPdfVisible() throws Exception {
    assertTrue( roh.isPdfVisible() );
    roh.setPdfVisible(false);
    assertFalse( roh.isPdfVisible());
  }

  public void testSetTextVisible() throws Exception {
    assertTrue( roh.isTextVisible() );
    roh.setTextVisible(false);
    assertFalse( roh.isTextVisible());
  }

  public void testSetMailVisible() throws Exception {
    assertTrue( roh.isMailVisible() );
    roh.setMailVisible(false);
    assertFalse( roh.isMailVisible());
  }

  public void testSetXmlTableVisible() throws Exception {
    assertTrue( roh.isXmlTableVisible() );
    roh.setXmlTableVisible(false);
    assertFalse( roh.isXmlTableVisible());
  }

  public void testSetXmlPageVisible() throws Exception {
    assertTrue( roh.isXmlPageVisible() );
    roh.setXmlPageVisible(false);
    assertFalse( roh.isXmlPageVisible());
  }

  public void testSetPngVisible() throws Exception {
    assertTrue( roh.isPngVisible() );
    roh.setPngVisible(false);
    assertFalse( roh.isPngVisible());
  }

  public void testSetHtmlStreamAvailable() throws Exception {
    assertTrue( roh.isHtmlStreamAvailable() );
    roh.setHtmlStreamAvailable(false);
    assertFalse( roh.isHtmlStreamAvailable());
  }

  public void testSetHtmlPageAvailable() throws Exception {
    assertTrue( roh.isHtmlPageAvailable() );
    roh.setHtmlPageAvailable(false);
    assertFalse( roh.isHtmlPageAvailable());
  }

  public void testSetXlsAvailable() throws Exception {
    assertTrue( roh.isXlsAvailable() );
    roh.setXlsAvailable(false);
    assertFalse( roh.isXlsAvailable());
  }

  public void testSetXlsxAvailable() throws Exception {
    assertTrue( roh.isXlsxAvailable() );
    roh.setXlsxAvailable(false);
    assertFalse( roh.isXlsxAvailable());
  }

  public void testSetCsvAvailable() throws Exception {
    assertTrue( roh.isCsvAvailable() );
    roh.setCsvAvailable(false);
    assertFalse( roh.isCsvAvailable());
  }

  public void testSetRtfAvailable() throws Exception {
    assertTrue( roh.isRtfAvailable() );
    roh.setRtfAvailable(false);
    assertFalse( roh.isRtfAvailable());
  }

  public void testSetPdfAvailable() throws Exception {
    assertTrue( roh.isPdfAvailable() );
    roh.setPdfAvailable(false);
    assertFalse( roh.isPdfAvailable());
  }

  public void testSetTextAvailable() throws Exception {
    assertTrue( roh.isTextAvailable() );
    roh.setTextAvailable(false);
    assertFalse( roh.isTextAvailable());
  }

  public void testSetMailAvailable() throws Exception {
    assertTrue( roh.isMailAvailable() );
    roh.setMailAvailable(false);
    assertFalse( roh.isMailAvailable());
  }

  public void testSetXmlTableAvailable() throws Exception {
    assertTrue( roh.isXmlTableAvailable() );
    roh.setXmlTableAvailable(false);
    assertFalse( roh.isXmlTableAvailable());
  }

  public void testSetXmlPageAvailable() throws Exception {
    assertTrue( roh.isXmlPageAvailable() );
    roh.setXmlPageAvailable(false);
    assertFalse( roh.isXmlPageAvailable());
  }

  public void testSetPngAvailable() throws Exception {
    assertTrue( roh.isPngAvailable() );
    roh.setPngAvailable(false);
    assertFalse( roh.isPngAvailable());
  }

  public void testGetMimeType() throws Exception {
    MockOutputHandlerSelector selector = new MockOutputHandlerSelector();

    HashMap<String, String> outpytTypes = new HashMap<String, String>();
    outpytTypes.put( "table/html;page-mode=stream", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "table/html;page-mode=page", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "table/excel;page-mode=flow", "application/vnd.ms-excel" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;page-mode=flow",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "table/csv;page-mode=stream", "text/csv" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "table/rtf;page-mode=flow", "application/rtf" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "pageable/pdf", "application/pdf" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "pageable/text", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "mime-message/text/html", "mime-message/text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "table/xml", "application/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "pageable/xml", "application/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "pageable/X-AWT-Graphics;image-type=png", "image/png" ); //$NON-NLS-1$ //$NON-NLS-2$
    outpytTypes.put( "", "application/octet-stream" ); //$NON-NLS-1$ //$NON-NLS-2$

    for (Map.Entry<String, String> outputType: outpytTypes.entrySet()) {
      selector.setOutputType( outputType.getKey() );
      assertEquals(outputType.getValue(), roh.getMimeType(selector) );
    }
  }

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
    List<Object> outputs = Arrays.asList(outputTypes.toArray());
    List<String> expectedOutputs = new ArrayList<String>(  );

    for (int i = 0; i < outputTypes.size(); i++) {
      expectedOutputs.add(((Map.Entry) outputs.get(i)).getKey().toString());
    }

    for (String expectation: expectations) {
      assertTrue( expectedOutputs.contains(expectation) );
    }
  }

  public void testCreateOutputHandlerForOutputType() throws Exception {
    MockOutputHandlerSelector selector = new MockOutputHandlerSelector();

    selector.setOutputType( "pageable/X-AWT-Graphics;image-type=png" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof PNGOutput );
    roh.setPngAvailable( false );
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "pageable/xml" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof XmlPageableOutput);
    roh.setXmlPageAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "table/xml" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof XmlTableOutput);
    roh.setXmlTableAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "pageable/pdf" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof PDFOutput);
    roh.setPdfAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "table/excel;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof XLSOutput);
    roh.setXlsAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof XLSXOutput);
    roh.setXlsxAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "table/csv;page-mode=stream" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof CSVOutput);
    roh.setCsvAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "table/rtf;page-mode=flow" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof RTFOutput);
    roh.setRtfAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "mime-message/text/html" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof EmailOutput);
    roh.setMailAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType( "pageable/text" ); //$NON-NLS-1$
    assertTrue( roh.createOutputHandlerForOutputType(selector) instanceof PlainTextOutput);
    roh.setTextAvailable(false);
    assertNull( roh.createOutputHandlerForOutputType(selector) );

    selector.setOutputType(""); //$NON-NLS-1$
    assertNull(roh.createOutputHandlerForOutputType(selector));
  }
}
