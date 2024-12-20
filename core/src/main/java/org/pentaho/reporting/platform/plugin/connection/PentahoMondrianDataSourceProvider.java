/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.connection;

import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DataSourceProvider;

public class PentahoMondrianDataSourceProvider implements DataSourceProvider {
  private String dataSourceName;

  public PentahoMondrianDataSourceProvider( final String dataSourceName ) {
    this.dataSourceName = dataSourceName;
  }

  public DataSource getDataSource() throws SQLException {
    try {
      final IDBDatasourceService datasourceService =
          PentahoSystem.getObjectFactory().get( IDBDatasourceService.class, null );
      final DataSource dataSource = datasourceService.getDataSource( dataSourceName );
      if ( dataSource != null ) {
        return dataSource;
      } else {
        // clear datasource cache
        datasourceService.clearDataSource( dataSourceName );
        throw new SQLException( Messages.getInstance().getErrorString(
            "PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", dataSourceName ) ); //$NON-NLS-1$
      }
    } catch ( Exception e ) {
      try {
        final IDBDatasourceService datasourceService =
            PentahoSystem.getObjectFactory().get( IDBDatasourceService.class, null );
        datasourceService.clearDataSource( dataSourceName );
        throw new SQLException(
            Messages
                .getInstance()
                .getErrorString(
                    "PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", dataSourceName, e.getLocalizedMessage() ) ); //$NON-NLS-1$
      } catch ( ObjectFactoryException objface ) {
        throw new SQLException(
            Messages
                .getInstance()
                .getErrorString(
                    "PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", dataSourceName, e.getLocalizedMessage() ) ); //$NON-NLS-1$
      }
    }
  }

  public Object getConnectionHash() {
    final ArrayList<Object> list = new ArrayList<Object>();
    list.add( getClass().getName() );
    list.add( dataSourceName );
    return list;
  }

}
