/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this
 *  program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  or from the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved. *
 */
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
