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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.drilldown;

import java.util.HashSet;
import java.util.Set;

import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.function.ReportFormulaContext;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.libraries.base.util.CSVTokenizer;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.FormulaContext;
import org.pentaho.reporting.libraries.formula.LibFormulaErrorValue;
import org.pentaho.reporting.libraries.formula.function.Function;
import org.pentaho.reporting.libraries.formula.function.ParameterCallback;
import org.pentaho.reporting.libraries.formula.lvalues.TypeValuePair;
import org.pentaho.reporting.libraries.formula.typing.ArrayCallback;
import org.pentaho.reporting.libraries.formula.typing.Type;
import org.pentaho.reporting.libraries.formula.typing.coretypes.LogicalType;

public class IsContentLinkFunction implements Function {
  public IsContentLinkFunction() {
  }

  public String getCanonicalName() {
    return "ISCONTENTLINK";
  }

  public TypeValuePair evaluate( final FormulaContext context, final ParameterCallback parameters )
    throws EvaluationException {
    if ( parameters.getParameterCount() != 1 ) {
      throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_ARGUMENTS_VALUE );
    }

    final HashSet<String> params = new HashSet<String>();
    final Object o = parameters.getValue( 0 );
    if ( o instanceof Object[][] ) {
      final Object[][] o2 = (Object[][]) o;
      for ( int i = 0; i < o2.length; i++ ) {
        final Object[] values = o2[i];
        if ( values == null || values.length == 0 ) {
          throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_ILLEGAL_ARRAY_VALUE );
        }
        params.add( String.valueOf( values[0] ) );
      }
    } else {
      final Type type = parameters.getType( 0 );
      final ArrayCallback callback = context.getTypeRegistry().convertToArray( type, o );
      if ( callback.getColumnCount() != 2 ) {
        throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_ILLEGAL_ARRAY_VALUE );
      }
      for ( int i = 0, n = callback.getRowCount(); i < n; i++ ) {
        params.add( String.valueOf( callback.getValue( i, 0 ) ) );
      }
    }

    return new TypeValuePair( LogicalType.TYPE, isContentLink( context, params ) );
  }

  private Boolean isContentLink( final FormulaContext context, final Set<String> parameters ) {
    if ( ( context instanceof ReportFormulaContext ) == false ) {
      return Boolean.FALSE;
    }

    final ReportFormulaContext reportFormulaContext = (ReportFormulaContext) context;
    final String exportType = reportFormulaContext.getExportType();
    if ( exportType.startsWith( "table/html" ) == false || HtmlTableModule.ZIP_HTML_EXPORT_TYPE.equals( exportType ) ) {
      return Boolean.FALSE;
    }

    final ReportEnvironment environment = reportFormulaContext.getRuntime().getProcessingContext().getEnvironment();
    final Object clText = environment.getEnvironmentProperty( "contentLink" );
    if ( clText == null ) {
      return Boolean.FALSE;
    }
    final CSVTokenizer csvTokenizer = new CSVTokenizer( String.valueOf( clText ), ",", "\"" );
    while ( csvTokenizer.hasMoreTokens() ) {
      final String el = csvTokenizer.nextToken();
      if ( parameters.contains( el ) ) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }
}
