package org.pentaho.reporting.platform.plugin.connection;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.ArrayList;
import javax.sql.DataSource;

import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.JndiDataSourceProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DataSourceProvider;
import org.pentaho.platform.api.data.IDatasourceService;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

/**
 * Todo: Document me!
 * <p/>
 * Date: 25.08.2009
 * Time: 18:45:33
 *
 * @author Thomas Morgner.
 */
public class PentahoMondrianDataSourceProvider implements DataSourceProvider
{
  private String dataSourceName;

  public PentahoMondrianDataSourceProvider(final String dataSourceName)
  {
    this.dataSourceName = dataSourceName;
  }

  public DataSource getDataSource() throws SQLException
  {
    try
    {
      final IDatasourceService datasourceService = PentahoSystem.getObjectFactory().get(IDatasourceService.class, null);
      final DataSource dataSource = datasourceService.getDataSource(dataSourceName);
      if (dataSource != null)
      {
        return dataSource;
      }
      else
      {
        // clear datasource cache
        datasourceService.clearDataSource(dataSourceName);
        throw new SQLException(Messages.getErrorString("PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", dataSourceName)); //$NON-NLS-1$
      }
    }
    catch (Exception e)
    {
      try
      {
        IDatasourceService datasourceService = PentahoSystem.getObjectFactory().get(IDatasourceService.class, null);
        datasourceService.clearDataSource(dataSourceName);
        throw new SQLException(Messages.getErrorString("PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", dataSourceName, e.getLocalizedMessage())); //$NON-NLS-1$
      }
      catch (ObjectFactoryException objface)
      {
        throw new SQLException(Messages.getErrorString("PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", dataSourceName, e.getLocalizedMessage())); //$NON-NLS-1$
      }
    }
  }

  public Object getConnectionHash()
  {
    final ArrayList<Object> list = new ArrayList<Object>();
    list.add(getClass().getName());
    list.add(dataSourceName);
    return list;
  }

}
