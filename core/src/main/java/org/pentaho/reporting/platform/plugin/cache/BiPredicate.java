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


package org.pentaho.reporting.platform.plugin.cache;

/**
 * Copied from JDK 1.8
 **/
public interface BiPredicate<T, U> {

  /**
   * Evaluates this predicate on the given arguments.
   *
   * @param t the first input argument
   * @param u the second input argument
   * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
   */
  boolean test( T t, U u );
}
