/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 - 2026 by Pentaho Canada Inc. : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2030-06-15
 ******************************************************************************/



package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.JndiDataSourceProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DataSourceProvider;

public class PentahoMondrianDataSourceProviderReadHandler extends JndiDataSourceProviderReadHandler {
  public PentahoMondrianDataSourceProviderReadHandler() {
  }

  public DataSourceProvider getProvider() {
    return new PentahoMondrianDataSourceProvider( getPath() );
  }
}
