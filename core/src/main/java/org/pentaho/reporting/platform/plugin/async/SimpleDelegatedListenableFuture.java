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
