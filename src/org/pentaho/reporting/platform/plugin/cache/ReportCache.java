package org.pentaho.reporting.platform.plugin.cache;

import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

/**
 * The report cache caches output handler.
 *
 * @author Thomas Morgner.
 */
public interface ReportCache
{
  public ReportOutputHandler get(ReportCacheKey key);

  public void put(ReportCacheKey key, ReportOutputHandler report);
}
