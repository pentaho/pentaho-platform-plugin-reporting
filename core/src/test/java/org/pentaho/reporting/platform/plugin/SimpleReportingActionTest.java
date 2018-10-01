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

package org.pentaho.reporting.platform.plugin;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IActionSequenceResource;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.config.ModifiableConfiguration;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


public class SimpleReportingActionTest {
  SimpleReportingAction sra, sraMock;

  @Before
  public void setUp() {
    sra = new SimpleReportingAction();
    sraMock = mock( SimpleReportingAction.class );
  }

  @Test
  public void testSetDefaultOutputTarget() throws Exception {
    try {
      sra.setDefaultOutputTarget( null );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
    sra.setDefaultOutputTarget( "defaultOutputTarget" );
    assertEquals( "defaultOutputTarget", sra.getDefaultOutputTarget() );
  }

  @Test
  public void testSetOutputTarget() throws Exception {
    sra.setOutputTarget( "outputTarget" );
    assertEquals( "outputTarget", sra.getOutputTarget() );
  }

  @Test
  public void testSetOutputType() throws Exception {
    sra.setOutputType( "outputType" );
    assertEquals( "outputType", sra.getOutputType() );
  }

  @Test
  public void testSetReportDefinition() throws Exception {
    IActionSequenceResource resource = mock( IActionSequenceResource.class );
    sra.setReportDefinition( resource );
    assertEquals( resource, sra.getReportDefinition() );
  }

  @Test
  public void testSetReportDefinitionPath() throws Exception {
    sra.setReportDefinitionPath( "path" );
    assertEquals( "path", sra.getReportDefinitionPath() );
  }

  @Test
  public void testSetPaginateOutput() throws Exception {
    assertFalse( sra.isPaginateOutput() );
    sra.setPaginateOutput( true );
    assertTrue( sra.isPaginateOutput() );
  }

  @Test
  public void testSetAcceptedPage() throws Exception {
    sra.setAcceptedPage( 1 );
    assertEquals( 1, sra.getAcceptedPage() );
  }

  @Test
  public void testSetDashboardMode() throws Exception {
    assertFalse( sra.isDashboardMode() );
    sra.setDashboardMode( true );
    assertTrue( sra.isDashboardMode() );
  }

  @Test
  public void testSetUseJcr() throws Exception {
    assertFalse( sra.getUseJCR() );
    sra.setUseJcr( true );
    assertTrue( sra.getUseJCR() );
  }

  @Test
  public void testSetJcrOutputPath() throws Exception {
    sra.setJcrOutputPath( "path" );
    assertEquals( "path", sra.getJcrOutputPath() );
  }

  @Test
  public void testSetPrint() throws Exception {
    assertFalse( sra.isPrint() );
    sra.setPrint( true );
    assertTrue( sra.isPrint() );
  }

  @Test
  public void testSetPrinter() throws Exception {
    sra.setPrinter( "printer" );
    assertEquals( "printer", sra.getPrinter() );
  }

  @Test
  public void testSetVarArgs() throws Exception {
    assertTrue( sra.getInputs().size() == 0 );
    sra.setVarArgs( null );
    assertTrue( sra.getInputs().size() == 0 );

    Map<String, Object> inputs = new HashMap<String, Object>() {
      {
        put( "key", "value" );
      }
    };
    sra.setVarArgs( inputs );
    assertEquals( inputs, sra.getInputs() );

    assertEquals( "value", sra.getInput( "key", "defaultValue" ) );
  }

  @Test
  public void testSetReport() throws Exception {
    MasterReport report = mock( MasterReport.class );
    Configuration config = mock( Configuration.class );
    doReturn( config ).when( report ).getConfiguration();
    doNothing().when( report ).setReportEnvironment( any( ReportEnvironment.class ) );

    sra.setReport( report );
    assertEquals( -1, sra.getPageCount() );
    assertEquals( report, sra.getReport() );

    final ArrayList<String> list = new ArrayList<String>() {
      {
        add( "value1" );
        add( "value2" );
      }
    };
    Map<String, Object> inputs = new HashMap<String, Object>() {
      {
        put( "::cl", list );
      }
    };
    sra.setVarArgs( inputs );
    sra.setReport( report );
    assertEquals( report, sra.getReport() );

    final String[] array = new String[ 3 ];
    array[ 0 ] = "value1";
    array[ 1 ] = "value2";
    inputs = new HashMap<String, Object>() {
      {
        put( "::cl", array );
      }
    };
    sra.setVarArgs( inputs );
    sra.setReport( report );
    assertEquals( report, sra.getReport() );

    inputs = new HashMap<String, Object>() {
      {
        put( "::cl", 1 );
      }
    };
    sra.setVarArgs( inputs );
    sra.setReport( report );
    assertEquals( report, sra.getReport() );
  }

  @Test
  public void testGetReportQueryLimit() throws Exception {

    MasterReport report = new MasterReport();
    report.setQueryLimit( 10 );
    assertEquals( 10, report.getQueryLimit() );
    sra.setReport( report );
    assertEquals( -1, sra.getReport().getQueryLimit() );
  }

  @Test
  public void testValidade() throws Exception {
    assertFalse( sra.validate() );

    IActionSequenceResource resource = mock( IActionSequenceResource.class );
    sra.setReportDefinition( resource );
    InputStream inputStream = mock( InputStream.class );
    sra.setInputStream( inputStream );
    sra.setReportDefinitionPath( "" );

    assertFalse( sra.validate() );

    OutputStream outputStream = mock( OutputStream.class );
    sra.setOutputStream( outputStream );
    sra.setPrint( true );

    assertTrue( sra.validate() );
  }

  @Test
  public void testGetViewerSessionId() throws Exception {
    assertNull( sra.getViewerSessionId() );

    Map<String, Object> inputs = new HashMap<String, Object>() {
      {
        put( "::session", 1 );
      }
    };
    sra.setVarArgs( inputs );
    assertNull( sra.getViewerSessionId() );

    inputs = new HashMap<String, Object>() {
      {
        put( "::session", "1" );
      }
    };
    sra.setVarArgs( inputs );
    assertEquals( "1", sra.getViewerSessionId() );
  }

  @Test
  public void testGetMimeType() throws Exception {
    String mimeType;
    mimeType = sra.getMimeType( "" );
    assertEquals( mimeType, sra.MIME_GENERIC_FALLBACK );

    MasterReport report = mock( MasterReport.class );
    Configuration config = mock( Configuration.class );
    doReturn( config ).when( report ).getConfiguration();
    doNothing().when( report ).setReportEnvironment( any( ReportEnvironment.class ) );
    sra.setReport( report );

    mimeType = sra.getMimeType();
    assertEquals( "text/html", mimeType );

    doReturn( true ).when( report ).getAttribute( AttributeNames.Core.NAMESPACE,
        AttributeNames.Core.LOCK_PREFERRED_OUTPUT_TYPE );
    doReturn( "pageable/X-AWT-Graphics;image-type=png" ).when( report )
        .getAttribute( AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE );
    sra.setReport( report );

    mimeType = sra.getMimeType();
    assertEquals( "image/png", mimeType );

    Map<String, String> map = new HashMap<String, String>() {
      {
        put( "text/csv", "text/csv" );
        put( "text/html", "text/html" );
        put( "application/xml", "application/xml" );
        put( "application/pdf", "application/pdf" );
        put( "application/rtf", "application/rtf" );
        put( "application/vnd.ms-excel", "application/vnd.ms-excel" );
        put( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" );
        put( "mime-message/text/html", "mime-message/text/html" );
        put( "text/plain", "text/plain" );
        put( "pdf", "application/pdf" );
        put( "html", "text/html" );
        put( "csv", "text/csv" );
        put( "rtf", "application/rtf" );
        put( "xls", "application/vnd.ms-excel" );
        put( "txt", "text/plain" );
      }
    };

    for ( int i = 0; i < map.size(); i++ ) {
      String key = map.keySet().toArray()[ i ].toString();
      doReturn( key ).when( report )
          .getAttribute( AttributeNames.Core.NAMESPACE, AttributeNames.Core.PREFERRED_OUTPUT_TYPE );
      mimeType = sra.getMimeType();
      assertEquals( map.get( key ), mimeType );
      sra.setPaginateOutput( true );

      mimeType = sra.getMimeType();
      assertEquals( map.get( key ), mimeType );
      sra.setPaginateOutput( false );
    }
  }

  @Test
  public void testGetYieldRate() throws Exception {
    assertEquals( 0, sra.getYieldRate() );

    Map<String, Object> inputs = new HashMap<String, Object>() {
      {
        put( "yield-rate", 0.5 );
      }
    };
    sra.setVarArgs( inputs );

    assertEquals( 0, sra.getYieldRate() );

    inputs = new HashMap<String, Object>() {
      {
        put( "yield-rate", 3 );
      }
    };
    sra.setVarArgs( inputs );

    assertEquals( 3, sra.getYieldRate() );
  }

  @Test
  public void testExecute() throws Exception {
    MasterReport report = mock( MasterReport.class );
    Configuration config = mock( Configuration.class );
    doReturn( config ).when( report ).getConfiguration();
    doNothing().when( report ).setReportEnvironment( any( ReportEnvironment.class ) );
    ModifiableConfiguration modifiableConfiguration = mock( ModifiableConfiguration.class );
    doReturn( modifiableConfiguration ).when( report ).getReportConfiguration();
    doNothing().when( modifiableConfiguration ).setConfigProperty( anyString(), anyString() );

    Map<String, Object> inputs = new HashMap<String, Object>() {
      {
        put( "_SCH_EMAIL_TO", "value" );
        put( "yield-rate", 3 );
      }
    };
    sra.setVarArgs( inputs );
    sra.setOutputTarget( "table/html;page-mode=page" );

    sra.setReport( report );
    assertFalse( sra._execute() );
    assertEquals( 3, sra.getYieldRate() );
    assertEquals( "table/html;page-mode=page", sra.getInput( "output-target", "" ) );
  }

  @Test
  public void testCreateOutputHandlerForOutputtype() throws Exception {
    MasterReport report = mock( MasterReport.class );
    Configuration config = mock( Configuration.class );
    doReturn( config ).when( report ).getConfiguration();
    doNothing().when( report ).setReportEnvironment( any( ReportEnvironment.class ) );
    doReturn( false ).when( report ).getAttribute( anyString(), anyString() );

    ModifiableConfiguration modifiableConfiguration = mock( ModifiableConfiguration.class );
    doNothing().when( modifiableConfiguration ).setConfigProperty( anyString(), anyString() );
    doReturn( modifiableConfiguration ).when( report ).getReportConfiguration();

    sra.setReport( report );
    sra.setDashboardMode( true );

    ReportOutputHandler result = sra.createOutputHandlerForOutputType( "pageable/text" );
    assertNotNull( result );
    verify( modifiableConfiguration, times( 1 ) ).setConfigProperty( HtmlTableModule.BODY_FRAGMENT, "true" );
  }
}
