package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.JndiDataSourceProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DataSourceProvider;

public class PentahoMondrianDataSourceProviderReadHandler extends JndiDataSourceProviderReadHandler
{
  public PentahoMondrianDataSourceProviderReadHandler()
  {
  }

  public DataSourceProvider getProvider()
  {
    return new PentahoMondrianDataSourceProvider(getPath());
  }
}
