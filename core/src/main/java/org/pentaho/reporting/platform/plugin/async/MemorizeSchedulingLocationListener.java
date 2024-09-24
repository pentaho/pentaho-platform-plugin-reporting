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
