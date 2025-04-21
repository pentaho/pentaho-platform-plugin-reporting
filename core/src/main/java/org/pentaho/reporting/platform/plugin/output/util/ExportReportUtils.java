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
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportHeader;
import org.pentaho.reporting.engine.classic.core.elementfactory.LabelElementFactory;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ExportReportUtils {
  private static final String FILTERS_SUMMARY = "Filters Summary";
  private static final String PROMPTS_SUMMARY = "Prompts Summary";
  private static final String NO_FILTERS = "No filters used";
  private static final String NO_PROMPTS = "No prompts used";
  private String readableFilterDescription;
  private boolean pirExported;
  private final Map<Class<?>, Function<Object, String>> typeFormattingStrategies = Map.of(
    String.class, String.class::cast,
    Number.class, Object::toString,
    Date.class, value -> ((java.sql.Date) value).toLocalDate().toString(),
    String[].class, value -> String.join( ", ", (String[]) value ),
    Number[].class, value -> formatNumberArray( (Number[]) value ),
    Date[].class, value -> formatDateArray( (Date[]) value )
  );

  public ExportReportUtils() {
    this.readableFilterDescription = null;
    this.pirExported = false;
  }

  public String getReadableFilterDescription() {
    return readableFilterDescription;
  }

  public void setReadableFilterDescription( String readableFilterDescription ) {
    this.readableFilterDescription = readableFilterDescription;
  }

  public boolean isPirExported() {
    return pirExported;
  }

  public void setPirExported( boolean exportPIR ) {
    this.pirExported = exportPIR;
  }

  public void addFiltersAndPromptsPage( MasterReport report ) {
    if ( !isPirExported() || report == null ) {
      return;
    }

    ReportHeader reportHeader = report.getReportHeader();
    if ( reportHeader == null ) {
      throw new IllegalStateException( "Report header cannot be null" );
    }

    reportHeader.setPagebreakAfterPrint( true );

    addElementToReportHeader( createLabel( FILTERS_SUMMARY ), reportHeader );
    addFilters( reportHeader );

    addElementToReportHeader( createLabel( PROMPTS_SUMMARY ), reportHeader );
    addPrompts( reportHeader, report );
  }

  @VisibleForTesting
  void addFilters( ReportHeader reportHeader ) {
    String filterDesc = getReadableFilterDescription();
    if ( filterDesc == null || filterDesc.isEmpty() ) {
      filterDesc = NO_FILTERS;
    }

    addElementToReportHeader( createText( filterDesc ), reportHeader );
  }

  @VisibleForTesting
  void addPrompts( ReportHeader reportHeader, MasterReport report ) {
    ReportParameterValues parameterValues = report.getParameterValues();
    String parameterValueString = null;
    if ( parameterValues == null ) {
      return;
    }

    for ( String parameterName : parameterValues.getColumnNames() ) {
      Object parameterValue = parameterValues.get( parameterName );
      if ( parameterValue != null ) {
        parameterValueString = formatParameterValue( parameterValue );
        addElementToReportHeader( createText( parameterName + ": " + parameterValueString ), reportHeader );
      }
    }

    if ( parameterValueString == null || parameterValueString.isEmpty() ) {
      addElementToReportHeader( createText( NO_PROMPTS ), reportHeader );
    }
  }

  private void addElementToReportHeader( Element element, ReportHeader reportHeader ) {
    if ( element != null && reportHeader != null ) {
      reportHeader.addElement( element );
    }
  }

  @VisibleForTesting
  Element createLabel( String text ) {
    return createElement( text, 12, true );
  }

  @VisibleForTesting
  Element createText( String text ) {
    return createElement( text, 10, false );
  }

  @VisibleForTesting
  Element createElement( String text, int fontSize, boolean isBold ) {
    LabelElementFactory elementFactory = new LabelElementFactory();
    elementFactory.setText( text );
    elementFactory.setMinimumHeight( 20f );
    elementFactory.setFontSize( fontSize );
    elementFactory.setDynamicHeight( true );
    elementFactory.setBold( isBold );
    return elementFactory.createElement();
  }

  private String formatNumberArray( Number[] array ) {
    return Arrays.stream( array )
      .map( Object::toString )
      .collect( Collectors.joining( ", " ) );
  }

  private String formatDateArray( Date[] dates ) {
    return Arrays.stream( dates )
      .map( date ->
        // Convert java.sql.Date to LocalDate directly
        ( (java.sql.Date) date ).toLocalDate().toString()
      )
      .collect( Collectors.joining( ", " ) );
  }

  /*
   * Formats the parameter value based on its type.
   *
   * @param parameterValue The parameter value to format.
   * @return The formatted string representation of the parameter value.
   */
  public String formatParameterValue( Object parameterValue ) {
    return typeFormattingStrategies.entrySet().stream()
      .filter( entry -> entry.getKey().isInstance( parameterValue ) )
      .findFirst()
      .map( entry -> entry.getValue().apply( parameterValue ) )
      .orElseThrow( () -> new IllegalArgumentException(
        "Unsupported type: " + parameterValue.getClass().getSimpleName()
      ) );
  }
}
