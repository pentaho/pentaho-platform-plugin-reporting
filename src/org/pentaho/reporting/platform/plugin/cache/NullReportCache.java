package org.pentaho.reporting.platform.plugin.cache;

import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

/**
 * Todo: Document me!
 * <p/>
 * Date: 09.02.11
 * Time: 15:47
 *
 * @author Thomas Morgner.
 */
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
