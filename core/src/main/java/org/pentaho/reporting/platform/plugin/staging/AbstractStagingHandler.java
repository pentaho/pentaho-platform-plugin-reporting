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


package org.pentaho.reporting.platform.plugin.staging;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;

import java.io.IOException;
import java.io.OutputStream;

public abstract class AbstractStagingHandler implements StagingHandler {

  protected OutputStream outputStream;
  protected IPentahoSession userSession;

  public AbstractStagingHandler( final OutputStream outputStream, final IPentahoSession userSession ) throws IOException {
    this.outputStream = outputStream;
    this.userSession = userSession;
    initialize();
  }

  // factory method
  public static StagingHandler getStagingHandlerImpl( final OutputStream outputStream, final IPentahoSession userSession, final StagingMode mode )
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
