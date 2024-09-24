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

package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface IAsyncReportExecution<V extends IAsyncReportState> extends Callable<IFixedSizeStreamingContent> {

  /**
   * Assigns the UUID and create task listener. This should be called before actual execution if we expect any state
   * from listener object.
   * This is called exclusively from the AsyncExecutor, which manages ids and guarantees the validity of them.
   * You can provide you own listeners to monitor execution
   * @param id
   *
   */
  void notifyTaskQueued( UUID id, List<? extends ReportProgressListener> callbackListeners );

  /**
   * Return the current state. Never null.
   * @return
     */
  V getState();

  String getReportPath();

  /**
   * Get generated content mime-type suggestion to
   * set proper http response header
   *
   * @return
   */
  String getMimeType();

  void requestPage( int page );

  boolean schedule();

  boolean preSchedule();
}
