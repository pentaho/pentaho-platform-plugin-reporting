package org.pentaho.reporting.platform.plugin.connection;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;
import org.pentaho.reporting.engine.classic.core.ParameterMapping;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransFromFileProducer;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.platform.plugin.RepositoryResourceLoader;
import org.pentaho.platform.engine.core.system.PentahoSystem;

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
        key = key.getParent();
        continue;
      }

      final Object identifier = key.getIdentifier();
      if (identifier instanceof String)
      {
        // get a local file reference ...
        final String file = (String) identifier;
        // pedro alves - Getting the file through normal apis
        final String fileName = PentahoSystem.getApplicationContext().getSolutionPath(file);
        if (fileName != null)
        {
          return fileName;
        }
      }
      key = key.getParent();
    }

    return super.computeFullFilename(key);
  }
}
