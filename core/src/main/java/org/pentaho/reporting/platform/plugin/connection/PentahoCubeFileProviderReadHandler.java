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

package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DefaultCubeFileProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.CubeFileProvider;

/**
 * @deprecated the DefaultCubeFileProviderReadHandler uses the object factory to get an instance of the actual
 *             DefaultCubeFileProviderReadHandler implementation. There is no need for this crutch here anymore.
 */
@Deprecated
public class PentahoCubeFileProviderReadHandler extends DefaultCubeFileProviderReadHandler {
  public PentahoCubeFileProviderReadHandler() {
  }

  public CubeFileProvider getProvider() {
    return new PentahoCubeFileProvider( getPath(), getCubeConnectionName() );
  }
}
