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
