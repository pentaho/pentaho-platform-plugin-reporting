/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.drilldown;

import junit.framework.TestCase;
import org.mockito.Mockito;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.function.ExpressionRuntime;
import org.pentaho.reporting.engine.classic.core.function.ProcessingContext;
import org.pentaho.reporting.engine.classic.core.function.ReportFormulaContext;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.FormulaContext;
import org.pentaho.reporting.libraries.formula.function.ParameterCallback;
import org.pentaho.reporting.libraries.formula.lvalues.TypeValuePair;
import org.pentaho.reporting.libraries.formula.typing.ArrayCallback;
import org.pentaho.reporting.libraries.formula.typing.TypeRegistry;
import org.pentaho.reporting.libraries.formula.typing.coretypes.LogicalType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class IsContentLinkFunctionTest extends TestCase {
  IsContentLinkFunction function, functionSpy;

  protected void setUp() {
    function = new IsContentLinkFunction();
    functionSpy = spy( function );
  }

  public void testGetCanonicalName() throws Exception {
    assertEquals( "ISCONTENTLINK", functionSpy.getCanonicalName() ); //$NON-NLS-1$
  }

  public void testEvaluate() throws Exception {
    FormulaContext formulacontext = mock( FormulaContext.class );
    ParameterCallback parameters = mock( ParameterCallback.class );

    try {
      functionSpy.evaluate( formulacontext, parameters );
    } catch ( EvaluationException ex ) {
      assertTrue( true );
    }

    doReturn( 1 ).when( parameters ).getParameterCount();
    TypeRegistry typeRegistry = mock( TypeRegistry.class );
    doReturn( typeRegistry ).when( formulacontext ).getTypeRegistry();
    ArrayCallback callback = mock( ArrayCallback.class );
    doReturn( callback ).when( typeRegistry )
        .convertToArray( Mockito.<org.pentaho.reporting.libraries.formula.typing.Type>any(), any() );

    try {
      functionSpy.evaluate( formulacontext, parameters );
    } catch ( EvaluationException ex ) {
      assertTrue( true );
    }

    doReturn( 2 ).when( callback ).getColumnCount();
    TypeValuePair result = functionSpy.evaluate( formulacontext, parameters );
    assertEquals( false, result.getValue() );

    ReportFormulaContext reportFormulaContext = mock( ReportFormulaContext.class );
    doReturn( typeRegistry ).when( reportFormulaContext ).getTypeRegistry();
    doReturn( "dummyExportType" ).when( reportFormulaContext ).getExportType();
    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertEquals( false, result.getValue() );

    doReturn( "table/html" ).when( reportFormulaContext ).getExportType();
    ExpressionRuntime runtime = mock( ExpressionRuntime.class );
    doReturn( runtime ).when( reportFormulaContext ).getRuntime();
    ProcessingContext processingContext = mock( ProcessingContext.class );
    doReturn( processingContext ).when( runtime ).getProcessingContext();
    ReportEnvironment reportEnvironment = mock( ReportEnvironment.class );
    doReturn( reportEnvironment ).when( processingContext ).getEnvironment();

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertEquals( false, result.getValue() );

    doReturn( 1 ).when( callback ).getRowCount();
    doReturn( "paramValue" ).when( callback ).getValue( anyInt(), anyInt() );
    doReturn( "dummyValue" ).when( reportEnvironment ).getEnvironmentProperty( anyString() );
    doReturn( reportEnvironment ).when( processingContext ).getEnvironment();

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertEquals( false, result.getValue() );

    doReturn( reportEnvironment ).when( processingContext ).getEnvironment();
    doReturn( "paramValue" ).when( reportEnvironment ).getEnvironmentProperty( anyString() );

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertEquals( true, result.getValue() );
    assertTrue( result.getType() instanceof LogicalType );

    Object[][] o = new Object[ 1 ][ 2 ];
    o[ 0 ][ 0 ] = "paramValue";

    doReturn( o ).when( parameters ).getValue( 0 );
    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertEquals( true, result.getValue() );
  }
}
