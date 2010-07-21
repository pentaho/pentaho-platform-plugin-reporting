package org.pentaho.reporting.platform.plugin;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IFileInfo;
import org.pentaho.platform.api.engine.ILogger;
import org.pentaho.platform.api.engine.ISolutionFile;
import org.pentaho.platform.api.engine.SolutionFileMetaAdapter;
import org.pentaho.platform.engine.core.solution.FileInfo;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.libraries.base.util.StringUtils;
import org.pentaho.reporting.libraries.docbundle.DocumentBundle;
import org.pentaho.reporting.libraries.docbundle.DocumentMetaData;
import org.pentaho.reporting.libraries.docbundle.ODFMetaAttributeNames;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.platform.plugin.messages.Messages;

public class ReportFileMetaDataProvider extends SolutionFileMetaAdapter
{
  private static final Log logger = LogFactory.getLog(ReportFileMetaDataProvider.class);

  public ReportFileMetaDataProvider()
  {
  }

  public void setLogger(final ILogger logger)
  {
  }

  private DocumentMetaData loadMetaData(final String reportDefinitionPath) throws ResourceException
  {
    final ResourceManager resourceManager = new ResourceManager();
    resourceManager.registerDefaults();
    final HashMap helperObjects = new HashMap();
    // add the runtime context so that PentahoResourceData class can get access
    // to the solution repo
    final ResourceKey key = resourceManager.createKey
        (RepositoryResourceLoader.SOLUTION_SCHEMA_NAME + RepositoryResourceLoader.SCHEMA_SEPARATOR
            + reportDefinitionPath, helperObjects);
    final Resource resource = resourceManager.create(key, null, DocumentBundle.class);
    final DocumentBundle bundle = (DocumentBundle) resource.getResource();
    return bundle.getMetaData();
  }

  public IFileInfo getFileInfo(final ISolutionFile solutionFile, final InputStream in)
  {
    try
    {
      final DocumentMetaData metaData = loadMetaData(solutionFile.getFullPath());
      final String title = (String) metaData.getBundleAttribute
          (ODFMetaAttributeNames.DublinCore.NAMESPACE, ODFMetaAttributeNames.DublinCore.TITLE);
      final String author = (String) metaData.getBundleAttribute
          (ODFMetaAttributeNames.DublinCore.NAMESPACE, ODFMetaAttributeNames.DublinCore.CREATOR);
      final String description = (String) metaData.getBundleAttribute
          (ODFMetaAttributeNames.DublinCore.NAMESPACE, ODFMetaAttributeNames.DublinCore.DESCRIPTION);
      final IFileInfo fileInfo = new FileInfo();
      if (StringUtils.isEmpty(title))
      {
        fileInfo.setTitle(solutionFile.getFileName());
      }
      else
      {
        fileInfo.setTitle(title);
      }
      fileInfo.setAuthor(author); //$NON-NLS-1$
      fileInfo.setDescription(description);

      // displaytype is a magical constant defined in a internal class of the platform.
      if ("false".equals(metaData.getBundleAttribute(ClassicEngineBoot.METADATA_NAMESPACE, "visible")))
      {
        fileInfo.setDisplayType("none"); // NON-NLS
      }
      else
      {
        fileInfo.setDisplayType("report"); // NON-NLS
      }
      return fileInfo;
    }
    catch (Exception e)
    {
      logger.warn(Messages.getInstance().getString("ReportPlugin.errorMetadataNotReadable"), e);
      return null;
    }
  }

}
