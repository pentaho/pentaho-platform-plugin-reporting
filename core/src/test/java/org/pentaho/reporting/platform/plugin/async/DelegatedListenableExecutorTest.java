/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class DelegatedListenableExecutorTest {
  @Test
  public void delegate() throws Exception {
    final ExecutorService exec = mock( ExecutorService.class );
    final DelegatedListenableExecutor delegatedListenableExecutor = new DelegatedListenableExecutor( exec );
    final ListeningExecutorService delegate = delegatedListenableExecutor.delegate();
    final Callable callable = mock( Callable.class );
    delegate.submit( callable );
    verify( exec, times( 1 ) ).execute( any( Runnable.class ) );
  }

  @Test
  public void submit() throws Exception {
    final ExecutorService exec = mock( ExecutorService.class );
    final DelegatedListenableExecutor delegatedListenableExecutor = new DelegatedListenableExecutor( exec );
    final Callable callable = mock( Callable.class );
    delegatedListenableExecutor.submit( callable );
    verify( exec, times( 1 ) ).execute( any( Runnable.class ) );
    final AbstractAsyncReportExecution delegator = mock( AbstractAsyncReportExecution.class );
    delegatedListenableExecutor.submit( delegator );
    verify( exec, times( 2 ) ).execute( any( Runnable.class ) );
    verify( delegator, times( 1 ) ).delegate( any( ListenableFuture.class ) );
  }

}
