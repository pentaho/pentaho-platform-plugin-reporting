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


package org.pentaho.reporting.platform.plugin;

/*
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved. 
 * This software was developed by Hitachi Vantara and is provided under the terms 
 * of the Mozilla Public License, Version 1.1, or any later version. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.mozilla.org/MPL/MPL-1.1.txt. The Original Code is the Pentaho 
 * BI Platform.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 *
 * Created Jan 9, 2006, Jan 15, 2009
 * @author mbatchel, mdamour
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IPentahoSystemListener;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class ReportingSystemStartupListener implements IPentahoSystemListener {
  private static final Log logger = LogFactory.getLog( ReportingSystemStartupListener.class );

  public ReportingSystemStartupListener() {
  }

  /*
   * This method will startup the classic reporting engine. If the engine is already started the result is a 'no-op'.
   * 
   * The IPentahoSession is optional here, as it is not used internally to this method or class, but it is required for
   * satisfaction of the IPentahoSystemListener interface.
   * 
   * @see org.pentaho.platform.api.engine.IPentahoSystemListener#startup(org.pentaho
   * .platform.api.engine.IPentahoSession)
   */
  public boolean startup( final IPentahoSession session ) {
    try {
      synchronized ( ClassicEngineBoot.class ) {
        if ( ClassicEngineBoot.getInstance().isBootDone() == false ) {
          ClassicEngineBoot.setUserConfig( new ReportingConfiguration() );
          ClassicEngineBoot.getInstance().start();
          logger.debug( Messages.getInstance().getString( "ReportPlugin.logDebugStartBoot" ) ); //$NON-NLS-1$

          if ( ClassicEngineBoot.getInstance().isBootFailed() ) {
            logger.warn( Messages.getInstance().getString( "ReportPlugin.logErrorGeneralBootError" ), ClassicEngineBoot
                .getInstance().getBootFailureReason() ); //$NON-NLS-1$
          }
          return true;
        }
      }
    } catch ( Exception ex ) {
      logger.warn( Messages.getInstance().getString( "ReportPlugin.logErrorFatalBootError" ), ex ); //$NON-NLS-1$
    }
    return false;
  }

  public void shutdown() {
    // Nothing required
  }

}
