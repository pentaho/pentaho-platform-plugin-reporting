/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/
package org.pentaho.reporting.platform.plugin.cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Session cache implementation - cache is killed when http session stops
 */
public class PluginSessionCache extends AbstractReportContentCache {

  private static final Log logger = LogFactory.getLog( PluginSessionCache.class );
  private static final String SEGMENT = "session";

  public PluginSessionCache( final ICacheBackend backend ) {
    super( backend );
    PentahoSystem.addLogoutListener( new LogoutHandler() );
  }

  public PluginSessionCache() {
    PentahoSystem.addLogoutListener( new LogoutHandler() );
  }

  /**
   * Glues session id with key
   *
   * @param key key
   * @return concatenated value
   */
  @Override
  protected List<String> computeKey( final String key ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    return Collections.unmodifiableList( Arrays.asList( SEGMENT, session.getId(), key ) );
  }

  /**
   * Cleans all session files
   */
  @Override public void cleanup() {
    getBackend().purge( Collections.singletonList( SEGMENT ) );
  }

  @Override public void cleanupCurrentSession() {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    getBackend().purge(  Collections.unmodifiableList( Arrays.asList( SEGMENT, session.getId() ) ) );
  }

  /**
   * Logout listener that purges cache
   */
  private class LogoutHandler implements ILogoutListener {
    private LogoutHandler() {
    }

    /**
     * Very straightforward clean up when session ends
     *
     * @param session session
     */
    public void onLogout( final IPentahoSession session ) {
      logger.debug( "Shutting down session " + session.getId() );
      final ICacheBackend backend = getBackend();
      backend.purge( Collections.unmodifiableList( Arrays.asList( SEGMENT, session.getId() ) ) );
      logger.debug( "Purged session cache " + session.getId() );
    }
  }
}
