/*
 * Copyright 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the Mozilla Public License, Version 1.1, or any later version. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.mozilla.org/MPL/MPL-1.1.txt. The Original Code is the Pentaho 
 * BI Platform.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 *
 * @created Apr 6, 2009 
 * @author wseyler
 */

package org.pentaho.reporting.platform.plugin.connection;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.pentaho.platform.api.data.IDatasourceService;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.ConnectionProvider;

/**
 * @author wseyler
 *
 */
public class PentahoJndiDatasourceConnectionProvider implements ConnectionProvider {

  private String datSourceName;

  /*
   * Default constructor
   */
  public PentahoJndiDatasourceConnectionProvider() {
    super();
  }


  /* (non-Javadoc)
   * @see org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.ConnectionProvider#getConnection()
   */
  public Connection getConnection() throws SQLException {
    Connection nativeConnection = null;
    try {
      final IDatasourceService datasourceService =  PentahoSystem.getObjectFactory().get(IDatasourceService.class ,null);
      final DataSource dataSource = datasourceService.getDataSource(datSourceName);      
      if (dataSource != null) {
        nativeConnection = dataSource.getConnection();
        if (nativeConnection == null) {
          // clear datasource cache
          datasourceService.clearDataSource(datSourceName);
          throw new SQLException(Messages.getInstance().getErrorString("PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", datSourceName)); //$NON-NLS-1$
        }
      } else {
        // clear datasource cache
        datasourceService.clearDataSource(datSourceName);
        throw new SQLException(Messages.getInstance().getErrorString("PentahoDatasourceConnectionProvider.ERROR_0001_INVALID_CONNECTION", datSourceName)); //$NON-NLS-1$
      }
    } catch (Exception e) {
      try {
        IDatasourceService datasourceService =  PentahoSystem.getObjectFactory().get(IDatasourceService.class ,null);
        datasourceService.clearDataSource(datSourceName);
        throw new SQLException(Messages.getInstance().getErrorString("PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", datSourceName, e.getLocalizedMessage())); //$NON-NLS-1$
      } catch(ObjectFactoryException objface) {
        throw new SQLException(Messages.getInstance().getErrorString("PentahoDatasourceConnectionProvider.ERROR_0002_UNABLE_TO_FACTORY_OBJECT", datSourceName, e.getLocalizedMessage())); //$NON-NLS-1$
      }
    }
    return nativeConnection;
  }

  public String getJndiName() {
    return datSourceName;
  }

  /**
   * @param result
   */
  public void setJndiName(String jndiName) {
    int seperatorIndex = jndiName.lastIndexOf('\\');
    if (seperatorIndex != -1) {
      jndiName = jndiName.substring(seperatorIndex+1);
    }
    datSourceName = jndiName.trim();
  }
}
