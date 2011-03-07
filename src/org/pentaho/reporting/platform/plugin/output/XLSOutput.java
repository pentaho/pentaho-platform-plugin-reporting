package org.pentaho.reporting.platform.plugin.output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.YieldReportListener;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.base.util.IOUtils;

public class XLSOutput implements ReportOutputHandler
{
  private byte[] templateData;
  private ProxyOutputStream proxyOutputStream;

  public XLSOutput(final InputStream templateInputStream) throws IOException
  {
    if (templateInputStream != null)
    {
      final ByteArrayOutputStream bout = new ByteArrayOutputStream();
      try
      {
        IOUtils.getInstance().copyStreams(templateInputStream, bout);
      }
      finally
      {
        templateInputStream.close();
      }
      templateData = bout.toByteArray();
    }
  }

  public Object getReportLock()
  {
    return this;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate)
      throws ReportProcessingException, IOException
  {
    return 0;
  }

  private FlowReportProcessor createProcessor(final MasterReport report, final int yieldRate)
      throws ReportProcessingException
  {
    proxyOutputStream = new ProxyOutputStream();
    final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor
        (report.getConfiguration(), proxyOutputStream, report.getResourceManager());
    target.setUseXlsxFormat(false);
    final FlowReportProcessor reportProcessor = new FlowReportProcessor(report, target);

    if (yieldRate > 0)
    {
      reportProcessor.addReportProgressListener(new YieldReportListener(yieldRate));
    }
    return reportProcessor;
  }

  public boolean generate(final MasterReport report,
                          final int acceptedPage,
                          final OutputStream outputStream,
                          final int yieldRate)
      throws ReportProcessingException, IOException
  {
    final FlowReportProcessor reportProcessor = createProcessor(report, yieldRate);
    try
    {
      proxyOutputStream.setParent(outputStream);
      if (templateData != null)
      {
        final FlowExcelOutputProcessor target = (FlowExcelOutputProcessor) reportProcessor.getOutputProcessor();
        target.setTemplateInputStream(new ByteArrayInputStream(templateData));
      }

      reportProcessor.processReport();
      outputStream.flush();
      return true;
    }
    finally
    {
      reportProcessor.close();
      proxyOutputStream.setParent(null);
      final FlowExcelOutputProcessor target = (FlowExcelOutputProcessor) reportProcessor.getOutputProcessor();
      target.setTemplateInputStream(null);
    }

  }

  public void close()
  {
  }
}
