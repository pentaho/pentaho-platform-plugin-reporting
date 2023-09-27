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
 * Copyright (c) 2002-2023 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import com.cronutils.utils.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.platform.plugin.action.jfreereport.helper.PentahoTableModel;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.ListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.StaticListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationMessage;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.engine.classic.core.util.beans.ConverterRegistry;
import org.pentaho.reporting.engine.classic.core.util.beans.ValueConverter;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.messages.Messages;

import javax.swing.table.TableModel;
import java.io.IOException;
import java.lang.reflect.Array;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class ReportContentUtil {
  private static final String ONLY_DATE_REGEX_PATTERN = "(y{4}|([dM]){2})([-/])(([dM]){2})([-/])(y{4}|([dM]){2})";
  private static final String CONFIG_START_DATE_RANGE_PARAM_NAME = "org.pentaho.reporting.engine.classic.core.scheduler.startDateRangeParamName";
  private static final String CONFIG_END_DATE_RANGE_PARAM_NAME = "org.pentaho.reporting.engine.classic.core.scheduler.endDateRangeParamName";
  public static String startDateParamName = ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty( CONFIG_START_DATE_RANGE_PARAM_NAME, "" );
  public static String endDateParamName = ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty( CONFIG_END_DATE_RANGE_PARAM_NAME, "" );
  private static final boolean useRelativeDateParams = !startDateParamName.isEmpty() && !endDateParamName.isEmpty();
  public static final String USE_RELATIVE_DATE_STRING = "Use Relative Date";

  public static final String CONFIG_FISCAL_YEAR_START = "org.pentaho.reporting.engine.classic.core.scheduler.fiscalYearStart";
  public static final String fiscalYearStartString = ClassicEngineBoot.getInstance().getGlobalConfig().getConfigProperty( CONFIG_FISCAL_YEAR_START, "2023-01-01" );

  /**
   * Apply inputs (if any) to corresponding report parameters, care is taken when checking parameter types to perform
   * any necessary casting and conversion.
   *
   * @param report           The report to retrieve parameter definitions and values from.
   * @param context          a ParameterContext for which the parameters will be under
   * @param validationResult the validation result that will hold the warnings. If null, a new one will be created.
   * @return the validation result containing any parameter validation errors.
   * @throws java.io.IOException if the report of this component could not be parsed.
   * @throws ResourceException   if the report of this component could not be parsed.
   */
  public static ValidationResult applyInputsToReportParameters( final MasterReport report,
                                                                final ParameterContext context,
                                                                final Map<String, Object> inputs,
                                                                ValidationResult validationResult )
    throws IOException, ResourceException {
    if ( validationResult == null ) {
      validationResult = new ValidationResult();
    }
    // apply inputs to report
    if ( inputs != null ) {
      final Log log = LogFactory.getLog( SimpleReportingComponent.class );
      ParameterDefinitionEntry[] params = report.getParameterDefinition().getParameterDefinitions();
      if ( useRelativeDateParams && shouldInjectRelativeDateParams( params ) ) {
        params = addRelativeDateFields( params, startDateParamName, endDateParamName );
        DefaultParameterDefinition newReportParamDef = new DefaultParameterDefinition();
        for ( ParameterDefinitionEntry entry : params ) {
          newReportParamDef.addParameterDefinition( entry );
        }
        report.setParameterDefinition( newReportParamDef );
      }
      final ReportParameterValues parameterValues = report.getParameterValues();
      for ( final ParameterDefinitionEntry param : params ) {
        final String paramName = param.getName();
        try {
          final Object computedParameter =
            ReportContentUtil.computeParameterValue( context, param, inputs.get( paramName ) );
          parameterValues.put( param.getName(), computedParameter );
          if ( log.isInfoEnabled() ) {
            log.info( Messages.getInstance().getString( "ReportPlugin.infoParameterValues", //$NON-NLS-1$
              paramName, String.valueOf( inputs.get( paramName ) ), String.valueOf( computedParameter ) ) );
          }
        } catch ( final Exception e ) {
          if ( log.isWarnEnabled() ) {
            log.warn( Messages.getInstance().getString( "ReportPlugin.logErrorParametrization" ), e ); //$NON-NLS-1$
          }
          validationResult.addError( paramName, new ValidationMessage( e.getMessage() ) );
        }
      }
    }
    return validationResult;
  }

  public static String getRelCheckboxParamName( String paramName ) {
    return paramName + "_Checkbox";
  }

  public static String getThisLastParamName( String paramName ) {
    return paramName + "_ThisLast";
  }

  public static String getRelativeValParamName( String paramName ) {
    return paramName + "_RelativeVal";
  }

  public static String getRelativeUnitParamName( String paramName ) {
    return paramName + "_RelativeUnit";
  }

  protected static boolean shouldInjectRelativeDateParams( ParameterDefinitionEntry[] inputParams ) {
    boolean hasStartDate = false;
    boolean hasEndDate = false;
    boolean paramsAlreadyPresent = false;
    for ( ParameterDefinitionEntry entry : inputParams ) {
      hasStartDate |= ( entry.getName().equalsIgnoreCase( startDateParamName ) && entry.getValueType().equals( java.util.Date.class ) );
      hasEndDate |= ( entry.getName().equalsIgnoreCase( endDateParamName ) && entry.getValueType().equals( java.util.Date.class ) );
      paramsAlreadyPresent |= entry.getName().equalsIgnoreCase( getRelCheckboxParamName( startDateParamName ) );
    }
    return hasStartDate && hasEndDate && !paramsAlreadyPresent;
  }

  protected static ParameterDefinitionEntry[] addRelativeDateFields( ParameterDefinitionEntry[] inputParams,
                                                                     String startDateParamName,
                                                                     String endDateParamName ) {
    ParameterDefinitionEntry[] modifiedParams = new ParameterDefinitionEntry[ inputParams.length + 4 ];
    String checkboxParamName = getRelCheckboxParamName( startDateParamName );

    StaticListParameter checkboxParam = new StaticListParameter( checkboxParamName, true, true, String.class );
    checkboxParam.setHidden( false );  // checkbox is always visible
    checkboxParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.ROLE_USER_PARAMETER, "user" );
    checkboxParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE, ParameterAttributeNames.Core.TYPE_CHECKBOX );
    checkboxParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL, "Use Relative Date" );
    checkboxParam.addValues( USE_RELATIVE_DATE_STRING, USE_RELATIVE_DATE_STRING );
    modifiedParams[ 0 ] = checkboxParam;
    // checkbox param existed and was true; add in the other fields
    StaticListParameter thisLastParam = new StaticListParameter( getThisLastParamName( startDateParamName ), false, true, String.class );
    thisLastParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.ROLE_USER_PARAMETER, "user" );
    thisLastParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE, ParameterAttributeNames.Core.TYPE_DROPDOWN );
    thisLastParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL, "This/Last" );
    thisLastParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.HIDDEN_FORMULA, getRelativeDateHiddenFormulaNegated( checkboxParamName ) );
    thisLastParam.addValues( "This", "This" );
    thisLastParam.addValues( "Last", "Last" );
    thisLastParam.setHidden( true );  // these will always be hidden at first
    thisLastParam.setDefaultValue( "This" );
    modifiedParams[ 1 ] = thisLastParam;
    PlainParameter valueParam = new PlainParameter( getRelativeValParamName( startDateParamName ), Integer.class );
    valueParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.ROLE_USER_PARAMETER, "user" );
    valueParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE, ParameterAttributeNames.Core.TYPE_TEXTBOX );
    valueParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL, "Value" );
    valueParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.HIDDEN_FORMULA, getRelativeDateHiddenFormulaNegated( checkboxParamName ) );
    valueParam.setHidden( true );
    valueParam.setDefaultValue( 1 );
    modifiedParams[ 2 ] = valueParam;
    StaticListParameter unitParam = new StaticListParameter( getRelativeUnitParamName( startDateParamName ), false, true, String.class );
    unitParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.ROLE_USER_PARAMETER, "user" );
    unitParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.TYPE, ParameterAttributeNames.Core.TYPE_DROPDOWN );
    unitParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.LABEL, "Units" );
    unitParam.setParameterAttribute( ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.HIDDEN_FORMULA, getRelativeDateHiddenFormulaNegated( checkboxParamName ) );
    unitParam.addValues( "Days", "Days" );
    unitParam.addValues( "Weeks", "Weeks" );
    unitParam.addValues( "Months", "Months" );
    unitParam.addValues( "Months (Calendar)", "Months (Calendar)" );
    unitParam.addValues( "Quarter (Calendar)", "Quarter (Calendar)" );
    unitParam.addValues( "Quarter (Fiscal)", "Quarter (Fiscal)" );
    unitParam.addValues( "Months", "Months" );
    unitParam.addValues( "Years", "Years" );
    unitParam.addValues( "Years (Calendar)", "Years (Calendar)" );
    unitParam.addValues( "Years (Fiscal)", "Years (Fiscal)" );
    unitParam.setDefaultValue( "Days" );
    unitParam.setHidden( true );
    modifiedParams[ 3 ] = unitParam;
    int paramIndex = 4;
    // copy original parameters into the modified array, updating the start and end date params with the hidden formula
    for ( ParameterDefinitionEntry entry : inputParams ) {
      modifiedParams[ paramIndex ] = entry;
      if ( entry.getName().equalsIgnoreCase( startDateParamName ) || entry.getName().equalsIgnoreCase( endDateParamName ) ) {
        ( (PlainParameter) modifiedParams[ paramIndex ] ).setParameterAttribute(
          ParameterAttributeNames.Core.NAMESPACE, ParameterAttributeNames.Core.HIDDEN_FORMULA, getRelativeDateHiddenFormula( checkboxParamName ) );
      }
      paramIndex++;
    }
    return modifiedParams;
  }

  private static String getRelativeDateHiddenFormula( String checkBoxParamName ) {
    return "=EQUALS([" + checkBoxParamName + "];\"" + USE_RELATIVE_DATE_STRING + "\")";
  }

  private static String getRelativeDateHiddenFormulaNegated( String checkBoxParamName ) {
    return "=NOT(EQUALS([" + checkBoxParamName + "];\"" + USE_RELATIVE_DATE_STRING + "\"))";
  }

  public static Object computeParameterValue( final ParameterContext parameterContext,
                                              final ParameterDefinitionEntry parameterDefinition, final Object value )
    throws ReportProcessingException {
    if ( value == null ) {
      // there are still buggy report definitions out there ...
      return null;
    }

    final Class valueType = parameterDefinition.getValueType();
    final boolean allowMultiSelect = isAllowMultiSelect( parameterDefinition );
    if ( allowMultiSelect && Collection.class.isInstance( value ) ) {
      final Collection c = (Collection) value;
      final Class componentType;
      if ( valueType.isArray() ) {
        componentType = valueType.getComponentType();
      } else {
        componentType = valueType;
      }

      final int length = c.size();
      final Object[] sourceArray = c.toArray();
      final Object array = Array.newInstance( componentType, length );
      for ( int i = 0; i < length; i++ ) {
        Array.set( array, i, convert( parameterContext, parameterDefinition, componentType, sourceArray[ i ] ) );
      }
      return array;
    } else if ( value.getClass().isArray() ) {
      final Class componentType;
      if ( valueType.isArray() ) {
        componentType = valueType.getComponentType();
      } else {
        componentType = valueType;
      }

      final int length = Array.getLength( value );
      final Object array = Array.newInstance( componentType, length );
      for ( int i = 0; i < length; i++ ) {
        Array.set( array, i, convert( parameterContext, parameterDefinition, componentType, Array.get( value, i ) ) );
      }
      return array;
    } else if ( allowMultiSelect ) {
      // if the parameter allows multi selections, wrap this single input in an array
      // and re-call addParameter with it
      final Object[] array = new Object[ 1 ];
      array[ 0 ] = value;
      return computeParameterValue( parameterContext, parameterDefinition, array );
    } else {
      return convert( parameterContext, parameterDefinition, parameterDefinition.getValueType(), value );
    }
  }

  private static boolean isAllowMultiSelect( final ParameterDefinitionEntry parameter ) {
    if ( parameter instanceof ListParameter ) {
      final ListParameter listParameter = (ListParameter) parameter;
      return listParameter.isAllowMultiSelection();
    }
    return false;
  }

  private static Object convert( final ParameterContext context, final ParameterDefinitionEntry parameter,
                                 final Class targetType, final Object rawValue ) throws ReportProcessingException {
    if ( targetType == null ) {
      throw new NullPointerException();
    }

    if ( rawValue == null ) {
      return null;
    }
    if ( targetType.isInstance( rawValue ) ) {
      return rawValue;
    }

    if ( targetType.isAssignableFrom( TableModel.class )
      && IPentahoResultSet.class.isAssignableFrom( rawValue.getClass() ) ) {
      // wrap IPentahoResultSet to simulate TableModel
      return new PentahoTableModel( (IPentahoResultSet) rawValue );
    }

    final String valueAsString = String.valueOf( rawValue );
    if ( StringUtils.isEmpty( valueAsString ) ) {
      // none of the converters accept empty strings as valid input. So we can return null instead.
      return null;
    }

    if ( targetType.equals( Timestamp.class ) ) {
      try {
        final Date date = parseDate( parameter, context, valueAsString );
        return new Timestamp( date.getTime() );
      } catch ( ParseException pe ) {
        // ignore, we try to parse it as real date now ..
        CommonUtil.checkStyleIgnore();
      }
    } else if ( targetType.equals( Time.class ) ) {
      try {
        final Date date = parseDate( parameter, context, valueAsString );
        return new Time( date.getTime() );
      } catch ( ParseException pe ) {
        // ignore, we try to parse it as real date now ..
        CommonUtil.checkStyleIgnore();
      }
    } else if ( targetType.equals( java.sql.Date.class ) ) {
      try {
        final Date date = parseDate( parameter, context, valueAsString );
        return new java.sql.Date( date.getTime() );
      } catch ( ParseException pe ) {
        // ignore, we try to parse it as real date now ..
        CommonUtil.checkStyleIgnore();
      }
    } else if ( targetType.equals( Date.class ) ) {
      try {
        final Date date = parseDate( parameter, context, valueAsString );
        return new Date( date.getTime() );
      } catch ( ParseException pe ) {
        // ignore, we try to parse it as real date now ..
        CommonUtil.checkStyleIgnore();
      }
    }

    final String dataFormat =
      parameter.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.DATA_FORMAT, context );
    if ( dataFormat != null ) {
      try {
        if ( Number.class.isAssignableFrom( targetType ) ) {
          final DecimalFormat format =
            new DecimalFormat( dataFormat, new DecimalFormatSymbols( LocaleHelper.getLocale() ) );
          format.setParseBigDecimal( true );
          final Number number = format.parse( valueAsString );
          final String asText = ConverterRegistry.toAttributeValue( number );
          return ConverterRegistry.toPropertyValue( asText, targetType );
        } else if ( Date.class.isAssignableFrom( targetType ) ) {
          final SimpleDateFormat format =
            new SimpleDateFormat( dataFormat, new DateFormatSymbols( LocaleHelper.getLocale() ) );
          format.setLenient( false );
          final Date number = format.parse( valueAsString );
          final String asText = ConverterRegistry.toAttributeValue( number );
          return ConverterRegistry.toPropertyValue( asText, targetType );
        }
      } catch ( Exception e ) {
        // again, ignore it .
        CommonUtil.checkStyleIgnore();
      }
    }

    final ValueConverter valueConverter = ConverterRegistry.getInstance().getValueConverter( targetType );
    if ( valueConverter != null ) {
      try {
        return valueConverter.toPropertyValue( valueAsString );
      } catch ( BeanException e ) {
        throw new ReportProcessingException( Messages.getInstance().getString(
          "ReportPlugin.unableToConvertParameter", parameter.getName(), valueAsString ) ); //$NON-NLS-1$
      }
    }
    return rawValue;
  }

  private static Date parseDate( final ParameterDefinitionEntry parameterEntry, final ParameterContext context,
                                 final String value ) throws ParseException {
    try {
      return parseDateStrict( parameterEntry, context, value );
    } catch ( ParseException pe ) {
      CommonUtil.checkStyleIgnore();
    }

    try {
      // parse the legacy format that we used in 3.5.0-GA.
      final Long dateAsLong = Long.parseLong( value );
      return new Date( dateAsLong );
    } catch ( NumberFormatException nfe ) {
      // ignored
      CommonUtil.checkStyleIgnore();
    }

    try {
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd" ); // NON-NLS
      return simpleDateFormat.parse( value );
    } catch ( ParseException pe ) {
      CommonUtil.checkStyleIgnore();
    }
    throw new ParseException( "Unable to parse Date", 0 );
  }

  static Date parseDateStrict( final ParameterDefinitionEntry parameterEntry, final ParameterContext context,
                               final String value ) throws ParseException {
    final String timezoneSpec =
      parameterEntry.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.TIMEZONE, context );
    if ( timezoneSpec == null || "server".equals( timezoneSpec ) ) { // NON-NLS
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" ); // NON-NLS
      return simpleDateFormat.parse( value );
    } else if ( "utc".equals( timezoneSpec ) ) { // NON-NLS
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" ); // NON-NLS
      simpleDateFormat.setTimeZone( TimeZone.getTimeZone( "UTC" ) ); // NON-NLS
      return simpleDateFormat.parse( value );
    } else if ( "client".equals( timezoneSpec ) ) { // NON-NLS
      try {
        // As a workaround, when there is only date configured (and no time) then we parse only as date to avoid errors
        // caused by change of day due to client/server different timezones (because reporting mechanism assumes no time = 00:00:00)
        // Note: a whole refactor should be done to integrate new Java 8 DateTime API and then this workaround can be removed.
        final SimpleDateFormat simpleDateFormat = isOnlyDateFormat( parameterEntry, context )
          ? new SimpleDateFormat( "yyyy-MM-dd" ) // NON-NLS
          : new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" ); // NON-NLS
        return simpleDateFormat.parse( value );
      } catch ( ParseException pe ) {
        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" ); // NON-NLS
        return simpleDateFormat.parse( value );
      }
    } else {
      final TimeZone timeZone = TimeZone.getTimeZone( timezoneSpec );
      // this never returns null, but if the timezone is not understood, we end up with GMT/UTC.
      final SimpleDateFormat simpleDateFormat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS" ); // NON-NLS
      simpleDateFormat.setTimeZone( timeZone );
      return simpleDateFormat.parse( value );
    }
  }

  /**
   * Checks whether expected format only contains date using a regex pattern
   *
   * @param parameterEntry the parameter definition entry
   * @param context        the parameter context
   * @return true if expected format only contains date, false otherwise
   */
  private static boolean isOnlyDateFormat( ParameterDefinitionEntry parameterEntry, final ParameterContext context ) {

    final String dataFormatSpec =
      parameterEntry.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
        ParameterAttributeNames.Core.DATA_FORMAT, context );

    return dataFormatSpec != null && dataFormatSpec.matches( ONLY_DATE_REGEX_PATTERN );
  }

  @VisibleForTesting
  protected static void setStartDateParamName( String name ) {
    startDateParamName = name;
  }

  @VisibleForTesting
  protected static void setEndDateParamName( String name ) {
    endDateParamName = name;
  }
}
