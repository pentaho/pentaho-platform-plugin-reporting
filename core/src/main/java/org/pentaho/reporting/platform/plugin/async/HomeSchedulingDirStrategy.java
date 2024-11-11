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


package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.repository2.ClientRepositoryPaths;

/**
 * Use home directory as scheduling directory
 */
public class HomeSchedulingDirStrategy implements ISchedulingDirectoryStrategy {

  @Override public RepositoryFile getSchedulingDir( final IUnifiedRepository repo ) {
    final IPentahoSession session = PentahoSessionHolder.getSession();

    if ( session != null ) {
      final String userHomeFolderPath = ClientRepositoryPaths.getUserHomeFolderPath( session.getName() );
      return repo.getFile( userHomeFolderPath );
    }

    return null;
  }

}
