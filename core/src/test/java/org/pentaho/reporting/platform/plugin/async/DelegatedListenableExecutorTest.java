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
import com.google.common.util.concurrent.ListeningExecutorService;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
