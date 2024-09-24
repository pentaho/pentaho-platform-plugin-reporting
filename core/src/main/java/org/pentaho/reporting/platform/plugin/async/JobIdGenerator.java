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
