package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class PDFOutput implements ReportOutputHandler
{
  public PDFOutput()
  {
  }

  public Object getReportLock()
  {
    return this;
  }

  private PageableReportProcessor createProcessor(final MasterReport report,
                                                  final int yieldRate,
                                                  final OutputStream outputStream) throws ReportProcessingException
  {
    final PdfOutputProcessor outputProcessor = new PdfOutputProcessor(report.getConfiguration(), outputStream);
    final PageableReportProcessor proc = new PageableReportProcessor(report, outputProcessor);
    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    return proc;
  }

  public int paginate(MasterReport report, int yieldRate) throws ReportProcessingException, IOException, ContentIOException {
    return 0;
  }
  
  public int generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate) throws ReportProcessingException, IOException
  {
    final PageableReportProcessor proc = createProcessor(report, yieldRate, outputStream);
    try
    {
      proc.processReport();
      return 0;
    }
    finally
    {
      proc.close();
    }
  }

  public boolean supportsPagination() {
    return false;
  }

  public void close()
  {
  }
}
