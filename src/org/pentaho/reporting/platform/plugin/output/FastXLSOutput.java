package org.pentaho.reporting.platform.plugin.output;

import java.io.IOException;
import java.io.OutputStream;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.fast.xls.FastExcelReportUtil;
import org.pentaho.reporting.libraries.repository.ContentIOException;

public class FastXLSOutput implements ReportOutputHandler
{
  private ProxyOutputStream proxyOutputStream;

  public FastXLSOutput()
  {
    proxyOutputStream = new ProxyOutputStream();
  }

  public int generate(final MasterReport report,
                      final int acceptedPage,
                      final OutputStream outputStream,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    proxyOutputStream.setParent(outputStream);
    FastExcelReportUtil.processXls(report, outputStream);
    return 0;
  }

  public int paginate(final MasterReport report,
                      final int yieldRate) throws ReportProcessingException, IOException, ContentIOException
  {
    return 0;
  }

  public void close()
  {

  }

  public boolean supportsPagination()
  {
    return false;
  }

  public Object getReportLock()
  {
    return this;
  }
}
