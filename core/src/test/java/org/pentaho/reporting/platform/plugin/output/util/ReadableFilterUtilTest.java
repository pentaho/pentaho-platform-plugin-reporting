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
import org.mockito.Mockito;
import org.pentaho.metadata.model.LogicalColumn;
import org.pentaho.metadata.model.LogicalModel;
import org.pentaho.metadata.model.concept.types.DataType;
import org.pentaho.metadata.query.model.Query;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactory;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.parser.ParseException;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReadableFilterUtilTest {
  private ReadableFilterUtil readableFilterUtilSpy;
  private Query queryMock;
  private LogicalModel logicalModelMock;
  private LogicalColumn logicalColumnMock;
  private MasterReport masterReportMock;
  private IMetadataDomainRepository metadataDomainRepositoryMock;

  @Before
  public void setUp() {
    readableFilterUtilSpy = Mockito.spy( ReadableFilterUtil.getInstance() );
    queryMock = mock( Query.class );
    logicalModelMock = mock( LogicalModel.class );
    logicalColumnMock = mock( LogicalColumn.class );
    masterReportMock = mock( MasterReport.class );
    metadataDomainRepositoryMock = mock( IMetadataDomainRepository.class );

    when( queryMock.getLogicalModel() ).thenReturn( logicalModelMock );

    // Mock ReportEnvironment and locale
    ReportEnvironment env = mock( ReportEnvironment.class );
    when( masterReportMock.getReportEnvironment() ).thenReturn( env );
    when( env.getLocale() ).thenReturn( Locale.US );

    // Mock LogicalColumn lookups and names
    when( logicalModelMock.findLogicalColumn( anyString() ) ).thenAnswer( invocation -> {
      String colName = invocation.getArgument( 0 );
      LogicalColumn col = mock( LogicalColumn.class );
      switch ( colName ) {
        case "BC_PRODUCTS_PRODUCTLINE":
          when( col.getName( anyString() ) ).thenReturn( "Product Line" );
          when( col.getDataType() ).thenReturn( DataType.STRING );
          break;
        case "BC_CUSTOMER_W_TER_TERRITORY":
          when( col.getName( anyString() ) ).thenReturn( "Territory" );
          when( col.getDataType() ).thenReturn( DataType.STRING );
          break;
        case "BC_CUSTOMER_W_TER_COUNTRY":
          when( col.getName( anyString() ) ).thenReturn( "Country" );
          when( col.getDataType() ).thenReturn( DataType.STRING );
          break;
        case "BC_PRODUCTS_BUYPRICE":
          when( col.getName( anyString() ) ).thenReturn( "Buy Price" );
          when( col.getDataType() ).thenReturn( DataType.NUMERIC );
          break;
        case "BC_ORDERS_REQUIREDDATE":
          when( col.getName( anyString() ) ).thenReturn( "Required Date" );
          when( col.getDataType() ).thenReturn( DataType.DATE );
          break;
        case "BC_PAYMENTS_PAYMENTDATE":
          when( col.getName( anyString() ) ).thenReturn( "Payment Date" );
          when( col.getDataType() ).thenReturn( DataType.DATE );
          break;
        case "BC_CUSTOMER_W_TER_CITY":
          when( col.getName( anyString() ) ).thenReturn( "City" );
          when( col.getDataType() ).thenReturn( DataType.STRING );
          break;
        case "BC_ORDERS_STATUS":
          when( col.getName( anyString() ) ).thenReturn( "Status" );
          when( col.getDataType() ).thenReturn( DataType.STRING );
          break;
        case "BC_ORDERDETAILS_TOTAL":
          when( col.getName( anyString() ) ).thenReturn( "Total" );
          when( col.getDataType() ).thenReturn( DataType.NUMERIC );
          break;
        default:
          when( col.getName( anyString() ) ).thenReturn( colName );
          when( col.getDataType() ).thenReturn( DataType.UNKNOWN );
      }
      return col;
    } );
  }

  @Test
  public void testRemoveDateValue() throws ParseException, EvaluationException {
    String mql = "DATEVALUE(\"2024-01-01\")";
    String result = readableFilterUtilSpy.toHumanReadableFilter( mql, queryMock, masterReportMock );
    assertTrue( result.contains( "2024-01-01" ) );
  }

  @Test
  public void testToHumanReadableFilter_NullOrBlank() throws Exception {
    assertEquals( "", readableFilterUtilSpy.toHumanReadableFilter( null, queryMock, masterReportMock ) );
    assertEquals( "", readableFilterUtilSpy.toHumanReadableFilter( "", queryMock, masterReportMock ) );
    assertEquals( "", readableFilterUtilSpy.toHumanReadableFilter( "   ", queryMock, masterReportMock ) );
  }

  @Test
  public void testToHumanReadableFilter_ValidSimpleMql() throws Exception {
    String mql1 = "NOT(IN([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE];\"APAC\";\"EMEA\"))";
    String mql2 = "AND(ENDSWITH([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE];\"A\");BEGINSWITH"
      + "([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_COUNTRY.NONE];\"A\");ISNA([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_CITY"
      + ".NONE]);NOT(ISNA([CAT_PRODUCTS.BC_PRODUCTS_PRODUCTLINE.NONE]));NOT(CONTAINS([CAT_ORDERS.BC_ORDERS_STATUS"
      + ".NONE];\"z\")))";
    String mql3 = "AND(CONTAINS([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE];\"A\");[CAT_ORDERS"
      + ".BC_ORDERDETAILS_TOTAL.SUM] >=45;[CAT_PAYMENTS.BC_PAYMENTS_PAYMENTDATE.NONE] <DATEVALUE(\"2025-05-05\"))";
    String mql4 =
      "OR([CAT_ORDERS.BC_ORDERDETAILS_TOTAL.SUM] <=45;[CAT_PAYMENTS.BC_PAYMENTS_PAYMENTDATE.NONE] >DATEVALUE"
        + "(\"2025-05-05\"))";

    String result1 = readableFilterUtilSpy.toHumanReadableFilter( mql1, queryMock, masterReportMock );
    String result2 = readableFilterUtilSpy.toHumanReadableFilter( mql2, queryMock, masterReportMock );
    String result3 = readableFilterUtilSpy.toHumanReadableFilter( mql3, queryMock, masterReportMock );
    String result4 = readableFilterUtilSpy.toHumanReadableFilter( mql4, queryMock, masterReportMock );

    assertEquals( "Territory Excludes (\"APAC\", \"EMEA\")", result1 );
    assertEquals( "Territory Ends with \"A\" AND Country Begins with \"A\" AND City Is null AND Product Line Is not "
      + "null AND Status Doesn't contain \"z\"", result2 );
    assertEquals( "Territory Contains \"A\" AND Total (SUM) Greater Than or Equals 45 AND Payment Date Before "
      + "\"2025-05-05\"", result3 );
    assertEquals( "Total (SUM) Less Than or Equals 45 OR Payment Date After \"2025-05-05\"", result4 );
  }

  @Test
  public void testToHumanReadableFilter_ValidComplexMql() throws Exception {
    String mql = "AND(OR(EQUALS([CAT_PRODUCTS.BC_PRODUCTS_PRODUCTLINE.NONE];[param:Product Line]);NOT(IN"
      + "([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_COUNTRY.NONE];\"Australia\";\"Belgium\";\"Canada\";\"Finland\")));IN"
      + "([BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE];[param:Territory]);[CAT_PRODUCTS.BC_PRODUCTS_BUYPRICE"
      + ".SUM] <10000;EQUALS([CAT_ORDERS.BC_ORDERS_REQUIREDDATE.NONE];DATEVALUE([param:Required Date]));[CAT_PAYMENTS"
      + ".BC_PAYMENTS_PAYMENTDATE.NONE] <=DATEVALUE([param:payDate]))";

    String result = readableFilterUtilSpy.toHumanReadableFilter( mql, queryMock, masterReportMock );

    assertEquals( "((Product Line Exactly matches value of Prompt Product Line OR Country Excludes (\"Australia\", "
      + "\"Belgium\", \"Canada\", \"Finland\")) AND Territory Includes (value of Prompt Territory) "
      + "AND Buy Price (SUM) Less Than 10000 AND Required Date On "
      + "value of Prompt Required Date AND Payment Date On or before value of Prompt payDate", result );
  }

  @Test
  public void testExtractQueryFromReport_ReturnsQuery() throws Exception {
    CompoundDataFactory dataFactory = mock( CompoundDataFactory.class );
    PmdDataFactory pmdDataFactory = mock( PmdDataFactory.class );
    when( masterReportMock.getDataFactory() ).thenReturn( dataFactory );
    when( masterReportMock.getQuery() ).thenReturn( "myQuery" );
    when( dataFactory.getDataFactoryForQuery( anyString() ) ).thenReturn( pmdDataFactory );
    when( pmdDataFactory.getQuery( anyString() ) ).thenReturn( "<mql>...</mql>" );

    doReturn( metadataDomainRepositoryMock ).when( readableFilterUtilSpy ).getMetadataRepository();
    doReturn( queryMock ).when( readableFilterUtilSpy ).parseQueryFromMql( anyString() );

    Query result = readableFilterUtilSpy.extractQueryFromReport( masterReportMock );
    assertEquals( queryMock, result );
  }

  @Test
  public void testExtractQueryFromReport_NullReportThrows() {
    assertThrows( IllegalArgumentException.class, () -> readableFilterUtilSpy.extractQueryFromReport( null ) );
  }

  @Test
  public void testParseQueryFromMql_NullOrEmptyThrows() {
    assertThrows( IllegalArgumentException.class, () -> readableFilterUtilSpy.parseQueryFromMql( null ) );
    assertThrows( IllegalArgumentException.class, () -> readableFilterUtilSpy.parseQueryFromMql( "" ) );
  }

  @Test
  public void testExtractQueryFromReport_NoPmdDataFactoryReturnsNull() throws Exception {
    CompoundDataFactory dataFactory = mock( CompoundDataFactory.class );
    DataFactory notPmd = mock( DataFactory.class );
    when( masterReportMock.getDataFactory() ).thenReturn( dataFactory );
    when( masterReportMock.getQuery() ).thenReturn( "myQuery" );
    when( dataFactory.getDataFactoryForQuery( anyString() ) ).thenReturn( notPmd );

    Query result = readableFilterUtilSpy.extractQueryFromReport( masterReportMock );
    assertNull( result );
  }

  @Test
  public void testRemoveDateValue_NullOrBlank() throws Exception {
    // Use reflection to call private method
    var method = ReadableFilterUtil.class.getDeclaredMethod( "removeDateValue", String.class );
    method.setAccessible( true );
    assertEquals( "", method.invoke( readableFilterUtilSpy, (Object) null ) );
    assertEquals( "", method.invoke( readableFilterUtilSpy, "" ) );
    assertEquals( "", method.invoke( readableFilterUtilSpy, "   " ) );
  }

  @Test
  public void testCleanCol_WithThreeParts() {
    when( logicalModelMock.findLogicalColumn( anyString() ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getName( anyString() ) ).thenReturn( "My Column" );

    String col = readableFilterUtilSpy.cleanCol( "[model.field1.SUM]", queryMock, masterReportMock );
    assertEquals( "My Column (SUM)", col );
  }

  @Test public void testCleanCol_WithTwoParts() {
    when( logicalModelMock.findLogicalColumn( anyString() ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getName( anyString() ) ).thenReturn( "My Column" );

    String col = readableFilterUtilSpy.cleanCol( "[model.field1]", queryMock, masterReportMock );
    assertEquals( "My Column", col );
  }

  @Test
  public void testGetFriendlyComparisonOperator_StringAndDate() {
    // DataType.STRING
    assertEquals( "EXACTLY_MATCHES", readableFilterUtilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.STRING ) );
    assertEquals( "MORE_THAN", readableFilterUtilSpy.getFriendlyComparisonOperator( ">", DataType.STRING ) );
    assertEquals( "LESS_THAN", readableFilterUtilSpy.getFriendlyComparisonOperator( "<", DataType.STRING ) );
    assertEquals( "MORE_THAN_OR_EQUAL", readableFilterUtilSpy.getFriendlyComparisonOperator( ">=", DataType.STRING ) );
    assertEquals( "LESS_THAN_OR_EQUAL", readableFilterUtilSpy.getFriendlyComparisonOperator( "<=", DataType.STRING ) );
    // DataType.DATE
    assertEquals( "ON", readableFilterUtilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.DATE ) );
    assertEquals( "AFTER", readableFilterUtilSpy.getFriendlyComparisonOperator( ">", DataType.DATE ) );
    assertEquals( "BEFORE", readableFilterUtilSpy.getFriendlyComparisonOperator( "<", DataType.DATE ) );
    assertEquals( "ON_OR_AFTER", readableFilterUtilSpy.getFriendlyComparisonOperator( ">=", DataType.DATE ) );
    assertEquals( "ON_OR_BEFORE", readableFilterUtilSpy.getFriendlyComparisonOperator( "<=", DataType.DATE ) );
    // DataType.NUMERIC
    assertEquals( "EQUALS", readableFilterUtilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.NUMERIC ) );
    assertEquals( "MORE_THAN", readableFilterUtilSpy.getFriendlyComparisonOperator( ">", DataType.NUMERIC ) );
    assertEquals( "LESS_THAN", readableFilterUtilSpy.getFriendlyComparisonOperator( "<", DataType.NUMERIC ) );
    assertEquals( "MORE_THAN_OR_EQUAL", readableFilterUtilSpy.getFriendlyComparisonOperator( ">=", DataType.NUMERIC ) );
    assertEquals( "LESS_THAN_OR_EQUAL", readableFilterUtilSpy.getFriendlyComparisonOperator( "<=", DataType.NUMERIC ) );
    // Unknown operator
    assertEquals( "SOMETHING", readableFilterUtilSpy.getFriendlyComparisonOperator( "SOMETHING", DataType.STRING ) );
    // Null dataType
    assertEquals( "EQUALS", readableFilterUtilSpy.getFriendlyComparisonOperator( "EQUALS", null ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfFieldNameIsNullOrShort() {
    assertEquals( DataType.UNKNOWN, readableFilterUtilSpy.getFieldDataType( "", queryMock ) );
    assertEquals( DataType.UNKNOWN, readableFilterUtilSpy.getFieldDataType( "foo", queryMock ) );
    assertEquals( DataType.UNKNOWN, readableFilterUtilSpy.getFieldDataType( "[foo]", queryMock ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfLogicalColumnIsNull() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( null );
    assertEquals( DataType.UNKNOWN, readableFilterUtilSpy.getFieldDataType( "foo.bar", queryMock ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfDataTypeIsNull() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getDataType() ).thenReturn( null );
    assertEquals( DataType.UNKNOWN, readableFilterUtilSpy.getFieldDataType( "foo.bar", queryMock ) );
  }

  @Test
  public void testGetFieldDataType_returnsCorrectDataType() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getDataType() ).thenReturn( DataType.DATE );
    assertEquals( DataType.DATE, readableFilterUtilSpy.getFieldDataType( "foo.bar", queryMock ) );
    when( logicalColumnMock.getDataType() ).thenReturn( DataType.STRING );
    assertEquals( DataType.STRING, readableFilterUtilSpy.getFieldDataType( "foo.bar", queryMock ) );
  }
}
