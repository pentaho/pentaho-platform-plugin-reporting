package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.ReportProcessor;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.FlowRTFOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class RTFOutput implements ReportOutputHandler
{
  public RTFOutput()
  {
  }

  public Object getReportLock()
  {
    return this;
  }

  public int paginate(MasterReport report, int yieldRate) throws ReportProcessingException, IOException, ContentIOException {
    return 0;
  }
  
  public int generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate) throws ReportProcessingException, IOException
  {
    final FlowRTFOutputProcessor target = new FlowRTFOutputProcessor(report.getConfiguration(), outputStream, report.getResourceManager());
    final ReportProcessor proc = new FlowReportProcessor(report, target);

    if (yieldRate > 0)
    {
      proc.addReportProgressListener(new YieldReportListener(yieldRate));
    }
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
