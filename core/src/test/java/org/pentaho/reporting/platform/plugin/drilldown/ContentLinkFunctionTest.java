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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ContentLinkFunctionTest extends TestCase {
  ContentLinkFunction function, functionSpy;

  protected void setUp() {
    function = new ContentLinkFunction();
    functionSpy = spy( function );
  }

  public void testGetCanonicalName() throws Exception {
    assertEquals( "CONTENTLINK", function.getCanonicalName() ); //$NON-NLS-1$
  }

  public void testEvaluate() throws Exception {
    TypeValuePair result;
    FormulaContext formulacontext = mock( FormulaContext.class );
    ParameterCallback parameters = mock( ParameterCallback.class );

    try {
      functionSpy.evaluate( formulacontext, parameters );
    } catch ( EvaluationException ex ) {
      assertTrue( true );
    }

    ReportFormulaContext reportFormulaContext = mock( ReportFormulaContext.class );
    ExpressionRuntime runtime = mock( ExpressionRuntime.class );
    ProcessingContext context = mock( ProcessingContext.class );
    ReportEnvironment environment = mock( ReportEnvironment.class );
    doReturn( environment ).when( context ).getEnvironment();
    doReturn( context ).when( runtime ).getProcessingContext();
    doReturn( runtime ).when( reportFormulaContext ).getRuntime();
    doReturn( null ).when( environment ).getEnvironmentProperty( anyString() );

    try {
      functionSpy.evaluate( reportFormulaContext, parameters );
    } catch ( EvaluationException ex ) {
      assertTrue( true );
    }

    doReturn( "testValue1" ).when( environment ).getEnvironmentProperty( anyString() );
    TypeRegistry typeRegistry = mock( TypeRegistry.class );
    doReturn( typeRegistry ).when( reportFormulaContext ).getTypeRegistry();
    ArrayCallback callback = mock( ArrayCallback.class );
    doReturn( callback ).when( typeRegistry )
        .convertToArray( Mockito.<org.pentaho.reporting.libraries.formula.typing.Type>any(), any() );

    try {
      functionSpy.evaluate( reportFormulaContext, parameters );
    } catch ( EvaluationException ex ) {
      assertTrue( true );
    }

    doReturn( 2 ).when( callback ).getColumnCount();

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertTrue(
        result.getType().toString().contains( "org.pentaho.reporting.libraries.formula.typing.coretypes.TextType" ) );
    assertEquals(
        "javascript:var wnd=window.parent;var slf;while(!(wnd.pentahoDashboardController && wnd.pentahoDashboardController.fireOutputParam) "
            + "&& wnd.parent && wnd.parent !== wnd){slf=wnd;wnd=wnd.parent};wnd.pentahoDashboardController.fireOutputParam(slf,"
            + "'testValue1',null);",
        result.getValue() );

    Object[][] o = new Object[ 1 ][ 2 ];
    o[ 0 ][ 0 ] = "testValue1";
    o[ 0 ][ 1 ] = "testValue2";
    doReturn( o ).when( parameters ).getValue( 0 );

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertTrue(
        result.getType().toString().contains( "org.pentaho.reporting.libraries.formula.typing.coretypes.TextType" ) );
    assertEquals(
        "javascript:var wnd=window.parent;var slf;while(!(wnd.pentahoDashboardController && wnd.pentahoDashboardController.fireOutputParam) && wnd"
            + ".parent && wnd.parent !== wnd){slf=wnd;wnd=wnd.parent};wnd.pentahoDashboardController.fireOutputParam(slf,"
            + "'testValue1','testValue2');",
        result.getValue() );

    o = new Object[ 1 ][ 2 ];
    o[ 0 ][ 0 ] = "testValue1";
    Object[] o2 = new Object[ 2 ];
    o2[ 0 ] = "testValue2";
    o2[ 1 ] = "testValue3";
    o[ 0 ][ 1 ] = o2;
    doReturn( o ).when( parameters ).getValue( 0 );

    result = functionSpy.evaluate( reportFormulaContext, parameters );
    assertTrue(
        result.getType().toString().contains( "org.pentaho.reporting.libraries.formula.typing.coretypes.TextType" ) );
    assertEquals(
        "javascript:var wnd=window.parent;var slf;while(!(wnd.pentahoDashboardController && wnd.pentahoDashboardController.fireOutputParam) && wnd"
            + ".parent && wnd.parent !== wnd){slf=wnd;wnd=wnd.parent};wnd.pentahoDashboardController.fireOutputParam(slf,"
            + "'testValue1',new Array('testValue2','testValue3'));",
        result.getValue() );
  }
}
