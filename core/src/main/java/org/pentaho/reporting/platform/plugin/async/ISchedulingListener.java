/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import java.io.Serializable;

/**
 * Listener for report scheduling
 */
public interface ISchedulingListener {

  /**
   * Is executed when report is successfully scheduled
   *
   * @param fileId report file id
   */
  void onSchedulingCompleted( Serializable fileId );

}
