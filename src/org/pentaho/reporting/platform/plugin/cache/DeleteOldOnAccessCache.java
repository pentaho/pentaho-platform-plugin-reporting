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
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.reporting.libraries.xmlns.parser.Base64;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eviction strategy that kills old cache when accessed
 */
public class DeleteOldOnAccessCache extends AbstractReportContentCache {

  private static final Log logger = LogFactory.getLog( DeleteOldOnAccessCache.class );
  private static final String SEGMENT = "long_term";
  public static final String TIMESTAMP = "timestamp";
  public static final int MILLIS_IN_DAY = 86400000;
  public static final String ANONYMOUS = "anonymous";
  private long millisToLive;


  public DeleteOldOnAccessCache( final ICacheBackend backend ) {
    super( backend );
  }

  public DeleteOldOnAccessCache() {
  }

  public void setDaysToLive( final long daysToLive ) {
    this.millisToLive = MILLIS_IN_DAY * daysToLive;
  }

  /*for testing purposes*/
  protected void setMillisToLive( final long millisToLive ) {
    this.millisToLive = millisToLive;
  }

  @Override protected List<String> computeKey( final String key ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    //Don't use username explicitly - compute hash
    return Collections.unmodifiableList( Arrays.asList( SEGMENT, createKey( session.getName() ), key ) );
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

    final HashMap<String, Serializable> metadata = new HashMap<>();
    metadata.put( TIMESTAMP, System.currentTimeMillis() );
    getBackend().write( computeKey( key ), value, metadata );
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

    backend.purgeSegment( Collections.singletonList( SEGMENT ),
      new BiPredicate<List<String>, Map<String, Serializable>>() {
        @Override public boolean test( final List<String> key, final Map<String, Serializable> md ) {
          final Object o = md.get( TIMESTAMP );
          if ( o instanceof Long ) {
            final long timestamp = (Long) o;
            if ( currentTimeMillis - timestamp > millisToLive ) {
              logger.debug( "Purged long-term cache: " + key );
              return true;
            }
          }
          return false;
        }
      } );

    logger.debug( "Finished periodical cache eviction" );
  }


  private String createKey( final String key ) {
    if ( StringUtil.isEmpty( key ) ) {
      return ANONYMOUS;
    }
    try {
      final MessageDigest md = MessageDigest.getInstance( "SHA-256" );
      md.update( key.getBytes() );
      final byte[] digest = md.digest();
      return new String( Base64.encode( digest ) );
    } catch ( final NoSuchAlgorithmException e ) {
      throw new Error( e );
    }
  }
}
