package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xml.XmlTableOutputProcessor;

public class XmlTableOutput implements ReportOutputHandler
{
  private StreamReportProcessor proc;
  private ProxyOutputStream proxyOutputStream;

  public XmlTableOutput()
  {
  }

  public Object getReportLock()
  {
    return this;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException
  {
    return 0;
  }

  private StreamReportProcessor createProcessor(final MasterReport report,
                                                final int yieldRate)
      throws ReportProcessingException
  {
    proxyOutputStream = new ProxyOutputStream();
    final XmlTableOutputProcessor target = new XmlTableOutputProcessor
        (report.getConfiguration(), proxyOutputStream);
    final StreamReportProcessor proc = new StreamReportProcessor(report, target);

    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    return proc;
  }

  public boolean generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate) throws ReportProcessingException, IOException
  {
    try
    {
      if (proc == null)
      {
        proc = createProcessor(report, yieldRate);
      }

      proxyOutputStream.setParent(outputStream);
      proc.processReport();
      return true;
    }
    finally
    {
      proxyOutputStream.setParent(null);
      outputStream.flush();
    }
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
