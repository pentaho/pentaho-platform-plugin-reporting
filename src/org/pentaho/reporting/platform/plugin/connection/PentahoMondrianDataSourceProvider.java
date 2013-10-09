/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

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
