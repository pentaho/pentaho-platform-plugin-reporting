/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.FormulaArgument;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.FormulaParameter;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransFromFileProducer;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.platform.plugin.RepositoryResourceLoader;

public class PentahoKettleTransFromFileProducer extends KettleTransFromFileProducer {
  public PentahoKettleTransFromFileProducer( final String repositoryName,
                                             final String transformationFile,
                                             final String stepName,
                                             final String username,
                                             final String password,
                                             final FormulaArgument[] definedArgumentNames,
                                             final FormulaParameter[] definedVariableNames ) {
    super( repositoryName, transformationFile, stepName, username, password,
      definedArgumentNames, definedVariableNames );
  }

  protected String computeFullFilename( ResourceKey key ) {
    while ( key != null ) {
      final Object schema = key.getSchema();
      if ( RepositoryResourceLoader.SOLUTION_SCHEMA_NAME.equals( schema ) == false ) {
        // these are not the droids you are looking for ..
        key = key.getParent();
        continue;
      }

      final Object identifier = key.getIdentifier();
      if ( identifier instanceof String ) {
        // get a local file reference ...
        final String file = (String) identifier;
        // pedro alves - Getting the file through normal apis
        final String fileName = PentahoSystem.getApplicationContext().getSolutionPath( file );
        if ( fileName != null ) {
          return fileName;
        }
      }
      key = key.getParent();
    }

    return super.computeFullFilename( key );
  }
}
