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
