package org.pentaho.reporting.platform.plugin.connection;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.pentaho.reporting.engine.classic.core.ParameterMapping;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransFromFileProducer;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.platform.plugin.RepositoryResourceLoader;

/**
 * Todo: Document me!
 * <p/>
 * Date: 05.01.2010
 * Time: 17:42:40
 *
 * @author Thomas Morgner.
 */
public class PentahoKettleTransFromFileProducer extends KettleTransFromFileProducer
{
  public PentahoKettleTransFromFileProducer(final String repositoryName,
                                            final String transformationFile,
                                            final String stepName,
                                            final String username,
                                            final String password,
                                            final String[] definedArgumentNames,
                                            final ParameterMapping[] definedVariableNames)
  {
    super(repositoryName, transformationFile, stepName, username, password, definedArgumentNames, definedVariableNames);
  }


  protected String computeFullFilename(ResourceKey key)
  {
    while (key != null)
    {
      final Object schema = key.getSchema();
      if (RepositoryResourceLoader.SOLUTION_SCHEMA_NAME.equals(schema) == false)
      {
        // these are not the droids you are looking for ..
        continue;
      }

      final Object identifier = key.getIdentifier();
      if (identifier instanceof String)
      {
        // get a local file reference ...
        try
        {
          final String file = (String) identifier;
          final FileSystemManager fileSystemManager = VFS.getManager();
          final String[] schemes = fileSystemManager.getSchemes();
          // We try to create a file-object. If that works, we are talking about a VFS path and we can be happy.
          //noinspection UnusedDeclaration
          final FileObject fileObject = fileSystemManager.resolveFile("solution:/" + file);
          if (fileObject.exists())
          {
            return "solution:/" + file;
          }
        }
        catch (FileSystemException e)
        {
          // ignored.
          e.printStackTrace();
        }

      }
      key = key.getParent();
    }

    return super.computeFullFilename(key);
  }
}
