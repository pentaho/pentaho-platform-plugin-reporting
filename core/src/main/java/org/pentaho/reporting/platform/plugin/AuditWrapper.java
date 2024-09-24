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

package org.pentaho.reporting.platform.plugin;

import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.engine.core.audit.AuditHelper;

/**
 * Wrapper to a static AuditHelper call. Simplify mocking and get rid of static dependencies in main code.
 */
public class AuditWrapper {

  public static final AuditWrapper NULL = new NullAudtitWrapper();

  public void audit( String instanceId, final String userId, String actionName, final String objectType,
                     String processId, final String messageType, final String message, final String value, final float duration,
                     final ILogger logger ) {
    // register execution attempt
    AuditHelper.audit( instanceId, userId, actionName, objectType, processId,
      messageType, message, value, duration, logger );
  }

  public static class NullAudtitWrapper extends AuditWrapper {

    @Override
    public void audit( String instanceId, final String userId, String actionName, final String objectType,
                       String processId, final String messageType, final String message, final String value, final float duration,
                       final ILogger logger ) {
      // no op
    }
  }
}
