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

import org.pentaho.metadata.query.model.Query;
import org.pentaho.metadata.query.model.util.QueryXmlHelper;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.pms.core.exception.PentahoMetadataException;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactory;
import org.pentaho.reporting.libraries.formula.DefaultFormulaContext;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.Formula;
import org.pentaho.reporting.libraries.formula.lvalues.ContextLookup;
import org.pentaho.reporting.libraries.formula.lvalues.FormulaFunction;
import org.pentaho.reporting.libraries.formula.lvalues.LValue;
import org.pentaho.reporting.libraries.formula.lvalues.StaticValue;
import org.pentaho.reporting.libraries.formula.lvalues.Term;
import org.pentaho.reporting.libraries.formula.parser.ParseException;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ReadableFilterUtil {
  private static final String DATEVALUE = "\\(?DATEVALUE\\((.*?)\\)\\)?";
  private Query query;
  private MasterReport report;

  public Query getQuery() {
    return query;
  }

  public void setQuery( Query query ) {
    this.query = query;
  }

  public MasterReport getReport() {
    return report;
  }

  public void setReport( MasterReport report ) {
    this.report = report;
  }

  private IMetadataDomainRepository getMetadataRepository() {
    return PentahoSystem.get( IMetadataDomainRepository.class, PentahoSessionHolder.getSession() );
  }

  public Query extractQueryFromReport( MasterReport report ) throws PentahoMetadataException {
    if ( report == null || report.getDataFactory() == null || report.getQuery() == null ) {
      throw new IllegalArgumentException( "Report, DataFactory, or Query cannot be null" );
    }
    setReport( report );


    CompoundDataFactory dataFactory = (CompoundDataFactory) report.getDataFactory();
    String[] tryNames = { report.getQuery(), "default" };
    for ( String dataSourceName : tryNames ) {
      DataFactory someDataFactory = dataFactory.getDataFactoryForQuery( dataSourceName );
      if ( someDataFactory instanceof PmdDataFactory ) {
        PmdDataFactory pmdDataFactory = (PmdDataFactory) someDataFactory;
        String mql = pmdDataFactory.getQuery( dataSourceName );
        setQuery( parseQueryFromMql( mql ) );
        return query;
      }
    }
    // If we reach here, we didn't find a PmdDataFactory or a valid MQL string
    return null;
  }

  private Query parseQueryFromMql( String mql ) throws PentahoMetadataException {
    if ( mql == null || mql.isEmpty() ) {
      throw new IllegalArgumentException( "MQL string cannot be null or empty" );
    }

    QueryXmlHelper helper = new QueryXmlHelper();
    IMetadataDomainRepository domainRepository = getMetadataRepository();

    return helper.fromXML( domainRepository, mql );

  }

  public String toHumanReadableFilter( String mql ) throws ParseException, EvaluationException {
    if ( mql == null || mql.isBlank() ) {
      return "";
    }

    Formula f = new Formula( mql );
    f.initialize( new DefaultFormulaContext() );
    LValue root = f.getRootReference();
    return toHumanReadable( root );
  }

  private String removeDateValue( String mql ) {
    if ( mql == null || mql.isBlank() ) {
      return "";
    }
    return mql.replaceAll( DATEVALUE, "$1" );
  }

  private String toHumanReadable( LValue value ) {
    return removeDateValue( createFriendlyFilterString( value, 0 ) );
  }

  private String createFriendlyFilterString( LValue value, int depth ) {
    if ( value instanceof Term ) {
      return handleTerm( (Term) value, depth );
    }
    if ( value instanceof FormulaFunction ) {
      return handleFormulaFunction( (FormulaFunction) value, depth );
    }
    if ( value instanceof StaticValue ) {
      return handleStaticValue( (StaticValue) value );
    }
    if ( value instanceof ContextLookup ) {
      return handleContextLookup( (ContextLookup) value );
    }
    return value != null ? value.toString() : "";
  }

  private String handleTerm( Term t, int depth ) {
    String head = createFriendlyFilterString( t.getHeadValue(), depth + 1 );
    Operator op = Operator.parse( t.getOperators()[ 0 ].toString() );
    String operand = createFriendlyFilterString( t.getOperands()[ 0 ], depth + 1 );
    return String.format( "%s %s %s", head, op, operand );
  }

  private String handleFormulaFunction( FormulaFunction func, int depth ) {
    String fn = func.getFunctionName().toUpperCase();
    LValue[] params = func.getChildValues();

    // Special handling for NOT(IN(...)), NOT(CONTAINS(...)), NOT(ISNA(...))
    if ( "NOT".equals( fn ) && params.length > 0 && params[ 0 ] instanceof FormulaFunction ) {
      return handleNotFunction( (FormulaFunction) params[ 0 ], depth + 1 );
    }

    switch ( fn ) {
      case "AND":
      case "OR": {
        String sep = " " + fn + " ";
        String joined = joinParamsWithDepth( params, sep, depth + 1 );
        boolean needsBrackets = needsBracketsForLogical( params, fn );
        // Only wrap in brackets if this logical op is nested (depth > 0) or has logical children
        if ( depth > 0 || needsBrackets ) {
          return "(" + joined + ")";
        }
        return joined;
      }
      case "EQUALS":
        return binaryOp( params, Operator.EQUAL.toString(), depth + 1 );
      case "<>":
        return binaryOp( params, Operator.NOT_EQUAL.toString(), depth + 1 );
      case ">":
        return binaryOp( params, Operator.GREATER_THAN.toString(), depth + 1 );
      case "<":
        return binaryOp( params, Operator.LESS_THAN.toString(), depth + 1 );
      case ">=":
        return binaryOp( params, Operator.GREATOR_OR_EQUAL.toString(), depth + 1 );
      case "<=":
        return binaryOp( params, Operator.LESS_OR_EQUAL.toString(), depth + 1 );
      case "IN":
        return handleInFunction( params, Operator.IN.toString(), depth + 1 );
      case "CONTAINS":
        return binaryOp( params, Operator.CONTAINS.toString(), depth + 1 );
      case "NOT":
        return "NOT (" + createFriendlyFilterString( params[ 0 ], depth + 1 ) + ")";
      case "ISNULL":
      case "ISNA":
        return createFriendlyFilterString( params[ 0 ], depth + 1 ) + Operator.IS_NULL;
      case "BEGINS WITH":
      case "BEGINSWITH":
        return binaryOp( params, Operator.BEGINS_WITH.toString(), depth + 1 );
      case "ENDS WITH":
      case "ENDSWITH":
        return binaryOp( params, Operator.ENDS_WITH.toString(), depth + 1 );
      default:
        return handleOtherFunction( fn, params, depth + 1 );
    }
  }

  private boolean needsBracketsForLogical( LValue[] params, String fn ) {
    // If any param is a logical function (AND/OR) and same as parent, don't bracket, else bracket
    for ( LValue param : params ) {
      if ( param instanceof FormulaFunction ) {
        String childFn = ( (FormulaFunction) param ).getFunctionName().toUpperCase();
        if ( ( "AND".equals( childFn ) || "OR".equals( childFn ) ) && !childFn.equals( fn ) ) {
          return true;
        }

      }
    }
    return false;
  }

  private String handleNotFunction( FormulaFunction inner, int depth ) {
    String innerFn = inner.getFunctionName().toUpperCase();
    LValue[] innerParams = inner.getChildValues();
    switch ( innerFn ) {
      case "IN":
        return handleInFunction( innerParams, Operator.EXCLUDES.toString(), depth + 1 );
      case "CONTAINS":
        return binaryOp( innerParams, Operator.DOES_NOT_CONTAIN.toString(), depth + 1 );
      case "ISNA":
        return createFriendlyFilterString( innerParams[ 0 ], depth + 1 ) + Operator.IS_NOT_NULL;
      default:
        throw new IllegalStateException( "Unexpected value: " + innerFn );
    }
  }

  private String handleInFunction( LValue[] params, String label, int depth ) {
    if ( params.length < 2 ) {
      return "";
    }
    String field = createFriendlyFilterString( params[ 0 ], depth + 1 );
    String values = joinParamsWithDepth( Arrays.copyOfRange( params, 1, params.length ), ", ", depth + 1 );
    return String.format( "%s %s (%s)", field, label, values );
  }

  private String binaryOp( LValue[] params, String opLabel, int depth ) {
    if ( params.length < 2 ) {
      return "";
    }
    return String.format( "%s %s %s", createFriendlyFilterString( params[ 0 ], depth + 1 ), opLabel,
      createFriendlyFilterString( params[ 1 ], depth + 1 ) );
  }

  private String joinParamsWithDepth( LValue[] params, String sep, int depth ) {
    return Arrays.stream( params )
      .map( p -> createFriendlyFilterString( p, depth ) )
      .collect( Collectors.joining( sep ) );
  }

  private String handleOtherFunction( String fn, LValue[] params, int depth ) {
    String joined = joinParamsWithDepth( params, ", ", depth );
    return String.format( "%s(%s)", fn, joined );
  }

  private String handleStaticValue( StaticValue value ) {
    Object v = value.getValue();
    return v instanceof String ? "\"" + v + "\"" : String.valueOf( v );
  }

  private String handleContextLookup( ContextLookup value ) {
    String name = value.getName();
    if ( name.matches( "param:(.+)" ) ) {
      String[] parts = name.split( ":", 2 );
      if ( parts.length == 2 ) {
        return "value of Prompt " + parts[ 1 ];
      }
    }
    return cleanCol( name );
  }

  private String cleanCol( String col ) {
    col = col.replaceAll( "[\\[\\]]", "" );
    col = col.replaceAll( "\\.NONE$", "" );
    String[] colName = col.split( "\\." );
    String realColName = getQuery().getLogicalModel().
      findLogicalColumn( colName[ 1 ] ).
      getName( getReport().getReportEnvironment().getLocale().toString() );

    if ( colName.length == 3 ) {
      return realColName + " (" + colName[ 2 ] + ") ";
    } else {
      return realColName;
    }
  }
}
