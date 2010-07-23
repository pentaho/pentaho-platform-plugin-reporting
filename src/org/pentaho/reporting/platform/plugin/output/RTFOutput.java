package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.ReportProcessor;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.rtf.FlowRTFOutputProcessor;
import org.pentaho.reporting.engine.classic.core.util.NullOutputStream;

public class RTFOutput
{
  public static int paginate(final MasterReport report,
                             final int yieldRate) throws ReportProcessingException, IOException
  {
    FlowReportProcessor proc = null;
    try
    {
      final FlowRTFOutputProcessor target = new FlowRTFOutputProcessor
          (report.getConfiguration(), new NullOutputStream(), report.getResourceManager());
      proc = new FlowReportProcessor(report, target);

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
                                 final int yieldRate) throws ReportProcessingException, IOException
  {
    ReportProcessor proc = null;
    try
    {
      final FlowRTFOutputProcessor target = new FlowRTFOutputProcessor(report.getConfiguration(), outputStream, report.getResourceManager());
      proc = new FlowReportProcessor(report, target);

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