package org.pentaho.reporting.platform.plugin.cache;

import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

public class NullReportCache implements ReportCache
{
  public NullReportCache()
  {
  }

  public ReportOutputHandler get(final ReportCacheKey key)
  {
    return null;
  }

  public ReportOutputHandler put(final ReportCacheKey key, final ReportOutputHandler report)
  {
    return report;
  }
}
