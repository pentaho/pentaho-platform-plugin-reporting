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

package org.pentaho.reporting.platform.plugin;

/*
 * Copyright 2006 - 2008 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
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
