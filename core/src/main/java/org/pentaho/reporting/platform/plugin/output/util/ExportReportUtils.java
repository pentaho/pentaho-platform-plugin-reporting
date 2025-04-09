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
import org.pentaho.metadata.query.model.Query;
import org.pentaho.pms.core.exception.PentahoMetadataException;
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportHeader;
import org.pentaho.reporting.engine.classic.core.elementfactory.LabelElementFactory;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.libraries.formula.EvaluationException;
import org.pentaho.reporting.libraries.formula.parser.ParseException;

import java.sql.Date;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface ExportReportUtils {
  String FILTERS_SUMMARY = "Filters Summary";
  String PROMPTS_SUMMARY = "Prompts Summary";
  String NO_FILTERS = "No filters used";
  String NO_PROMPTS = "No prompts used";
  ReadableFilterUtil readableFilterUtil = new ReadableFilterUtil();

  Map<Class<?>, Function<Object, String>> typeFormattingStrategies = Map.of(
    String.class, String.class::cast,
    Number.class, Object::toString,
    Date.class, value -> ( (java.sql.Date) value ).toLocalDate().toString(),
    String[].class, value -> String.join( ", ", (String[]) value ),
    Number[].class, value -> formatNumberArray( (Number[]) value ),
    Date[].class, value -> formatDateArray( (Date[]) value )
  );

  private static String formatNumberArray( Number[] array ) {
    return Arrays.stream( array )
      .map( Object::toString )
      .collect( Collectors.joining( ", " ) );
  }

  private static String formatDateArray( Date[] dates ) {
    return Arrays.stream( dates )
      .map( date ->
        // Convert java.sql.Date to LocalDate directly
        date.toLocalDate().toString()
      )
      .collect( Collectors.joining( ", " ) );
  }

  /*
   * Formats the parameter value based on its type.
   *
   * @param parameterValue The parameter value to format.
   * @return The formatted string representation of the parameter value.
   */
  static String formatParameterValue( Object parameterValue ) {
    return typeFormattingStrategies.entrySet().stream()
      .filter( entry -> entry.getKey().isInstance( parameterValue ) )
      .findFirst()
      .map( entry -> entry.getValue().apply( parameterValue ) )
      .orElseThrow( () -> new IllegalArgumentException(
        "Unsupported type: " + parameterValue.getClass().getSimpleName()
      ) );
  }

  default String getReadableFilterDescription() {
    return null;
  }

  private void addElementToReportHeader( Element element, ReportHeader reportHeader ) {
    if ( element != null && reportHeader != null ) {
      reportHeader.addElement( element );
    }
  }

  default void addReportDetailsPage( MasterReport report, DefaultParameterContext parameterContext )
    throws ParseException, EvaluationException, PentahoMetadataException, ReportDataFactoryException {
    if ( report == null ) {
      return;
    }

    ReportHeader reportHeader = report.getReportHeader();
    if ( reportHeader == null ) {
      throw new IllegalStateException( "Report header cannot be null" );
    }

    reportHeader.setPagebreakAfterPrint( true );

    Query thinQuery = readableFilterUtil.getQueryFromString( report );

    addElementToReportHeader( createLabel( FILTERS_SUMMARY ), reportHeader );
    if ( thinQuery != null && !thinQuery.getConstraints().isEmpty() ) {
      addFiltersFromQuery( reportHeader, thinQuery );
    } else {
      addFilters( reportHeader );
    }

    addElementToReportHeader( createLabel( PROMPTS_SUMMARY ), reportHeader );
    addPrompts( reportHeader, report, parameterContext );
  }

  @VisibleForTesting
  default void addFilters( ReportHeader reportHeader ) {
    String filterDesc = getReadableFilterDescription();
    if ( filterDesc == null || filterDesc.isEmpty() ) {
      filterDesc = NO_FILTERS;
    }

    addElementToReportHeader( createText( filterDesc ), reportHeader );
  }

  @VisibleForTesting
  default void addFiltersFromQuery( ReportHeader reportHeader, Query query )
    throws ParseException, EvaluationException {
    String filterDesc = readableFilterUtil.toHumanReadableMql( query.getConstraints().get( 0 ).getFormula() );
    if ( filterDesc.isEmpty() ) {
      filterDesc = NO_FILTERS;
    }

    addElementToReportHeader( createText( filterDesc ), reportHeader );
  }

  @VisibleForTesting
  default void addPrompts( ReportHeader reportHeader, MasterReport report, DefaultParameterContext parameterContext )
    throws ReportDataFactoryException {
    ParameterDefinitionEntry[] parameterDefinitions = report.getParameterDefinition().getParameterDefinitions();
    if ( parameterDefinitions == null || parameterDefinitions.length == 0 ) {
      addElementToReportHeader( createText( NO_PROMPTS ), reportHeader );
    } else {

      for ( ParameterDefinitionEntry parameter : parameterDefinitions ) {
        String parameterName = parameter.getName();
        Object parameterValue = parameter.getDefaultValue( parameterContext );
        if ( parameterValue != null ) {
          String parameterValueString = formatParameterValue( parameterValue );
          addElementToReportHeader( createText( parameterName + ": " + parameterValueString ), reportHeader );
        }
      }
    }

  }

  @VisibleForTesting
  default Element createLabel( String text ) {
    return createElement( text, 12, true );
  }

  @VisibleForTesting
  default Element createText( String text ) {
    return createElement( text, 10, false );
  }

  @VisibleForTesting
  default Element createElement( String text, int fontSize, boolean isBold ) {
    LabelElementFactory elementFactory = new LabelElementFactory();
    elementFactory.setText( text );
    elementFactory.setMinimumHeight( 20f );
    elementFactory.setFontSize( fontSize );
    elementFactory.setDynamicHeight( true );
    elementFactory.setBold( isBold );
    return elementFactory.createElement();
  }

}
