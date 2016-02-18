/*
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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Eviction strategy that kills old cache when accessed
 */
public class DeleteOldOnAccessCache extends AbstractReportContentCache {

  private static final Log logger = LogFactory.getLog( DeleteOldOnAccessCache.class );
  private static final String SEGMENT = "long_term";
  public static final String TIMESTAMP = "-timestamp";
  public static final int MILLIS_IN_DAY = 86400000;
  private long millisToLive;

  public DeleteOldOnAccessCache( final ICacheBackend backend ) {
    super( backend );
  }

  public void setDaysToLive( final long daysToLive ) {
    this.millisToLive = MILLIS_IN_DAY * daysToLive;
  }

  /*for testing purposes*/
  protected void setMillisToLive( long millisToLive ) {
    this.millisToLive = millisToLive;
  }

  @Override protected List<String> computeKey( final String key ) {
    return Collections.unmodifiableList( Arrays.asList( SEGMENT, key ) );
  }

  /**
   * Cleans old entries Saves value with timestamp
   *
   * @param key   key
   * @param value value
   * @return success
   */
  @Override public boolean put( final String key, final IReportContent value ) {
    cleanUp();
    if ( super.put( key, value ) ) {
      return super.getBackend()
        .write( Collections.unmodifiableList( Arrays.asList( SEGMENT, key + TIMESTAMP ) ), System.currentTimeMillis() );
    }
    return false;
  }

  /**
   * @param key key
   * @return ReportContent
   */
  @Override public IReportContent get( final String key ) {
    cleanUp();
    return super.get( key );
  }

  private void cleanUp() {
    logger.debug( "Starting periodical cache eviction" );
    final long currentTimeMillis = System.currentTimeMillis();
    final ICacheBackend backend = getBackend();
    //Check all timestamps
    for ( final String key : backend.listKeys( Collections.singletonList( SEGMENT ) ) ) {
      if ( key.matches( ".*" + TIMESTAMP ) ) {
        final List<String> compositeKey = Arrays.asList( SEGMENT, key );
        final Long timestamp = (Long) backend.read( compositeKey );
        if ( currentTimeMillis - timestamp > millisToLive ) {
          backend.purge( compositeKey );
          compositeKey.set( 1, compositeKey.get( 1 ).replace( TIMESTAMP, "" ) );
          backend.purge( compositeKey );
          logger.debug( "Purged long-term cache: " + key );
        }
      }
    }
    logger.debug( "Finished periodical cache eviction" );
  }
}
