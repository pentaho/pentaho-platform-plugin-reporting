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

import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DefaultCubeFileProvider;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class PentahoCubeFileProvider extends DefaultCubeFileProvider {
  public PentahoCubeFileProvider() {
  }

  public PentahoCubeFileProvider( final String definedFile ) {
    super( definedFile );
  }

  public PentahoCubeFileProvider( final String mondrianCubeFile, final String cubeConnectionName ) {
    super( mondrianCubeFile, cubeConnectionName );
  }

  public String getCubeFile( final ResourceManager resourceManager, final ResourceKey contextKey )
    throws ReportDataFactoryException {
    // We need to handle legacy reports gracefully. If a report has the 'cubeConnectionName' property
    // set, we assume it is a new or migrated report. In that case, we only lookup the mondrian schema by
    // its name, and we will NOT search the file system or do any other magic.
    //
    // If the name is given, but not found, we report an error, in the same way a non-existing JNDI definition
    // would raise an error.
    //
    // Was added fallback - if schema wasn't found on server try to find file by full path.

    if ( StringUtils.isEmpty( getCubeConnectionName() ) == false ) {
      final IMondrianCatalogService catalogService =
          PentahoSystem.get( IMondrianCatalogService.class, PentahoSessionHolder.getSession() );
      final MondrianCatalog catalog =
          catalogService.getCatalog( getCubeConnectionName(), PentahoSessionHolder.getSession() );
      if ( catalog == null ) {
        // throw new ReportDataFactoryException( "Unable to locate mondrian schema with name '" +
        // getCubeConnectionName()
        // + "'" );
        return getLegacyCubeFile( resourceManager, contextKey );
      }
      return catalog.getDefinition();
    }

    return getLegacyCubeFile( resourceManager, contextKey );
  }

  private String getLegacyCubeFile( final ResourceManager resourceManager, final ResourceKey contextKey )
    throws ReportDataFactoryException {
    final String superDef = getMondrianCubeFile();
    if ( superDef == null ) {
      throw new ReportDataFactoryException( Messages.getInstance().getString( "ReportPlugin.noSchemaDefined" ) ); //$NON-NLS-1$
    }

    // resolve the file relative to the report for legacy reports ..
    // This will match against the filename specified in the "mondrianCubeFile" property.
    return super.getCubeFile( resourceManager, contextKey );
  }
}
