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

  public Connection createConnection( final DatabaseMeta databaseMeta, final String username, final String password )
    throws ReportDataFactoryException {
    try {
      final String realUser = ( databaseMeta.getUsername() == null ) ? username : databaseMeta.getUsername();
      final String realPassword = ( databaseMeta.getPassword() == null ) ? password : databaseMeta.getPassword();

      if ( databaseMeta.getAccessType() == DatabaseMeta.TYPE_ACCESS_JNDI ) {
        final String jndiName = databaseMeta.getDatabaseName();
        if ( jndiName != null ) {
          try {
            final PentahoJndiDatasourceConnectionProvider connectionProvider =
                new PentahoJndiDatasourceConnectionProvider();
            connectionProvider.setJndiName( jndiName );
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
