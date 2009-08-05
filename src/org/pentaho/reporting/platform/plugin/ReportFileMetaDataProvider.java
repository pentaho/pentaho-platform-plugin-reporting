package org.pentaho.reporting.platform.plugin;

import java.io.InputStream;
import java.util.HashMap;

import org.pentaho.platform.api.engine.IFileInfo;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.engine.SolutionFileMetaAdapter;
import org.pentaho.platform.api.repository.ISolutionRepository;
import org.pentaho.platform.engine.core.solution.FileInfo;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.AttributeNames;
import org.pentaho.reporting.libraries.docbundle.DocumentBundle;
import org.pentaho.reporting.libraries.resourceloader.FactoryParameterKey;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

public class ReportFileMetaDataProvider extends SolutionFileMetaAdapter
{

  public ReportFileMetaDataProvider()
  {
  }

  public void setLogger(ILogger logger)
  {
  }

  public IFileInfo getFileInfo(ISolutionFile solutionFile, InputStream in)
  {
    IFileInfo fileInfo = null;
    try
    {
      fileInfo = new FileInfo();
      fileInfo.setTitle(solutionFile.getFileName());
      fileInfo.setAuthor("");
      fileInfo.setDescription(solutionFile.getFullPath());
    } catch (Exception e)
    {
      fileInfo = null;
    }
    return fileInfo;
  }

}
