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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.staging;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by dima.prokopenko@gmail.com on 2/3/2016.
 */
public abstract class AbstractStagingHandler implements StagingHandler {

  protected OutputStream outputStream;
  protected IPentahoSession userSession;

  public AbstractStagingHandler ( final OutputStream outputStream, final IPentahoSession userSession ) throws IOException {
    this.outputStream = outputStream;
    this.userSession = userSession;
    initialize();
  }

  // factory method
  public static StagingHandler getStagingHandlerImpl( final OutputStream outputStream, final IPentahoSession userSession, StagingMode mode )
      throws IOException {
    // so sad that StagingMode is not enum...
    if ( mode.equals( StagingMode.THRU ) ) {
      return new ThruStagingHandler( outputStream, userSession );
    } else if ( mode.equals( StagingMode.TMPFILE ) ) {
      return new TempFileStagingHandler( outputStream, userSession );
    } else if ( mode.equals( StagingMode.MEMORY ) ) {
      return new MemStagingHandler( outputStream, userSession );
    } else {
      return new ThruStagingHandler( outputStream, userSession );
    }
  }

  protected abstract void initialize() throws IOException;
}
