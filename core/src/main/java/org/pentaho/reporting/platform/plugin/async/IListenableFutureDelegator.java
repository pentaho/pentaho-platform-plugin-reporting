/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.reporting.platform.plugin.async;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Interface to implement in order to return extended ListenableFuture
 *
 * @param <T> type parameter
 */
public interface IListenableFutureDelegator<T> {

  ListenableFuture<T> delegate( ListenableFuture<T> delegate );

}
