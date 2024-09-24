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
import java.util.List;
import java.util.Map;

/**
 * Simple interface for cache backend
 */
public interface ICacheBackend {

  /**
   * Persist object
   *
   * @param key   path
   * @param value object
   * @return if operation succeed
   */
  boolean write( List<String> key, Serializable value, Map<String, Serializable> metaData );

  /**
   * Retrive object from storage
   *
   * @param key path
   * @return object
   */
  Serializable read( List<String> key );

  Map<String, Serializable> readMetaData( final List<String> key );

  /**
   * Remove object from storage
   *
   * @param key path
   * @return if operation succeed
   */
  boolean purge( List<String> key );

  void purgeSegment( final List<String> key, final BiPredicate<List<String>, Map<String, Serializable>> p );

}
