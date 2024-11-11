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
import java.util.Map;

public interface IReportContentCache {

  boolean put( String key, IReportContent value );

  boolean put( String key, IReportContent value, Map<String, Serializable> metaData );

  IReportContent get( String key );

  Map<String, Serializable> getMetaData( String key );

  void cleanup();

  void cleanupCurrentSession();
}
