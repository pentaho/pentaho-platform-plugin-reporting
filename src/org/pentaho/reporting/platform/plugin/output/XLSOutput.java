package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.engine.classic.core.util.NullOutputStream;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

public class XLSOutput
{
  public static int paginate(final MasterReport report,
                                 final InputStream templateInputStream,
                                 final int yieldRate)
      throws ReportProcessingException, IOException
  {
    final ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();

    final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor(report.getConfiguration(), new NullOutputStream(), resourceManager);
    final FlowReportProcessor reportProcessor = new FlowReportProcessor(report, target);

    if (templateInputStream != null)
    {
      target.setTemplateInputStream(templateInputStream);
    }

    if (yieldRate > 0)
    {
      reportProcessor.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    reportProcessor.paginate();
    reportProcessor.close();
    return reportProcessor.getPhysicalPageCount();
  }

  public static boolean generate(final MasterReport report,
                                 final OutputStream outputStream,
                                 final InputStream templateInputStream,
                                 final int yieldRate)
      throws ReportProcessingException, IOException
  {
    final ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();

    final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor(report.getConfiguration(), outputStream, resourceManager);
    final FlowReportProcessor reportProcessor = new FlowReportProcessor(report, target);

    if (templateInputStream != null)
    {
      target.setTemplateInputStream(templateInputStream);
    }

    if (yieldRate > 0)
    {
      reportProcessor.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    reportProcessor.processReport();
    reportProcessor.close();
    outputStream.flush();
    outputStream.close();
    return true;
  }
}
