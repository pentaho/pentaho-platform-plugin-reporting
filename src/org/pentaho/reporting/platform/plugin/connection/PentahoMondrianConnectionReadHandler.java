package org.pentaho.reporting.platform.plugin.connection;

import java.io.File;
import java.util.List;

import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalogHelper;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.MondrianConnectionReadHandler;

public class PentahoMondrianConnectionReadHandler extends MondrianConnectionReadHandler
{

  public String getMondrianCubeDefinitionFile()
  {
    final String superDef = super.getMondrianCubeDefinitionFile();
    final File cubeFile = new File(superDef);
    if (cubeFile.exists()) {
      return superDef;
    }
    
    String name = cubeFile.getName();
    List<MondrianCatalog> catalogs = MondrianCatalogHelper.getInstance().listCatalogs(PentahoSessionHolder.getSession(), false);
    
    for (MondrianCatalog cat : catalogs) {
      if (cat.getDefinition().toLowerCase().endsWith(name.toLowerCase())) {
        return cat.getDefinition();
      }
    }
    
    return superDef;
  }
  
}
