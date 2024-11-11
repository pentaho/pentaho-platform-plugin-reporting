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

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Used to store scheduled files id against job keys Use lock and unlock to achieve thread safety
 */
public class MemorizeSchedulingLocationListener {

  private ReentrantLock lock = new ReentrantLock();

  private ConcurrentHashMap<PentahoAsyncExecutor.CompositeKey, Serializable> locationMap = new ConcurrentHashMap<>();

  public void recordOutputFile( final PentahoAsyncExecutor.CompositeKey key, final Serializable fileId ) {
    locationMap.put( key, fileId );
  }

  public Serializable lookupOutputFile( final PentahoAsyncExecutor.CompositeKey key ) {
    return locationMap.get( key );
  }

  public void shutdown() {
    this.locationMap.clear();
  }

  public void onLogout( final String sessionId ) {
    this.locationMap.keySet().removeIf( k -> k.isSameSession( sessionId ) );
  }

  public void lock() {
    lock.lock();
  }

  public void unlock() {
    lock.unlock();
  }
}
