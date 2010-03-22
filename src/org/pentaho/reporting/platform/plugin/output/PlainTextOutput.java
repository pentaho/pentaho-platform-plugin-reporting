package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PageableTextOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PlainTextPageableModule;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.driver.TextFilePrinterDriver;
import org.pentaho.reporting.engine.classic.core.util.StringUtil;

public class PlainTextOutput
{

  public static boolean generate(final MasterReport report,
                                 final OutputStream outputStream,
                                 final int yieldRate) throws ReportProcessingException, IOException
  {
    final float charPerInch = StringUtil.parseFloat(report.getReportConfiguration().getConfigProperty
        (PlainTextPageableModule.CHARS_PER_INCH), 12.0f);
    final float linesPerInch = StringUtil.parseFloat(report.getReportConfiguration().getConfigProperty
        (PlainTextPageableModule.LINES_PER_INCH), 6.0f);

    final TextFilePrinterDriver driver = new TextFilePrinterDriver(outputStream, charPerInch, linesPerInch);
    
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
