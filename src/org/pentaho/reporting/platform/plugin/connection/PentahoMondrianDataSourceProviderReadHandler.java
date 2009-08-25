package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.JndiDataSourceProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DataSourceProvider;

/**
 * Todo: Document me!
 * <p/>
 * Date: 25.08.2009
 * Time: 18:51:52
 *
 * @author Thomas Morgner.
 */
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
