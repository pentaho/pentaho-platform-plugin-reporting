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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.function.ReportFormulaContext;
import org.pentaho.reporting.engine.classic.core.function.formula.QuoteTextFunction;
import org.pentaho.reporting.libraries.base.util.CSVTokenizer;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.FormulaContext;
import org.pentaho.reporting.libraries.formula.LibFormulaErrorValue;
import org.pentaho.reporting.libraries.formula.function.Function;
import org.pentaho.reporting.libraries.formula.function.ParameterCallback;
import org.pentaho.reporting.libraries.formula.lvalues.TypeValuePair;
import org.pentaho.reporting.libraries.formula.typing.ArrayCallback;
import org.pentaho.reporting.libraries.formula.typing.Sequence;
import org.pentaho.reporting.libraries.formula.typing.Type;
import org.pentaho.reporting.libraries.formula.typing.coretypes.TextType;
import org.pentaho.reporting.libraries.formula.typing.sequence.RecursiveSequence;

public class ContentLinkFunction implements Function {
  public ContentLinkFunction() {
  }

  public String getCanonicalName() {
    return "CONTENTLINK";
  }

  public TypeValuePair evaluate( final FormulaContext context, final ParameterCallback parameters )
    throws EvaluationException {
    final String[] contentLink = getContentLink( context );
    if ( contentLink.length == 0 ) {
      throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_NA_VALUE );
    }

    final Object o = parameters.getValue( 0 );
    final Type type = parameters.getType( 0 );
    final HashMap<String, Object[]> values = collectParameterValues( o, type, context );

    final StringBuilder builder = new StringBuilder();
    builder.append( "javascript:" );
    // window.parent.Dashboards.fireChange(PARAM, VALUE);"
    for ( int i = 0; i < contentLink.length; i++ ) {
      final String variable = contentLink[i];
      builder
          .append( "var wnd=window.parent;var slf;while(!(wnd.Dashboards && wnd.Dashboards.fireOutputParam) && "
             + "wnd.parent && wnd.parent !== wnd){slf=wnd;wnd=wnd.parent};wnd.Dashboards.fireOutputParam(slf," );
      builder.append( '\'' );
      builder.append( QuoteTextFunction.saveConvert( variable ) );
      builder.append( '\'' );
      builder.append( "," );

      final Object[] objects = values.get( variable );
      if ( objects == null || objects.length == 0 ) {
        builder.append( "null" );
      } else if ( objects.length == 1 ) {
        builder.append( '\'' );
        builder.append( QuoteTextFunction.saveConvert( String.valueOf( objects[0] ) ) );
        builder.append( '\'' );
      } else {
        builder.append( "new Array(" );
        for ( int j = 0; j < objects.length; j++ ) {
          if ( j != 0 ) {
            builder.append( "," );
          }
          builder.append( '\'' );
          builder.append( QuoteTextFunction.saveConvert( String.valueOf( objects[j] ) ) );
          builder.append( '\'' );
        }
        builder.append( ")" );
      }
      builder.append( ");" );
    }

    final String value = builder.toString();
    return new TypeValuePair( TextType.TYPE, value );
  }

  private String[] getContentLink( final FormulaContext context ) {
    if ( ( context instanceof ReportFormulaContext ) == false ) {
      return new String[0];
    }

    final ReportFormulaContext reportFormulaContext = (ReportFormulaContext) context;
    final ReportEnvironment environment = reportFormulaContext.getRuntime().getProcessingContext().getEnvironment();
    final Object clText = environment.getEnvironmentProperty( "contentLink" );
    if ( clText == null ) {
      return new String[0];
    }
    final CSVTokenizer csvTokenizer = new CSVTokenizer( String.valueOf( clText ), ",", "\"" );
    final LinkedHashSet<String> result = new LinkedHashSet<String>();
    while ( csvTokenizer.hasMoreTokens() ) {
      final String el = csvTokenizer.nextToken();
      result.add( el );
    }
    return result.toArray( new String[result.size()] );
  }

  private HashMap<String, Object[]> collectParameterValues( final Object o, final Type type,
      final FormulaContext context ) throws EvaluationException {
    final HashMap<String, Object[]> params = new HashMap<String, Object[]>();
    if ( o instanceof Object[][] ) {
      final Object[][] o2 = (Object[][]) o;
      for ( int i = 0; i < o2.length; i++ ) {
        final Object[] values = o2[i];
        if ( values == null || values.length < 2 ) {
          throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_ILLEGAL_ARRAY_VALUE );
        }
        final Object value = values[1];
        if ( value instanceof Object[] ) {
          params.put( String.valueOf( values[0] ), (Object[]) value );
        } else {
          params.put( String.valueOf( values[0] ), new Object[] { value } );
        }
      }
    } else {
      final ArrayCallback callback = context.getTypeRegistry().convertToArray( type, o );
      if ( callback.getColumnCount() != 2 ) {
        throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_ILLEGAL_ARRAY_VALUE );
      }
      for ( int i = 0, n = callback.getRowCount(); i < n; i++ ) {
        final Sequence sequenceRaw =
            context.getTypeRegistry().convertToSequence( callback.getType( i, 1 ), callback.getValue( i, 1 ) );
        if ( sequenceRaw == null ) {
          throw EvaluationException.getInstance( LibFormulaErrorValue.ERROR_NA_VALUE );
        }
        final RecursiveSequence sequence = new RecursiveSequence( sequenceRaw, context );
        final ArrayList<Object> retval = new ArrayList<Object>();
        while ( sequence.hasNext() ) {
          final Object s = sequence.next();
          retval.add( s );
        }

        params.put( String.valueOf( callback.getValue( i, 0 ) ), retval.toArray() );
      }
    }
    return params;
  }
}
