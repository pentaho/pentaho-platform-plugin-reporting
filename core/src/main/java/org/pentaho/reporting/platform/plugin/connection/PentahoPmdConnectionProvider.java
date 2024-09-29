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

import java.sql.Connection;


import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.metadata.repository.IMetadataDomainRepository;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdConnectionProvider;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.CommonUtil;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class PentahoPmdConnectionProvider extends PmdConnectionProvider {
  public PentahoPmdConnectionProvider() {
  }

  public IMetadataDomainRepository getMetadataDomainRepository( final String domain,
      final ResourceManager resourceManager, final ResourceKey contextKey, final String xmiFile )
    throws ReportDataFactoryException {
    return PentahoSystem.get( IMetadataDomainRepository.class, null );
  }

  PentahoJndiDatasourceConnectionProvider getJndiProvider() {
    return new PentahoJndiDatasourceConnectionProvider();
  }

  PentahoPoolDataSourceConnectionProvider getPoolProvider() {
    return new PentahoPoolDataSourceConnectionProvider();
  }

  public Connection createConnection( final DatabaseMeta databaseMeta, final String username, final String password )
    throws ReportDataFactoryException {
    try {
      final String realUser = ( databaseMeta.getUsername() == null ) ? username : databaseMeta.getUsername();
      final String realPassword = ( databaseMeta.getPassword() == null ) ? password : databaseMeta.getPassword();

      if ( databaseMeta.getAccessType() == DatabaseMeta.TYPE_ACCESS_JNDI ) {
        final String jndiName = databaseMeta.getDatabaseName();
        if ( jndiName != null ) {
          try {
            final PentahoJndiDatasourceConnectionProvider connectionProvider = getJndiProvider();
            connectionProvider.setJndiName( jndiName );
            return connectionProvider.createConnection( realUser, realPassword );
          } catch ( Exception e ) {
            // fall back to JDBC
            CommonUtil.checkStyleIgnore();
          }
        }
      }
      if ( databaseMeta.isUsingConnectionPool() ) {
        final String name = databaseMeta.getName();
        if ( name != null ) {
          try {
            final PentahoPoolDataSourceConnectionProvider connectionProvider = getPoolProvider();
            connectionProvider.setName( name );
            return connectionProvider.createConnection( realUser, realPassword );
          } catch ( Exception e ) {
            // fall back to JDBC
            CommonUtil.checkStyleIgnore();
          }
        }
      }
    } catch ( Exception e ) {
      throw new ReportDataFactoryException(
          Messages.getInstance().getString( "ReportPlugin.unableToCreateConnection" ), e ); //$NON-NLS-1$
    }

    return super.createConnection( databaseMeta, username, password );
  }
}
