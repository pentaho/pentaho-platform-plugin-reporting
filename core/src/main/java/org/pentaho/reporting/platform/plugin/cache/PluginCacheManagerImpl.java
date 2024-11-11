/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin.cache;

public class PluginCacheManagerImpl implements IPluginCacheManager {

  public PluginCacheManagerImpl( final IReportContentCache strategy ) {
    this.strategy = strategy;
  }

  public PluginCacheManagerImpl() {
  }

  private IReportContentCache strategy;

  public IReportContentCache getStrategy() {
    return strategy;
  }

  public void setStrategy( final IReportContentCache strategy ) {
    this.strategy = strategy;
  }

  @Override
  public IReportContentCache getCache() {
    return strategy;
  }

}
