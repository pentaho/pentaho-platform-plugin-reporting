package org.pentaho.reporting.platform.plugin.connection;

import java.sql.Connection;

import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdConnectionProvider;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

public class PentahoPmdConnectionProvider extends PmdConnectionProvider
{
  public PentahoPmdConnectionProvider()
  {
  }

  public IMetadataDomainRepository getMetadataDomainRepository(final String domain,
                                                               final ResourceManager resourceManager,
                                                               final ResourceKey contextKey,
                                                               final String xmiFile) throws ReportDataFactoryException
  {
    return PentahoSystem.get(IMetadataDomainRepository.class, null);
  }


  public Connection getConnection(DatabaseMeta databaseMeta)
      throws ReportDataFactoryException
  {
    try
    {
      if (databaseMeta.getAccessType() == DatabaseMeta.TYPE_ACCESS_JNDI)
      {
        final String jndiName = databaseMeta.getDatabaseName();
        if (jndiName != null)
        {
          try
          {
            PentahoJndiDatasourceConnectionProvider connectionProvider = new PentahoJndiDatasourceConnectionProvider();
            connectionProvider.setJndiName(jndiName);
            return connectionProvider.getConnection();
          }
          catch (Exception e)
          {
            // fall back to JDBC
          }
        }
      }
    }
    catch (Exception e)
    {
      throw new ReportDataFactoryException("Unable to create a connection", e);
    }

    return super.getConnection(databaseMeta);
  }
}
