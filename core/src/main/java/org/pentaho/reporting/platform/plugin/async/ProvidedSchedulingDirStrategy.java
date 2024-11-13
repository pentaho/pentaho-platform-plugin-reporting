/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.util.StringUtil;

/**
 * Use absolute path or relative to home directory path as scheduling directory
 * Fallbacks to parent strategy
 */
public class ProvidedSchedulingDirStrategy extends HomeSchedulingDirStrategy {

  private static final String PREFIX = "/";
  private final String providedDir;

  /**
   * Provide absolute path or relative to home directory path to use as scheduling directory
   *
   * @param providedDir path
   */
  public ProvidedSchedulingDirStrategy( final String providedDir ) {
    this.providedDir = providedDir;
  }

  @Override public RepositoryFile getSchedulingDir( final IUnifiedRepository repo ) {

    if ( StringUtil.isEmpty( providedDir ) ) {
      return super.getSchedulingDir( repo );
    }

    //absolute
    if ( providedDir.startsWith( PREFIX ) ) {

      final RepositoryFile file = repo.getFile( providedDir );

      if ( file != null && file.isFolder() ) {
        return file;
      }

    } else {
      //relative
      final RepositoryFile parentDir = super.getSchedulingDir( repo );
      if ( parentDir != null ) {

        final RepositoryFile file = repo.getFile( parentDir.getPath() + PREFIX + providedDir );

        if ( file != null && file.isFolder() ) {
          return file;
        }

      }
    }

    return super.getSchedulingDir( repo );
  }
}
