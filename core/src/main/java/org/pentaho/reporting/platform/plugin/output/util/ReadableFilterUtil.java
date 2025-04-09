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

import com.google.common.annotations.VisibleForTesting;
import org.pentaho.metadata.model.concept.types.DataType;
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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ReadableFilterUtil {
  private static final String DATEVALUE = "\\(?DATEVALUE\\((.*?)\\)\\)?";
  private static final String SPACE = " ";
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

  @VisibleForTesting
  IMetadataDomainRepository getMetadataRepository() {
    return PentahoSystem.get( IMetadataDomainRepository.class, PentahoSessionHolder.getSession() );
  }

  /**
   * Extracts a {@link Query} object from the given {@link MasterReport}.
   * <p>
   * This method attempts to retrieve the query associated with the provided report by:
   * <ul>
   *   <li>Validating that the report, its data factory, and its query are not {@code null}.</li>
   *   <li>Searching for a {@link PmdDataFactory} within the report's {@link CompoundDataFactory} using the report's query name and "default" as fallback.</li>
   *   <li>Parsing the MQL string from the found {@link PmdDataFactory} to construct a {@link Query} object.</li>
   * </ul>
   * If no suitable {@link PmdDataFactory} or MQL string is found, {@code null} is returned.
   *
   * @param report the {@link MasterReport} from which to extract the query
   * @return the extracted {@link Query}, or {@code null} if not found
   * @throws PentahoMetadataException if an error occurs during query extraction
   * @throws IllegalArgumentException if the report, its data factory, or its query is {@code null}
   */
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

  @VisibleForTesting
  Query parseQueryFromMql( String mql ) throws PentahoMetadataException {
    if ( mql == null || mql.isEmpty() ) {
      throw new IllegalArgumentException( "MQL string cannot be null or empty" );
    }

    QueryXmlHelper helper = new QueryXmlHelper();
    IMetadataDomainRepository domainRepository = getMetadataRepository();

    return helper.fromXML( domainRepository, mql );

  }

  public String toHumanReadableFilter( String mql ) throws ParseException, EvaluationException {
    if ( isNullOrBlank( mql ) ) {
      return "";
    }

    Formula formula = new Formula( mql );
    formula.initialize( new DefaultFormulaContext() );
    LValue root = formula.getRootReference();
    return toHumanReadable( root );
  }

  private String removeDateValue( String mql ) {
    if ( isNullOrBlank( mql ) ) {
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

  /**
   * Generates a human-readable string representation of a filter term.
   * This method constructs a friendly filter string by extracting the column name,
   * determining the data type, formatting the comparison operator, and recursively
   * formatting the operands. The resulting string is localized and suitable for display.
   *
   * @param term  the filter term to be processed
   * @param depth the current recursion depth, used for formatting nested terms
   * @return a localized, human-readable string representing the filter term
   */
  private String handleTerm( Term term, int depth ) {
    String head = createFriendlyFilterString( term.getHeadValue(), depth + 1 );
    String rawCol = extractColumnName( term.getHeadValue() );
    DataType dataType = getFieldDataType( rawCol );
    String op = getFriendlyComparisonOperator( term.getOperators()[ 0 ].toString(), dataType );
    String operand = createFriendlyFilterString( term.getOperands()[ 0 ], depth + 1 );
    return String.format( "%s %s %s", head, getLocaleString( op ), operand );
  }

  /**
   * Handles the conversion of a {@link FormulaFunction} into a human-readable string representation,
   * applying special handling for logical, comparison, and other supported formula functions.
   * <p>
   * This method recursively processes the formula function and its parameters, generating a
   * friendly filter string suitable for display or logging. Special cases such as NOT, AND, OR,
   * IN, ISNA, and various comparison functions are handled explicitly.
   * </p>
   *
   * @param func  the {@link FormulaFunction} to process
   * @param depth the current recursion depth, used for formatting or limiting recursion
   * @return a human-readable string representation of the formula function
   */
  private String handleFormulaFunction( FormulaFunction func, int depth ) {
    String fn = func.getFunctionName().toUpperCase();
    LValue[] params = func.getChildValues();

    // Special handling for NOT(IN(...)), NOT(CONTAINS(...)), NOT(ISNA(...))
    if ( "NOT".equals( fn ) && params.length > 0 && params[ 0 ] instanceof FormulaFunction ) {
      return handleNotFunction( (FormulaFunction) params[ 0 ], depth + 1 );
    }

    switch ( fn ) {
      case "AND":
      case "OR":
        return handleLogicalFunction( fn, params, depth );
      case "EQUALS":
      case "BEGINSWITH":
      case "ENDSWITH":
      case "CONTAINS":
      case "<=":
      case ">=":
      case "<":
      case ">":
        return handleComparisonFunction( fn, params, depth );
      case "IN":
        return handleInFunction( params, getLocaleString( fn ), depth + 1 );
      case "NOT":
        return "NOT (" + createFriendlyFilterString( params[ 0 ], depth + 1 ) + ")";
      case "ISNA":
        return createFriendlyFilterString( params[ 0 ], depth + 1 ) + SPACE + getLocaleString( fn );
      default:
        return handleOtherFunction( fn, params, depth + 1 );
    }
  }

  private String handleLogicalFunction( String fn, LValue[] params, int depth ) {
    String operatorType = "OPERATOR_TYPES_" + fn;
    String sep = SPACE + Messages.getInstance().getString( operatorType ) + SPACE;
    String joined = joinParamsWithDepth( params, sep, depth + 1 );
    boolean needsBrackets = needsBracketsForLogical( params, fn );
    // Only wrap in brackets if this logical op is nested (depth > 0) or has logical children
    if ( depth > 0 || needsBrackets ) {
      return "(" + joined + ")";
    }
    return joined;
  }

  private String handleComparisonFunction( String fn, LValue[] params, int depth ) {
    if ( params[ 0 ] instanceof Term || params[ 0 ] instanceof ContextLookup ) {
      String rawCol = extractColumnName( params[ 0 ] );
      DataType dataType = getFieldDataType( rawCol );
      String friendlyOp = getFriendlyComparisonOperator( fn, dataType );
      return binaryOp( params, getLocaleString( friendlyOp ), depth + 1 );
    } else {
      return binaryOp( params, getLocaleString( fn ), depth + 1 );
    }
  }

  /**
   * Determines whether brackets are needed around logical function parameters.
   * <p>
   * This method checks if any of the provided parameters is a logical function ("AND" or "OR")
   * and if its function name differs from the parent function name. If so, brackets are needed
   * to preserve the correct logical grouping in the output.
   * </p>
   *
   * @param params the array of {@link LValue} parameters to check
   * @param fn the name of the parent logical function (e.g., "AND" or "OR")
   * @return {@code true} if brackets are needed around the parameters; {@code false} otherwise
   */
  private boolean needsBracketsForLogical( LValue[] params, String fn ) {
    // If any param is a logical function (AND/OR) and same as parent, don't bracket, else bracket
    for ( LValue param : params ) {
      if ( param instanceof FormulaFunction ) {
        String childFn = ( (FormulaFunction) param ).getFunctionName().toUpperCase();
        if ( ( childFn.equals( "AND" ) || childFn.equals( "OR" ) ) && !childFn.equals( fn ) ) {
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
        return handleInFunction( innerParams, Messages.getInstance().getString( "FilterCombinationTypeNotIn" ),
          depth + 1 );
      case "CONTAINS":
        return binaryOp( innerParams, Messages.getInstance().getString( "DOES_NOT_CONTAIN" ), depth + 1 );
      case "ISNA":
        return createFriendlyFilterString( innerParams[ 0 ], depth + 1 ) + SPACE + getLocaleString( "IS_NOT_NULL" );
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

  private String extractColumnName( LValue value ) {
    if ( value instanceof Term ) {
      return ( (Term) value ).getHeadValue().toString();
    }
    if ( value instanceof ContextLookup ) {
      return ( (ContextLookup) value ).getName();
    }

    return value != null ? value.toString() : "";
  }

  /**
   * Determines the {@link DataType} of a specified field by its name.
   * <p>
   * The method processes the given field name by removing square brackets and splitting it by the dot character.
   * It expects the field name to be in the format "table.column". If the format is invalid or the data type
   * cannot be determined, {@link DataType#UNKNOWN} is returned.
   * </p>
   *
   * @param fieldName the name of the field, potentially including table and column information (e.g., "[table].[column]")
   * @return the {@link DataType} of the specified field, or {@link DataType#UNKNOWN} if it cannot be determined
   */
  @VisibleForTesting
  DataType getFieldDataType( String fieldName ) {
    String[] colName = fieldName.replaceAll( "[\\[\\]]", "" ).split( "\\." );
    if ( colName.length < 2 ) {
      return DataType.UNKNOWN;
    }
    var logicalCol = getQuery().getLogicalModel().findLogicalColumn( colName[ 1 ] );
    if ( logicalCol == null || logicalCol.getDataType() == null ) {
      return DataType.UNKNOWN;
    }
    return logicalCol.getDataType();
  }

  /**
   * Returns a user-friendly string representation of a comparison operator based on the provided operator and data type.
   * <p>
   * This method maps technical comparison operators (such as "EQUALS", ">", "<", ">=", "<=") to more readable
   * descriptions, taking into account the data type context (e.g., STRING, DATE). If the data type is not specified,
   * it defaults to {@code DataType.UNKNOWN}. For example, "EQUALS" with a STRING type becomes "EXACTLY_MATCHES",
   * and ">" with a DATE type becomes "AFTER".
   * </p>
   *
   * @param operator the comparison operator to be converted (e.g., "EQUALS", ">", "<", ">=", "<=")
   * @param dataType the data type context for the operator (e.g., STRING, DATE, UNKNOWN)
   * @return a user-friendly string representing the comparison operator, or the original operator if no mapping exists
   */
  @VisibleForTesting
  String getFriendlyComparisonOperator( String operator, DataType dataType ) {
    operator = operator.toUpperCase();
    if ( dataType == null ) {
      dataType = DataType.UNKNOWN;
    }
    switch ( operator ) {
      case "EQUALS":
        if ( dataType == DataType.STRING ) {
          return "EXACTLY_MATCHES";
        }
        if ( dataType == DataType.DATE ) {
          return "ON";
        }
        return operator;
      case ">":
        if ( dataType == DataType.DATE ) {
          return "AFTER";
        }
        return "MORE_THAN";
      case "<":
        if ( dataType == DataType.DATE ) {
          return "BEFORE";
        }
        return "LESS_THAN";
      case ">=":
        if ( dataType == DataType.DATE ) {
          return "ON_OR_AFTER";
        }
        return "MORE_THAN_OR_EQUAL";
      case "<=":
        if ( dataType == DataType.DATE ) {
          return "ON_OR_BEFORE";
        }
        return "LESS_THAN_OR_EQUAL";
      default:
        return operator;
    }
  }

  private String binaryOp( LValue[] params, String opLabel, int depth ) {
    if ( params.length < 2 ) {
      return "";
    }
    return String.format( "%s %s %s", createFriendlyFilterString( params[ 0 ], depth + 1 ), opLabel,
      createFriendlyFilterString( params[ 1 ], depth + 1 ) );
  }

  /**
   * Joins an array of {@link LValue} parameters into a single string, using the specified separator.
   * Each parameter is converted to a friendly filter string representation with the given depth.
   *
   * @param params the array of {@link LValue} parameters to join
   * @param sep the separator string to use between joined elements
   * @param depth the depth to use when creating the friendly filter string for each parameter
   * @return a single string with all parameters joined by the specified separator
   */
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

  /**
   * Processes a {@link ContextLookup} object to generate a readable string representation.
   * <p>
   * If the context name matches the pattern "param:(.+)", it extracts the parameter name and formats it
   * using a localized message pattern. Otherwise, it returns a cleaned version of the context name.
   * </p>
   *
   * @param value the {@link ContextLookup} object containing the context name to process
   * @return a human-readable string representation of the context name, formatted if it is a parameter reference,
   *         or a cleaned version otherwise
   */
  private String handleContextLookup( ContextLookup value ) {
    String name = value.getName();
    if ( name.matches( "param:(.+)" ) ) {
      String[] parts = name.split( ":", 2 );
      if ( parts.length == 2 ) {
        String pattern = Messages.getInstance().getString( "FilterTextValueFromPrompt" );
        return MessageFormat.format( pattern, parts[ 1 ] );
      }
    }
    return cleanCol( name );
  }

  @VisibleForTesting
  String cleanCol( String col ) {
    col = col.replaceAll( "[\\[\\]]", "" );
    col = col.replaceAll( "\\.NONE$", "" );
    String[] colName = col.split( "\\." );
    var logicalCol = getQuery().getLogicalModel().findLogicalColumn( colName[ 1 ] );
    String displayName = logicalCol.getName( getReport().getReportEnvironment().getLocale().toString() );

    if ( colName.length == 3 ) {
      return displayName + " (" + colName[ 2 ] + ") ";
    } else {
      return displayName;
    }
  }

  private boolean isNullOrBlank( String str ) {
    return str == null || str.isBlank();
  }

  public static String getLocaleString( String op ) {
    if ( op == null ) {
      return Messages.getInstance().getString( "UNKNOWN" );
    }
    switch ( op.trim().toUpperCase() ) {
      case "MORE_THAN":
        return Messages.getInstance().getString( "MORE_THAN" );
      case "LESS_THAN":
        return Messages.getInstance().getString( "LESS_THAN" );
      case "EQUAL":
      case "EQUALS":
        return Messages.getInstance().getString( "EQUALS" );
      case "EXACTLY_MATCHES":
        return Messages.getInstance().getString( "EXACTLY_MATCHES" );
      case "ON":
        return Messages.getInstance().getString( "ON" );
      case "AFTER":
        return Messages.getInstance().getString( "AFTER" );
      case "BEFORE":
        return Messages.getInstance().getString( "BEFORE" );
      case "ON_OR_AFTER":
        return Messages.getInstance().getString( "ON_OR_AFTER" );
      case "ON_OR_BEFORE":
        return Messages.getInstance().getString( "ON_OR_BEFORE" );
      case "MORE_THAN_OR_EQUAL":
        return Messages.getInstance().getString( "MORE_THAN_OR_EQUAL" );
      case "LESS_THAN_OR_EQUAL":
        return Messages.getInstance().getString( "LESS_THAN_OR_EQUAL" );
      case "CONTAINS":
        return Messages.getInstance().getString( "CONTAINS" );
      case "BEGINSWITH":
        return Messages.getInstance().getString( "BEGINS_WITH" );
      case "ENDSWITH":
        return Messages.getInstance().getString( "ENDS_WITH" );
      case "ISNA":
        return Messages.getInstance().getString( "IS_NULL" );
      case "IS_NOT_NULL":
        return Messages.getInstance().getString( "IS_NOT_NULL" );
      case "IN":
        return Messages.getInstance().getString( "FilterCombinationTypeIn" );
      default:
        return Messages.getInstance().getString( "UNKNOWN" );
    }
  }
}
