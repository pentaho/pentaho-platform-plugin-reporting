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
