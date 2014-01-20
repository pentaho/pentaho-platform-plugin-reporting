package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.InputStream;

import org.pentaho.reporting.platform.plugin.SimpleReportingAction;

public class FastExportReportOutputHandlerFactory extends DefaultReportOutputHandlerFactory
{
  public FastExportReportOutputHandlerFactory()
  {
  }

  protected ReportOutputHandler createXlsxOutput(final ReportOutputHandlerSelector selector) throws IOException
  {
    if (isXlsxAvailable() == false)
    {
      return null;
    }
    InputStream input = selector.getInput(SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class);
    if (input != null)
    {
      XLSXOutput xlsxOutput = new XLSXOutput(input);
      return xlsxOutput;
    }

    return new FastXLSXOutput();
  }

  protected ReportOutputHandler createXlsOutput(final ReportOutputHandlerSelector selector) throws IOException
  {
    if (isXlsxAvailable() == false)
    {
      return null;
    }
    InputStream input = selector.getInput(SimpleReportingAction.XLS_WORKBOOK_PARAM, null, InputStream.class);
    if (input != null)
    {
      XLSOutput xlsOutput = new XLSOutput(input);
      return xlsOutput;
    }

    return new FastXLSOutput();
  }

  protected ReportOutputHandler createCsvOutput()
  {
    if (isCsvAvailable() == false)
    {
      return null;
    }
    return new FastCSVOutput();
  }

  protected ReportOutputHandler createHtmlStreamOutput(final ReportOutputHandlerSelector selector)
  {
    if (isHtmlStreamAvailable() == false)
    {
      return null;
    }
    if (selector.isUseJcrOutput())
    {
      return new FastStreamJcrHtmlOutput(computeContentHandlerPattern(selector), selector.getJcrOutputPath());
    }
    else
    {
      return new FastStreamHtmlOutput(computeContentHandlerPattern(selector));
    }
  }


}
