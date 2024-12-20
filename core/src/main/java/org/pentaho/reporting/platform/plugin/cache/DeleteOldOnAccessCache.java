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
    return put( key, value, new HashMap<>() );
  }

  /**
   * Cleans old entries Saves value with timestamp
   *
   * @param key   key
   * @param value value
   * @param metaData metaData
   * @return success
   */
  @Override public boolean put( final String key, final IReportContent value,  Map<String, Serializable> metaData ) {
    cleanUp();

    metaData.put( TIMESTAMP, System.currentTimeMillis() );
    getBackend().write( computeKey( key ), value, metaData );
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

  /**
   * @param key key
   * @return Map<String, Serializable>
   */
  @Override public Map<String, Serializable> getMetaData( String key ) {
    cleanUp();
    return super.getMetaData( key );
  }

  /**
   * Cleans old files
   */
  @Override public void cleanup() {
    cleanUp();
  }

  @Override public void cleanupCurrentSession() {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final List<String> key = Collections.unmodifiableList( Arrays.asList( SEGMENT, createKey( session.getName() ) ) );
    final ICacheBackend backend = getBackend();
    backend.purgeSegment( key, ( k, m ) -> true );
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
