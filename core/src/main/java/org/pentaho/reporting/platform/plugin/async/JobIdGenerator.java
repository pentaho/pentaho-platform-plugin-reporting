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

import com.google.common.collect.Sets;
import org.pentaho.platform.api.engine.ILogoutListener;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JobIdGenerator implements ILogoutListener, IJobIdGenerator {

  private static final String SESSION = "session";
  public static final String ID = "id";


  private ConcurrentHashMap<IPentahoSession, Set<Object>> reservedIds = new ConcurrentHashMap<>();

  public JobIdGenerator() {
    PentahoSystem.addLogoutListener( this );
  }

  @Override public void onLogout( final IPentahoSession session ) {
    if ( session != null ) {
      reservedIds.remove( session );
    }
  }

  @Override public UUID generateId( final IPentahoSession session ) {
    ArgumentNullException.validate( SESSION, session );
    final Set<Object> freshSet = Sets.newConcurrentHashSet();
    final UUID uuid = UUID.randomUUID();

    Set<Object> sessionIds = reservedIds.putIfAbsent( session, freshSet );
    if ( sessionIds == null ) {
      sessionIds = freshSet;
    }
    sessionIds.add( uuid );
    return uuid;
  }

  @Override public boolean acquire( final IPentahoSession session, final UUID id ) {
    ArgumentNullException.validate( SESSION, session );
    //It is totally safe if id is null but requires not null session
    return reservedIds.getOrDefault( session, Collections.emptySet() ).remove( id );
  }
}
