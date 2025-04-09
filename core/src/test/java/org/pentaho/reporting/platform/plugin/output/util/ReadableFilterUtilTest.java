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

  private ReadableFilterUtil utilSpy;
  private Query queryMock;
  private LogicalModel logicalModelMock;
  private LogicalColumn logicalColumnMock;
  private MasterReport masterReportMock;
  private IMetadataDomainRepository metadataDomainRepositoryMock;

  @Before
  public void setUp() {
    utilSpy = Mockito.spy( new ReadableFilterUtil() );
    queryMock = mock( Query.class );
    logicalModelMock = mock( LogicalModel.class );
    logicalColumnMock = mock( LogicalColumn.class );
    masterReportMock = mock( MasterReport.class );
    metadataDomainRepositoryMock = mock( IMetadataDomainRepository.class );
    utilSpy.setQuery( queryMock );
    when( queryMock.getLogicalModel() ).thenReturn( logicalModelMock );

    doReturn( "Product Line" ).when( utilSpy ).cleanCol( "CAT_PRODUCTS.BC_PRODUCTS_PRODUCTLINE.NONE" );
    doReturn( "Territory" ).when( utilSpy ).cleanCol( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE" );
    doReturn( "Country" ).when( utilSpy ).cleanCol( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_COUNTRY.NONE" );
    doReturn( "Buy Price (SUM)" ).when( utilSpy ).cleanCol( "CAT_PRODUCTS.BC_PRODUCTS_BUYPRICE.SUM" );
    doReturn( "Required Date" ).when( utilSpy ).cleanCol( "CAT_ORDERS.BC_ORDERS_REQUIREDDATE.NONE" );
    doReturn( "Payment Date" ).when( utilSpy ).cleanCol( "CAT_PAYMENTS.BC_PAYMENTS_PAYMENTDATE.NONE" );
    doReturn( "City" ).when( utilSpy ).cleanCol( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_CITY.NONE" );
    doReturn( "Status" ).when( utilSpy ).cleanCol( "CAT_ORDERS.BC_ORDERS_STATUS.NONE" );
    doReturn( "Total (SUM)" ).when( utilSpy ).cleanCol( "CAT_ORDERS.BC_ORDERDETAILS_TOTAL.SUM" );

    doReturn( DataType.STRING ).when( utilSpy ).getFieldDataType( "CAT_PRODUCTS.BC_PRODUCTS_PRODUCTLINE.NONE" );
    doReturn( DataType.STRING ).when( utilSpy )
      .getFieldDataType( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_TERRITORY.NONE" );
    doReturn( DataType.STRING ).when( utilSpy ).getFieldDataType( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_COUNTRY.NONE" );
    doReturn( DataType.NUMERIC ).when( utilSpy ).getFieldDataType( "CAT_ORDERS.BC_ORDERDETAILS_TOTAL.SUM" );
    doReturn( DataType.NUMERIC ).when( utilSpy ).getFieldDataType( "CAT_PRODUCTS.BC_PRODUCTS_BUYPRICE.SUM" );
    doReturn( DataType.DATE ).when( utilSpy ).getFieldDataType( "CAT_ORDERS.BC_ORDERS_REQUIREDDATE.NONE" );
    doReturn( DataType.DATE ).when( utilSpy ).getFieldDataType( "CAT_PAYMENTS.BC_PAYMENTS_PAYMENTDATE.NONE" );
    doReturn( DataType.STRING ).when( utilSpy ).getFieldDataType( "BC_CUSTOMER_W_TER_.BC_CUSTOMER_W_TER_CITY.NONE" );
    doReturn( DataType.STRING ).when( utilSpy ).getFieldDataType( "CAT_ORDERS.BC_ORDERS_STATUS.NONE" );

  }

  @Test
  public void testRemoveDateValue() throws ParseException, EvaluationException {
    String mql = "DATEVALUE(\"2024-01-01\")";
    String result = utilSpy.toHumanReadableFilter( mql );
    assertTrue( result.contains( "2024-01-01" ) );
  }

  @Test
  public void testToHumanReadableFilter_NullOrBlank() throws Exception {
    assertEquals( "", utilSpy.toHumanReadableFilter( null ) );
    assertEquals( "", utilSpy.toHumanReadableFilter( "" ) );
    assertEquals( "", utilSpy.toHumanReadableFilter( "   " ) );
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

    String result1 = utilSpy.toHumanReadableFilter( mql1 );
    String result2 = utilSpy.toHumanReadableFilter( mql2 );
    String result3 = utilSpy.toHumanReadableFilter( mql3 );
    String result4 = utilSpy.toHumanReadableFilter( mql4 );

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

    String result = utilSpy.toHumanReadableFilter( mql );

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

    doReturn( metadataDomainRepositoryMock ).when( utilSpy ).getMetadataRepository();
    doReturn( queryMock ).when( utilSpy ).parseQueryFromMql( anyString() );

    Query result = utilSpy.extractQueryFromReport( masterReportMock );
    assertEquals( queryMock, result );
  }

  @Test
  public void testExtractQueryFromReport_NullReportThrows() {
    assertThrows( IllegalArgumentException.class, () -> utilSpy.extractQueryFromReport( null ) );
  }

  @Test
  public void testGetAndSetQuery() {
    utilSpy.setQuery( queryMock );
    assertEquals( queryMock, utilSpy.getQuery() );
  }

  @Test
  public void testGetAndSetReport() {
    utilSpy.setReport( masterReportMock );
    assertEquals( masterReportMock, utilSpy.getReport() );
  }

  @Test
  public void testParseQueryFromMql_NullOrEmptyThrows() {
    assertThrows( IllegalArgumentException.class, () -> utilSpy.parseQueryFromMql( null ) );
    assertThrows( IllegalArgumentException.class, () -> utilSpy.parseQueryFromMql( "" ) );
  }

  @Test
  public void testExtractQueryFromReport_NoPmdDataFactoryReturnsNull() throws Exception {
    CompoundDataFactory dataFactory = mock( CompoundDataFactory.class );
    DataFactory notPmd = mock( DataFactory.class );
    when( masterReportMock.getDataFactory() ).thenReturn( dataFactory );
    when( masterReportMock.getQuery() ).thenReturn( "myQuery" );
    when( dataFactory.getDataFactoryForQuery( anyString() ) ).thenReturn( notPmd );

    Query result = utilSpy.extractQueryFromReport( masterReportMock );
    assertNull( result );
  }

  @Test
  public void testRemoveDateValue_NullOrBlank() throws Exception {
    // Use reflection to call private method
    var method = ReadableFilterUtil.class.getDeclaredMethod( "removeDateValue", String.class );
    method.setAccessible( true );
    assertEquals( "", method.invoke( utilSpy, (Object) null ) );
    assertEquals( "", method.invoke( utilSpy, "" ) );
    assertEquals( "", method.invoke( utilSpy, "   " ) );
  }

  @Test
  public void testCleanCol_WithThreeParts() {
    ReportEnvironment env = mock( ReportEnvironment.class );

    when( logicalModelMock.findLogicalColumn( anyString() ) ).thenReturn( logicalColumnMock );
    when( masterReportMock.getReportEnvironment() ).thenReturn( env );
    when( env.getLocale() ).thenReturn( Locale.US );
    when( logicalColumnMock.getName( anyString() ) ).thenReturn( "My Column" );

    utilSpy.setReport( masterReportMock );

    String col = utilSpy.cleanCol( "[model.field1.SUM]" );
    assertEquals( "My Column (SUM) ", col );
  }

  @Test public void testCleanCol_WithTwoParts() {
    ReportEnvironment env = mock( ReportEnvironment.class );

    when( logicalModelMock.findLogicalColumn( anyString() ) ).thenReturn( logicalColumnMock );
    when( masterReportMock.getReportEnvironment() ).thenReturn( env );
    when( env.getLocale() ).thenReturn( Locale.US );
    when( logicalColumnMock.getName( anyString() ) ).thenReturn( "My Column" );

    utilSpy.setReport( masterReportMock );

    String col = utilSpy.cleanCol( "[model.field1]" );
    assertEquals( "My Column", col );
  }

  @Test
  public void testGetFriendlyComparisonOperator_StringAndDate() {
    // DataType.STRING
    assertEquals( "EXACTLY_MATCHES", utilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.STRING ) );
    assertEquals( "MORE_THAN", utilSpy.getFriendlyComparisonOperator( ">", DataType.STRING ) );
    assertEquals( "LESS_THAN", utilSpy.getFriendlyComparisonOperator( "<", DataType.STRING ) );
    assertEquals( "MORE_THAN_OR_EQUAL", utilSpy.getFriendlyComparisonOperator( ">=", DataType.STRING ) );
    assertEquals( "LESS_THAN_OR_EQUAL", utilSpy.getFriendlyComparisonOperator( "<=", DataType.STRING ) );
    // DataType.DATE
    assertEquals( "ON", utilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.DATE ) );
    assertEquals( "AFTER", utilSpy.getFriendlyComparisonOperator( ">", DataType.DATE ) );
    assertEquals( "BEFORE", utilSpy.getFriendlyComparisonOperator( "<", DataType.DATE ) );
    assertEquals( "ON_OR_AFTER", utilSpy.getFriendlyComparisonOperator( ">=", DataType.DATE ) );
    assertEquals( "ON_OR_BEFORE", utilSpy.getFriendlyComparisonOperator( "<=", DataType.DATE ) );
    // DataType.NUMERIC
    assertEquals( "EQUALS", utilSpy.getFriendlyComparisonOperator( "EQUALS", DataType.NUMERIC ) );
    assertEquals( "MORE_THAN", utilSpy.getFriendlyComparisonOperator( ">", DataType.NUMERIC ) );
    assertEquals( "LESS_THAN", utilSpy.getFriendlyComparisonOperator( "<", DataType.NUMERIC ) );
    assertEquals( "MORE_THAN_OR_EQUAL", utilSpy.getFriendlyComparisonOperator( ">=", DataType.NUMERIC ) );
    assertEquals( "LESS_THAN_OR_EQUAL", utilSpy.getFriendlyComparisonOperator( "<=", DataType.NUMERIC ) );
    // Unknown operator
    assertEquals( "SOMETHING", utilSpy.getFriendlyComparisonOperator( "SOMETHING", DataType.STRING ) );
    // Null dataType
    assertEquals( "EQUALS", utilSpy.getFriendlyComparisonOperator( "EQUALS", null ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfFieldNameIsNullOrShort() {
    assertEquals( DataType.UNKNOWN, utilSpy.getFieldDataType( "" ) );
    assertEquals( DataType.UNKNOWN, utilSpy.getFieldDataType( "foo" ) );
    assertEquals( DataType.UNKNOWN, utilSpy.getFieldDataType( "[foo]" ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfLogicalColumnIsNull() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( null );
    assertEquals( DataType.UNKNOWN, utilSpy.getFieldDataType( "foo.bar" ) );
  }

  @Test
  public void testGetFieldDataType_returnsUnknownIfDataTypeIsNull() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getDataType() ).thenReturn( null );
    assertEquals( DataType.UNKNOWN, utilSpy.getFieldDataType( "foo.bar" ) );
  }

  @Test
  public void testGetFieldDataType_returnsCorrectDataType() {
    when( logicalModelMock.findLogicalColumn( "bar" ) ).thenReturn( logicalColumnMock );
    when( logicalColumnMock.getDataType() ).thenReturn( DataType.DATE );
    assertEquals( DataType.DATE, utilSpy.getFieldDataType( "foo.bar" ) );
    when( logicalColumnMock.getDataType() ).thenReturn( DataType.STRING );
    assertEquals( DataType.STRING, utilSpy.getFieldDataType( "foo.bar" ) );
  }
}
