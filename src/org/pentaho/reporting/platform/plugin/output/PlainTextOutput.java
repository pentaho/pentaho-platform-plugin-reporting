package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PageableTextOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.driver.TextFilePrinterDriver;

public class PlainTextOutput
{
  public static int paginate(final MasterReport report,
                                 final int yieldRate,
                                 final TextFilePrinterDriver driver) throws ReportProcessingException, IOException
  {
    PageableReportProcessor proc = null;
    try
    {
      final PageableTextOutputProcessor outputProcessor = new PageableTextOutputProcessor(driver, report.getConfiguration());

      proc = new PageableReportProcessor(report, outputProcessor);
      if (yieldRate > 0)
      {
        proc.addReportProgressListener(new YieldReportListener(yieldRate));
      }
      proc.processReport();
      return proc.getPhysicalPageCount();
    }
    finally
    {
      if (proc != null)
      {
        proc.close();
      }
    }
  }

  public static boolean generate(final MasterReport report,
                                 final OutputStream outputStream,
                                 final int yieldRate,
                                 final TextFilePrinterDriver driver) throws ReportProcessingException, IOException
  {
    PageableReportProcessor proc = null;
    try
    {
      final PageableTextOutputProcessor outputProcessor = new PageableTextOutputProcessor(driver, report.getConfiguration());

      proc = new PageableReportProcessor(report, outputProcessor);
      if (yieldRate > 0)
      {
        proc.addReportProgressListener(new YieldReportListener(yieldRate));
      }
      proc.processReport();
      proc.close();
      proc = null;
      outputStream.close();
      return true;
    }
    finally
    {
      if (proc != null)
      {
        proc.close();
      }
    }
  }
}