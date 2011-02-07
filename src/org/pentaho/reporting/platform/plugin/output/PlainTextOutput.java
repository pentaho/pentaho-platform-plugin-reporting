package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.PageableTextOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.plaintext.driver.TextFilePrinterDriver;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class PlainTextOutput implements ReportOutputHandler
{
  private PageableReportProcessor proc;
  private ProxyOutputStream proxyOutputStream;

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    return 0;
  }

  public boolean generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    if (proc == null)
    {
      this.proc = create(report, yieldRate);
    }
    if (proc.isPaginated() == false)
    {
      proc.paginate();
    }

    proxyOutputStream.setParent(outputStream);
    try
    {
      proc.processReport();
      proc.close();
      proc = null;
    }
    finally
    {
      proxyOutputStream.setParent(null);
      outputStream.close();
    }
    return true;
  }

  private PageableReportProcessor create(final MasterReport report, final int yieldRate)
      throws ReportProcessingException
  {
    proxyOutputStream = new ProxyOutputStream();
    final TextFilePrinterDriver driver = new TextFilePrinterDriver(proxyOutputStream, 12, 6);
    final PageableTextOutputProcessor outputProcessor = new PageableTextOutputProcessor(driver, report.getConfiguration());
    final PageableReportProcessor proc = new PageableReportProcessor(report, outputProcessor);
    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    return proc;
  }

  public void close()
  {
    if (proc != null)
    {
      proc.close();
      proc = null;
      proxyOutputStream = null;
    }
  }
}
