/*
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
 * Copyright 2010-2013 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.connection;

import java.io.File;
import java.util.List;

import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalogHelper;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.DefaultCubeFileProvider;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.base.util.IOUtils;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class PentahoCubeFileProvider extends DefaultCubeFileProvider
{

  public PentahoCubeFileProvider(final String definedFile)
  {
    setMondrianCubeFile(definedFile);
  }

  public String getCubeFile(final ResourceManager resourceManager,
                            final ResourceKey contextKey) throws ReportDataFactoryException
  {
    final String superDef = getMondrianCubeFile();
    if (superDef == null)
    {
      throw new ReportDataFactoryException(Messages.getInstance().getString("ReportPlugin.noSchemaDefined")); //$NON-NLS-1$
    }

    final File cubeFile = new File(superDef);

    final String name = cubeFile.getName();
    final List<MondrianCatalog> catalogs =
        MondrianCatalogHelper.getInstance().listCatalogs(PentahoSessionHolder.getSession(), false);

    for (final MondrianCatalog cat : catalogs)
    {
      final String definition = cat.getDefinition();
      final String definitionFileName = IOUtils.getInstance().getFileName(definition);
      if (definitionFileName.equals(name))
      {
        return cat.getDefinition();
      }
    }

    // resolve relative to the report ..
    return super.getCubeFile(resourceManager, contextKey);
  }
}
