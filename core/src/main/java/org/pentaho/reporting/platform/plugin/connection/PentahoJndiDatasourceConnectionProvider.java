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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.sql.DataSource;

import org.pentaho.platform.api.data.IDBDatasourceService;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.ConnectionProvider;

/**
 * @author wseyler
 */
public class PentahoJndiDatasourceConnectionProvider implements ConnectionProvider {
  private String jndiName;
  private String username;
  private String password;

  /*
   * Default constructor
   */
  public PentahoJndiDatasourceConnectionProvider() {
    super();
  }

  /**
   * Although named getConnection() this method should always return a new connection when being queried or should wrap
   * the connection in a way so that calls to "close()" on that connection do not prevent subsequent calls to this
   * method to fail.
   * 
   * @return
   * @throws java.sql.SQLException
   */
  public Connection createConnection( final String user, final String password ) throws SQLException {
    try {
      final IDBDatasourceService datasourceService =
          PentahoSystem.getObjectFactory().get( IDBDatasourceService.class, null );
      final DataSource dataSource = datasourceService.getDataSource( jndiName );
      if ( dataSource != null ) {
        final String realUser;
        final String realPassword;
        if ( username != null ) {
          realUser = username;
        } else {
          realUser = user;
        }
        if ( this.password != null ) {
          realPassword = this.password;
        } else {
          realPassword = password;
        }

        if ( realUser == null ) {
          final Connection connection = dataSource.getConnection();
          if ( connection == null ) {
            datasourceService.clearDataSource( jndiName );
            throw new SQLException( Messages.getInstance().getErrorString(
                "PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", jndiName ) ); //$NON-NLS-1$
          }
          return connection;
        }

        try {
          final Connection connection = dataSource.getConnection( realUser, realPassword );
          if ( connection == null ) {
            datasourceService.clearDataSource( jndiName );
            throw new SQLException( "JNDI DataSource is invalid; it returned null "
               + "without throwing a meaningful error." );
          }
          return connection;
        } catch ( UnsupportedOperationException uoe ) {
          final Connection connection = dataSource.getConnection();
          if ( connection == null ) {
            datasourceService.clearDataSource( jndiName );
            throw new SQLException( Messages.getInstance().getErrorString(
                "PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", jndiName ) ); //$NON-NLS-1$
          }
          return connection;
        } catch ( SQLException ex ) {
          final Connection nativeConnection = dataSource.getConnection();
          if ( nativeConnection == null ) {
            // clear datasource cache
            datasourceService.clearDataSource( jndiName );
            throw new SQLException( Messages.getInstance().getErrorString(
                "PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", jndiName ) ); //$NON-NLS-1$
          }
          return nativeConnection;
        }
      } else {
        // clear datasource cache
        datasourceService.clearDataSource( jndiName );
        throw new SQLException( Messages.getInstance().getErrorString(
            "PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", jndiName ) ); //$NON-NLS-1$
      }
    } catch ( Exception e ) {
      try {
        final IDBDatasourceService datasourceService =
            PentahoSystem.getObjectFactory().get( IDBDatasourceService.class, null );
        datasourceService.clearDataSource( jndiName );
        throw new SQLException(
            Messages
                .getInstance()
                .getErrorString(
                    "PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", jndiName, e.getLocalizedMessage() ) ); //$NON-NLS-1$
      } catch ( ObjectFactoryException objface ) {
        throw new SQLException(
            Messages
                .getInstance()
                .getErrorString(
                    "PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", jndiName, e.getLocalizedMessage() ) ); //$NON-NLS-1$
      }
    }
  }

  public String getJndiName() {
    return jndiName;
  }

  public void setJndiName( final String jndiName ) {
    this.jndiName = jndiName;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername( final String username ) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword( final String password ) {
    this.password = password;
  }

  public Object getConnectionHash() {
    final ArrayList<Object> list = new ArrayList<Object>();
    list.add( getClass().getName() );
    list.add( jndiName );
    list.add( username );
    return list;
  }
}
