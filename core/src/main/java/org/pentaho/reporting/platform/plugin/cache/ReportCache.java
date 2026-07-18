/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.reporting.platform.plugin.cache;

import org.pentaho.reporting.platform.plugin.output.ReportOutputHandler;

/**
 * The report cache caches output handler.
 * 
 * @author Thomas Morgner.
 */
public interface ReportCache {
  public ReportOutputHandler get( ReportCacheKey key );

  public ReportOutputHandler put( ReportCacheKey key, ReportOutputHandler report );
}
