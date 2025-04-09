package org.pentaho.reporting.platform.plugin.output.util;

import org.pentaho.reporting.engine.classic.core.Band;
import org.pentaho.reporting.engine.classic.core.Element;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.elementfactory.LabelElementFactory;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReportUtils {
  private static final String FILTERS_SUMMARY = "Filters Summary";
  private static final String PROMPTS_SUMMARY = "Prompts Summary";
  private static final String NO_FILTERS = "No filters used";
  private static final String NO_PROMPTS = "No prompts used";
  private String readableFilterDescription;
  private boolean fromPIR;

  public ReportUtils() {
    this.readableFilterDescription = null;
    this.fromPIR = false;
  }

  public String getReadableFilterDescription() {
    return readableFilterDescription;
  }

  public void setReadableFilterDescription( String readableFilterDescription ) {
    this.readableFilterDescription = readableFilterDescription;
  }

  public boolean isFromPIR() {
    return fromPIR;
  }

  public void setFromPIR( boolean fromPIR ) {
    this.fromPIR = fromPIR;
  }

  public void addFiltersAndPromptsPage( MasterReport report ) {
    if ( !isFromPIR() || report == null ) {
      return;
    }

    Band reportHeader = report.getReportHeader();
    if ( reportHeader == null ) {
      throw new IllegalStateException( "Report header cannot be null" );
    }

    reportHeader.setPagebreakAfterPrint( true );

    addLabelToReportHeader( createLabel( FILTERS_SUMMARY ), reportHeader );
    addFilters( reportHeader );

    addLabelToReportHeader( createLabel( PROMPTS_SUMMARY ), reportHeader );
    addPrompts( reportHeader, report );
  }

  private void addFilters( Band reportHeader ) {
    String filterDesc = getReadableFilterDescription();
    if ( filterDesc == null || filterDesc.isEmpty() ) {
      filterDesc = NO_FILTERS;
    }

    addLabelToReportHeader( createText( filterDesc ), reportHeader );
  }

  private String formatParameterValue( Object parameterValue ) {
    if ( parameterValue instanceof String ) {
      return (String) parameterValue;
    } else if ( parameterValue instanceof Number ) {
      return parameterValue.toString();
    } else if ( parameterValue instanceof Date ) {
      return new SimpleDateFormat( "yyyy-MM-dd" ).format( parameterValue );
    } else if ( parameterValue instanceof String[] ) {
      return String.join( ", ", (String[]) parameterValue );
    } else if ( parameterValue instanceof Number[] ) {
      return String.join( ", ",
        Arrays.stream( (Number[]) parameterValue )
          .map( Object::toString )
          .toArray( String[]::new ) );
    } else if ( parameterValue instanceof Date[] ) {
      return String.join( ", ",
        Arrays.stream( (Date[]) parameterValue )
          .map( date -> new SimpleDateFormat( "yyyy-MM-dd" ).format( date ) )
          .toArray( String[]::new ) );
    } else if ( parameterValue instanceof List<?> ) { // Handle generic lists.
      return ( (List<?>) parameterValue ).stream()
        .map( Object::toString )
        .reduce( ( a, b ) -> a + ", " + b )
        .orElse( "" );
    } else {
      throw new IllegalArgumentException( "Unsupported type: " + parameterValue.getClass().getSimpleName() );
    }
  }

  private void addPrompts( Band reportHeader, MasterReport report ) {
    ReportParameterValues parameterValues = report.getParameterValues();
    String parameterValueString = null;
    if ( parameterValues == null ) {
      return;
    }

    for ( String parameterName : parameterValues.getColumnNames() ) {
      Object parameterValue = parameterValues.get( parameterName );
      if ( parameterValue != null ) {
        parameterValueString = formatParameterValue( parameterValue );
        addLabelToReportHeader( createText( parameterName + ": " + parameterValueString ), reportHeader );
      }
    }

    if ( parameterValueString == null || parameterValueString.isEmpty() ) {
      addLabelToReportHeader( createText( NO_PROMPTS ), reportHeader );
    }
  }

  private void addLabelToReportHeader( Element label, Band reportHeader ) {
    if ( label != null && reportHeader != null ) {
      reportHeader.addElement( label );
    }
  }

  private Element createLabel( String text ) {
    LabelElementFactory labelFactory = new LabelElementFactory();
    labelFactory.setText( text );
    labelFactory.setMinimumHeight( 20f );
    labelFactory.setBold( true );
    labelFactory.setFontSize( 12 );
    labelFactory.setDynamicHeight( true );
    return labelFactory.createElement();
  }

  private Element createText( String text ) {
    LabelElementFactory textFactory = new LabelElementFactory();
    textFactory.setText( text );
    textFactory.setMinimumHeight( 20f );
    textFactory.setDynamicHeight( true );
    textFactory.setFontSize( 10 );
    return textFactory.createElement();
  }
}
