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

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.Future;

public interface IPentahoAsyncExecutor<TReportState extends IAsyncReportState> {

  Future<IFixedSizeStreamingContent> getFuture( UUID id, IPentahoSession session );

  void cleanFuture( UUID id, IPentahoSession session );

  UUID addTask( IAsyncReportExecution<TReportState> task, IPentahoSession session );

  UUID addTask( IAsyncReportExecution<TReportState> task, IPentahoSession session, UUID uuid );

  TReportState getReportState( UUID id, IPentahoSession session );

  void requestPage( UUID id, IPentahoSession session, int page );

  boolean schedule( UUID uuid, IPentahoSession session );

  boolean preSchedule( UUID uuid, IPentahoSession session );

  UUID recalculate( UUID uuid, IPentahoSession session );

  void updateSchedulingLocation( UUID uuid, IPentahoSession session, Serializable folderId, String newName );

  void shutdown();
}
