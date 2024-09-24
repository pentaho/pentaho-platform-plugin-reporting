/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/
package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.platform.plugin.SimpleReportingAction;

import java.io.IOException;
import java.io.InputStream;

public class FastExportReportOutputHandlerFactory extends DefaultReportOutputHandlerFactory {
  public FastExportReportOutputHandlerFactory() {
  }

  protected ReportOutputHandler createXlsxOutput( final ReportOutputHandlerSelector selector ) throws IOException {
    if ( isXlsxAvailable() == false ) {
      return null;
    }
    InputStream input = selector.getInput( SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class );
    if ( input != null ) {
      XLSXOutput xlsxOutput = new XLSXOutput();
      xlsxOutput.setTemplateDataFromStream( input );
      return xlsxOutput;
    }

    return new FastXLSXOutput();
  }

  protected ReportOutputHandler createXlsOutput( final ReportOutputHandlerSelector selector ) throws IOException {
    if ( isXlsxAvailable() == false ) {
      return null;
    }
    InputStream input = selector.getInput( SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class );
    if ( input != null ) {
      XLSOutput xlsOutput = new XLSOutput();
      xlsOutput.setTemplateDataFromStream( input );
      return xlsOutput;
    }

    return new FastXLSOutput();
  }

  protected ReportOutputHandler createCsvOutput() {
    if ( isCsvAvailable() == false ) {
      return null;
    }
    return new FastCSVOutput();
  }

  protected ReportOutputHandler createHtmlStreamOutput( final ReportOutputHandlerSelector selector ) {
    if ( isHtmlStreamAvailable() == false ) {
      return null;
    }
    if ( selector.isUseJcrOutput() ) {
      return new FastStreamJcrHtmlOutput( computeContentHandlerPattern( selector ), selector.getJcrOutputPath() );
    } else {
      return new FastStreamHtmlOutput( computeContentHandlerPattern( selector ) );
    }
  }
}
