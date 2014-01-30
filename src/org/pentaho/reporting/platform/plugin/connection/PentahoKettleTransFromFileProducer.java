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

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.FormulaArgument;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.FormulaParameter;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransFromFileProducer;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.platform.plugin.RepositoryResourceLoader;

public class PentahoKettleTransFromFileProducer extends KettleTransFromFileProducer {
  public PentahoKettleTransFromFileProducer(final String repositoryName,
                                            final String transformationFile,
                                            final String stepName,
                                            final String username,
                                            final String password,
                                            final FormulaArgument[] definedArgumentNames,
                                            final FormulaParameter[] definedVariableNames) {
    super(repositoryName, transformationFile, stepName, username, password,
        definedArgumentNames, definedVariableNames);
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

    return super.computeFullFilename(key);
  }
}
