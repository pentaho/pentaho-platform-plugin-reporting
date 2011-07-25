package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DefaultCubeFileProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.CubeFileProvider;

public class PentahoCubeFileProviderReadHandler extends DefaultCubeFileProviderReadHandler
{
  public PentahoCubeFileProviderReadHandler()
  {
  }

  public CubeFileProvider getProvider()
  {
    return new PentahoCubeFileProvider(getPath());
  }
}
