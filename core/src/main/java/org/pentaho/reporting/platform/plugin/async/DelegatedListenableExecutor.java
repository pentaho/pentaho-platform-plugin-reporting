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

import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Decorates ExecutorService with Listenable functionality and
 * any other functionality provided by IListenableFutureDelegator
 */
public class DelegatedListenableExecutor extends ForwardingListeningExecutorService {

  public DelegatedListenableExecutor( final ExecutorService delegateExecutor ) {
    ArgumentNullException.validate( "delegateExecutor", delegateExecutor );
    this.delegateExecutor = MoreExecutors.listeningDecorator( delegateExecutor );
  }

  private final ListeningExecutorService delegateExecutor;

  @Override protected ListeningExecutorService delegate() {
    return delegateExecutor;
  }

  @Override public <T> ListenableFuture<T> submit( final Callable<T> callable ) {
    if ( callable instanceof IListenableFutureDelegator ) {
      final ListenableFuture<T> delegateFuture = delegateExecutor.submit( callable );
      // Modify Future
      return  ( (IListenableFutureDelegator) callable ).delegate( delegateFuture );
    } else {
      return delegateExecutor.submit( callable );
    }
  }


}
