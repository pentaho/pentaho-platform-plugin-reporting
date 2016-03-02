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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by dima.prokopenko@gmail.com on 2/19/2016.
 */
public class PentahoAsyncCancellingExecutor extends ThreadPoolExecutor {

  public PentahoAsyncCancellingExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue ) {
    super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue );
  }

  public PentahoAsyncCancellingExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory ) {
    super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory );
  }

  public PentahoAsyncCancellingExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler ) {
    super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler );
  }

  public PentahoAsyncCancellingExecutor( int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
      BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler ) {
    super( corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler );
  }

  protected <T> RunnableFuture<T> newTaskFor( Callable<T> callable ) {
    if ( callable instanceof IAsyncReportExecution ) {
      return ( (IAsyncReportExecution) callable ).newTask();
    } else {
      return super.newTaskFor( callable );
    }
  }
}
