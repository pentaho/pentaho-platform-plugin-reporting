/*!
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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.cache;

import java.util.Map;

import org.pentaho.reporting.engine.classic.core.cache.DataCacheKey;
import org.pentaho.reporting.libraries.base.util.ObjectUtilities;
import org.pentaho.reporting.platform.plugin.ParameterXmlContentHandler;

public class ReportCacheKey extends DataCacheKey {
  private String sessionId;

  public ReportCacheKey( final String sessionId, final Map<String, Object> parameter ) {
    this.sessionId = sessionId;
    for ( final Map.Entry<String, Object> entry : parameter.entrySet() ) {
      final String key = entry.getKey();
      if ( ParameterXmlContentHandler.SYS_PARAM_RENDER_MODE.equals( key ) ) {
        continue;
      }
      if ( ParameterXmlContentHandler.SYS_PARAM_ACCEPTED_PAGE.equals( key ) ) {
        continue;
      }

      addParameter( key, entry.getValue() );
    }
  }

  public String getSessionId() {
    return sessionId;
  }

  public boolean equals( final Object o ) {
    if ( this == o ) {
      return true;
    }
    if ( o == null || getClass() != o.getClass() ) {
      return false;
    }
    if ( !super.equals( o ) ) {
      return false;
    }

    final ReportCacheKey that = (ReportCacheKey) o;

    if ( ObjectUtilities.equal( sessionId, that.sessionId ) == false ) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + ( sessionId != null ? sessionId.hashCode() : 0 );
    return result;
  }
}
