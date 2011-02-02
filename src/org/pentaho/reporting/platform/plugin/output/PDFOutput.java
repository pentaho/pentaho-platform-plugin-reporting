package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.util.NullOutputStream;

public class PDFOutput implements ReportOutputHandler
{
  private PageableReportProcessor proc;

  public PDFOutput()
  {
  }

  private PageableReportProcessor createProcessor(final MasterReport report,
                                                  final int yieldRate) throws ReportProcessingException
  {
    final PdfOutputProcessor outputProcessor = new PdfOutputProcessor(report.getConfiguration(), new NullOutputStream());
    final PageableReportProcessor proc = new PageableReportProcessor(report, outputProcessor);
    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    return proc;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException
  {
    if (proc == null)
    {
      proc = createProcessor(report, yieldRate);
    }

    proc.paginate();
    return proc.getPhysicalPageCount();
  }

  public boolean generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate) throws ReportProcessingException, IOException
  {
    if (proc == null)
    {
      proc = createProcessor(report, yieldRate);
    }
    try
    {
      proc.processReport();
      return true;
    }
    finally
    {
      outputStream.close();
    }
  }

  public void close()
  {
    if (proc != null)
    {
      proc.close();
    }
  }
}
