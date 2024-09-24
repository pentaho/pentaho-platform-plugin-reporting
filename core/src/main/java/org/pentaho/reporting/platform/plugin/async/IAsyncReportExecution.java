/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

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
