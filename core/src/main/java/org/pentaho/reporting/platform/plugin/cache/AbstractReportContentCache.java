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


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of caching policy
 */
public abstract class AbstractReportContentCache implements IReportContentCache {

  public AbstractReportContentCache() {
  }

  public AbstractReportContentCache( final ICacheBackend backend ) {
    this.backend = backend;
  }

  private ICacheBackend backend;

  @Override
  public boolean put( final String key, final IReportContent value ) {
    return put( key, value, new HashMap<String, Serializable>() );
  }

  @Override
  public boolean put( final String key, final IReportContent value, Map<String, Serializable> metaData ) {
    return getBackend().write( computeKey( key ), value, metaData );
  }

  @Override
  public IReportContent get( final String key ) {
    return (IReportContent) getBackend().read( computeKey( key ) );
  }

  @Override public Map<String, Serializable> getMetaData( String key ) {
    return getBackend().readMetaData( computeKey( key ) );
  }

  public ICacheBackend getBackend() {
    return backend;
  }

  public void setBackend( final ICacheBackend backend ) {
    this.backend = backend;
  }

  protected abstract List<String> computeKey( final String key );
}
