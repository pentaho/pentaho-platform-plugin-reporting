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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.drilldown;

import junit.framework.TestCase;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
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
        .convertToArray( any( org.pentaho.reporting.libraries.formula.typing.Type.class ), anyObject() );

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
