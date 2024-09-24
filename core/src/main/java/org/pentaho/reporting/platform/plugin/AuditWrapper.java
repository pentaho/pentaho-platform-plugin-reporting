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
