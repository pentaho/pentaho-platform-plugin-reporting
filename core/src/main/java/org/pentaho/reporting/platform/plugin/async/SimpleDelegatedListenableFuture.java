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

import com.google.common.util.concurrent.ListenableFuture;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Just delegates the work. Is intended to be extended.
 *
 * @param <T> type parameter
 */
public abstract class SimpleDelegatedListenableFuture<T> implements ListenableFuture<T> {

  private ListenableFuture<T> delegate;

  private AtomicBoolean canceled = new AtomicBoolean( false );

  public SimpleDelegatedListenableFuture( final ListenableFuture<T> delegate ) {
    ArgumentNullException.validate( "delegate", delegate );
    this.delegate = delegate;
  }

  @Override public boolean cancel( final boolean mayInterruptIfRunning ) {
    return canceled.compareAndSet( false, true );
  }

  @Override public boolean isCancelled() {
    return canceled.get();
  }

  @Override public boolean isDone() {
    return delegate.isDone();
  }

  @Override public T get() throws InterruptedException, ExecutionException {
    return delegate.get();
  }

  @Override public T get( final long timeout, final TimeUnit unit )
    throws InterruptedException, ExecutionException, TimeoutException {
    return delegate.get( timeout, unit );
  }

  @Override public void addListener( final Runnable listener, final Executor executor ) {
    delegate.addListener( listener, executor );
  }
}
