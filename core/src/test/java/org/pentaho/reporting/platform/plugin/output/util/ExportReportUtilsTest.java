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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.metadata.query.model.Constraint;
import org.pentaho.metadata.query.model.Query;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportHeader;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.parser.ParseException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class ExportReportUtilsTest {

  private ExportReportUtils exportReportUtilsSpy;
  @Mock
  private MasterReport masterReportMock;
  @Mock
  private ReportHeader reportHeaderMock;
  @Mock
  private DefaultParameterContext parameterContextMock;
  @Mock
  private Query queryMock;
  @Mock
  private Constraint constraintMock;
  @Mock
  private ReadableFilterUtil readableFilterUtilMock;
  @Mock
  private Element mockElement;

  @Before
  public void setUp() {
    exportReportUtilsSpy = spy( ExportReportUtils.getInstance() );
    when( masterReportMock.getReportHeader() ).thenReturn( reportHeaderMock );
    when( masterReportMock.getDataFactory() ).thenReturn( mock( CompoundDataFactory.class ) );
    when( masterReportMock.getQuery() ).thenReturn( "query" );
    when( masterReportMock.getParameterDefinition() ).thenReturn( mock( ReportParameterDefinition.class ) );
    when( masterReportMock.getParameterDefinition().getParameterDefinitions() ).thenReturn(
      new ParameterDefinitionEntry[ 0 ] );

    doReturn( mockElement ).when( exportReportUtilsSpy ).createElement( anyString(), anyInt(), anyBoolean() );
  }

  @Test
  public void testFormatParameterValue_WithSupportedTypes() {

    String stringValue = "Test String";
    Number numberValue = 123;
    java.sql.Date dateValue = java.sql.Date.valueOf( LocalDate.now() );
    String[] stringArray = { "A", "B", "C" };
    Number[] numberArray = { 1, 2, 3 };
    String[] dateStrings = { "2025-04-17", "2023-12-25", "2024-01-01" };

    java.sql.Date[] sqlDateArray = Arrays.stream( dateStrings )
      .map( LocalDate::parse ) // parse string to LocalDate
      .map( java.sql.Date::valueOf )    // convert LocalDate to java.sql.Date
      .toArray( java.sql.Date[]::new );

    java.util.Date[] utilDateArray = Arrays.stream( dateStrings )
      .map( LocalDate::parse )
      .map( ld -> java.util.Date.from( ld.atStartOfDay( ZoneId.systemDefault() ).toInstant() ) )
      .toArray( java.util.Date[]::new );

    assertEquals( stringValue, ExportReportUtils.formatParameterValue( stringValue ) );
    assertEquals( "123", ExportReportUtils.formatParameterValue( numberValue ) );
    assertNotNull( ExportReportUtils.formatParameterValue( dateValue ) );
    assertEquals( "A, B, C", ExportReportUtils.formatParameterValue( stringArray ) );
    assertEquals( "1, 2, 3", ExportReportUtils.formatParameterValue( numberArray ) );
    assertEquals( "2025-04-17, 2023-12-25, 2024-01-01", ExportReportUtils.formatParameterValue( sqlDateArray ) );
    assertEquals( "2025-04-17, 2023-12-25, 2024-01-01", ExportReportUtils.formatParameterValue( utilDateArray ) );
  }

  @Test( expected = IllegalArgumentException.class )
  public void testFormatParameterValue_WithUnsupportedType() {
    Object unsupportedValue = new Object();

    ExportReportUtils.formatParameterValue( unsupportedValue );
  }

  @Test
  public void testAddReportDetailsPage_NullReport() throws Exception {
    exportReportUtilsSpy.addReportDetailsPage( null, parameterContextMock );
    verifyNoInteractions( reportHeaderMock, readableFilterUtilMock, parameterContextMock );
  }

  @Test( expected = IllegalStateException.class )
  public void testAddReportDetailsPage_NullReportHeader() throws Exception {
    when( masterReportMock.getReportHeader() ).thenReturn( null );
    exportReportUtilsSpy.addReportDetailsPage( masterReportMock, parameterContextMock );
  }

  @Test
  public void testAddReportDetailsPage_NoConstraints() throws Exception {
    exportReportUtilsSpy.addReportDetailsPage( masterReportMock, parameterContextMock );
    verify( reportHeaderMock ).setPagebreakAfterPrint( true );
    verify( reportHeaderMock, times( 4 ) ).addElement( any( Element.class ) );
    verify( readableFilterUtilMock, times( 0 ) ).toHumanReadableFilter( "foobar", queryMock, masterReportMock );
  }

  @Test
  public void testAddReportDetailsPage_WithPrompts() throws Exception {
    ParameterDefinitionEntry param = mock( ParameterDefinitionEntry.class );
    when( param.getName() ).thenReturn( "p1" );
    when( param.getDefaultValue( any() ) ).thenReturn( "v1" );
    when( masterReportMock.getDataFactory() ).thenReturn( mock( CompoundDataFactory.class ) );
    when( masterReportMock.getQuery() ).thenReturn( "query" );
    ReportParameterDefinition paramDef = mock( ReportParameterDefinition.class );
    when( paramDef.getParameterDefinitions() ).thenReturn( new ParameterDefinitionEntry[] { param } );
    when( masterReportMock.getParameterDefinition() ).thenReturn( paramDef );

    exportReportUtilsSpy.addReportDetailsPage( masterReportMock, parameterContextMock );

    verify( reportHeaderMock, atLeastOnce() ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddFilters_NullDescription() {
    exportReportUtilsSpy.addFilters( reportHeaderMock );
    verify( reportHeaderMock ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddFiltersFromQuery_EmptyDescription() throws ParseException, EvaluationException {
    when( queryMock.getConstraints() ).thenReturn( Collections.singletonList( constraintMock ) );
    when( constraintMock.getFormula() ).thenReturn( "someFormula" );

    exportReportUtilsSpy.addFiltersFromQuery( reportHeaderMock, queryMock, masterReportMock );

    verify( reportHeaderMock ).addElement( any( Element.class ) );
    verify( readableFilterUtilMock, times( 0 ) ).toHumanReadableFilter( "someFormula", queryMock, masterReportMock );
  }

  @Test
  public void testAddPrompts_NoParameters() throws Exception {
    exportReportUtilsSpy.addPrompts( reportHeaderMock, masterReportMock, parameterContextMock );

    verify( reportHeaderMock ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddPrompts_WithNullParameterContext() throws Exception {
    exportReportUtilsSpy.addPrompts( reportHeaderMock, masterReportMock, null );

    verify( reportHeaderMock ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddPrompts_WithNullParameterDefinitions() throws Exception {
    ReportParameterDefinition paramDef = mock( ReportParameterDefinition.class );
    when( paramDef.getParameterDefinitions() ).thenReturn( null );
    when( masterReportMock.getParameterDefinition() ).thenReturn( paramDef );

    exportReportUtilsSpy.addPrompts( reportHeaderMock, masterReportMock, parameterContextMock );

    verify( reportHeaderMock ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddPrompts_WithParameterValueNull() throws Exception {
    ParameterDefinitionEntry param = mock( ParameterDefinitionEntry.class );
    when( param.getName() ).thenReturn( "p2" );
    when( param.getDefaultValue( any() ) ).thenReturn( null );
    ReportParameterDefinition paramDef = mock( ReportParameterDefinition.class );
    when( paramDef.getParameterDefinitions() ).thenReturn( new ParameterDefinitionEntry[] { param } );
    when( masterReportMock.getParameterDefinition() ).thenReturn( paramDef );

    exportReportUtilsSpy.addPrompts( reportHeaderMock, masterReportMock, parameterContextMock );

    verify( reportHeaderMock, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddElementToReportHeader_NullElement() {
    exportReportUtilsSpy.addElementToReportHeader( null, reportHeaderMock );
    verify( reportHeaderMock, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddElementToReportHeader_NullHeader() {
    exportReportUtilsSpy.addElementToReportHeader( mockElement, null );
    verify( reportHeaderMock, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testAddElementToReportHeader_DoesNotAddWhenBothNull() {
    exportReportUtilsSpy.addElementToReportHeader( null, null );
    // Should not throw and should not interact
    verify( reportHeaderMock, never() ).addElement( any( Element.class ) );
  }

  @Test
  public void testCreateElement_WithVariousInputs() {
    Element e1 = exportReportUtilsSpy.createElement( "Text", 14, false );
    Element e2 = exportReportUtilsSpy.createElement( "Bold", 16, true );
    assertNotNull( e1 );
    assertNotNull( e2 );
  }
}
