/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Hitachi Vantara.  All rights reserved.
 */

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
