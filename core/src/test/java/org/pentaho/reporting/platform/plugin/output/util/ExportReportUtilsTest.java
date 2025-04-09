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

package org.pentaho.reporting.platform.plugin.output.util;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportHeader;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExportReportUtilsTest {

  private ExportReportUtils reportUtils;
  private MasterReport mockReport;
  private ReportHeader mockReportHeader;
  private Element mockLabel;
  private Element mockText;

  @Before
  public void setUp() {
    reportUtils = spy( new ExportReportUtils() );
    mockReport = mock( MasterReport.class );
    mockReportHeader = mock( ReportHeader.class );
    mockLabel = mock( Element.class );
    mockText = mock( Element.class );
    when( mockReport.getReportHeader() ).thenReturn( mockReportHeader );
    doReturn( mockLabel ).when( reportUtils ).createLabel( anyString() );
    doReturn( mockText ).when( reportUtils ).createText( anyString() );
  }

  @Test
  public void testAddFiltersAndPromptsPage_WithValidReport() {
    reportUtils.setPirExported( true );
    reportUtils.setReadableFilterDescription( "Test Filter Description" );

    reportUtils.addFiltersAndPromptsPage( mockReport );

    verify( mockReportHeader, times( 1 ) ).setPagebreakAfterPrint( true );
    verify( mockReportHeader, atLeastOnce() ).addElement( mockLabel );
  }

  @Test( expected = IllegalStateException.class )
  public void testAddFiltersAndPromptsPage_WithNullReportHeader() {
    reportUtils.setPirExported( true );
    when( mockReport.getReportHeader() ).thenReturn( null );

    reportUtils.addFiltersAndPromptsPage( mockReport );
  }

  @Test
  public void testAddFiltersAndPromptsPage_WithNullReport() {
    reportUtils.setPirExported( true );
    reportUtils.addFiltersAndPromptsPage( null );

    verify( mockReportHeader, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddFiltersAndPromptsPage_NotFromPIR() {
    reportUtils.setPirExported( false );
    reportUtils.addFiltersAndPromptsPage( mockReport );

    verify( mockReportHeader, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testFormatParameterValue_WithSupportedTypes() {

    String stringValue = "Test String";
    Number numberValue = 123;
    java.sql.Date dateValue = java.sql.Date.valueOf( LocalDate.now() );
    String[] stringArray = { "A", "B", "C" };
    Number[] numberArray = { 1, 2, 3 };
    String[] dateStrings = { "2025-04-17", "2023-12-25", "2024-01-01" };
    Date[] sqlDateArray = Arrays.stream( dateStrings )
      .map( LocalDate::parse ) // parse string to LocalDate
      .map( Date::valueOf )    // convert LocalDate to java.sql.Date
      .toArray( Date[]::new );

    assertEquals( stringValue, reportUtils.formatParameterValue( stringValue ) );
    assertEquals( "123", reportUtils.formatParameterValue( numberValue ) );
    assertNotNull( reportUtils.formatParameterValue( dateValue ) );
    assertEquals( "A, B, C", reportUtils.formatParameterValue( stringArray ) );
    assertEquals( "1, 2, 3", reportUtils.formatParameterValue( numberArray ) );
    assertEquals( "2025-04-17, 2023-12-25, 2024-01-01", reportUtils.formatParameterValue( sqlDateArray ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testFormatParameterValue_WithUnsupportedType() {
    Object unsupportedValue = new Object();

    reportUtils.formatParameterValue( unsupportedValue );
  }

  @Test
  public void testAddFilters_WithReadableFilterDescription() {
    reportUtils.setReadableFilterDescription( "Test Filter Description" );

    reportUtils.addFilters( mockReportHeader );

    verify( mockReportHeader, times( 1 ) ).addElement( mockText );
  }

  @Test
  public void testAddFilters_WithNoReadableFilterDescription() {
    reportUtils.setReadableFilterDescription( null );

    reportUtils.addFilters( mockReportHeader );

    verify( mockReportHeader, times( 1 ) ).addElement( mockText );
  }

  @Test
  public void testAddPrompts_WithNullParameterValue() {
    ReportParameterValues mockParameterValues = mock( ReportParameterValues.class );
    when( mockReport.getParameterValues() ).thenReturn( mockParameterValues );
    when( mockParameterValues.getColumnNames() ).thenReturn( new String[] { "Param1" } );
    when( mockParameterValues.get( "Param1" ) ).thenReturn( null );

    reportUtils.addPrompts( mockReportHeader, mockReport );

    verify( mockReportHeader, times( 1 ) ).addElement( mockText );
  }

  @Test
  public void testAddPrompts_WithValidParameterValues() {
    ReportParameterValues mockParameterValues = mock( ReportParameterValues.class );
    when( mockReport.getParameterValues() ).thenReturn( mockParameterValues );
    when( mockParameterValues.getColumnNames() ).thenReturn( new String[] { "Param1", "Param2" } );
    when( mockParameterValues.get( "Param1" ) ).thenReturn( "Value1" );
    when( mockParameterValues.get( "Param2" ) ).thenReturn( 123 );

    reportUtils.addPrompts( mockReportHeader, mockReport );

    verify( mockReportHeader, times( 2 ) ).addElement( mockText );
  }

  @Test
  public void testReportUtilsFactoryReturnsSingletonInstance() {
    ExportReportUtils instance1 = ExportReportUtilsFactory.getUtil();
    ExportReportUtils instance2 = ExportReportUtilsFactory.getUtil();

    assertNotNull( instance1 );
    assertNotNull( instance2 );
    assertEquals( instance1, instance2 );
  }
}
