package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DefaultCubeFileProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.CubeFileProvider;

/**
 * Todo: Document me!
 * <p/>
 * Date: 25.08.2009
 * Time: 18:43:36
 *
 * @author Thomas Morgner.
 */
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
