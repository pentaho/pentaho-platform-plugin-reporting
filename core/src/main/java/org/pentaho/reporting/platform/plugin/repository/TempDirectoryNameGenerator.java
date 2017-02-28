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

package org.pentaho.reporting.platform.plugin.repository;

import java.io.File;
import java.io.IOException;

import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.MimeRegistry;
import org.pentaho.reporting.libraries.repository.file.FileContentLocation;

public class TempDirectoryNameGenerator implements PentahoNameGenerator {
  private File targetDirectory;
  private MimeRegistry mimeRegistry;
  private DefaultNameGenerator fallback;
  private boolean safeToDelete;

  public TempDirectoryNameGenerator() {
  }

  public void initialize( final ContentLocation contentLocation, final boolean safeToDelete ) {
    this.safeToDelete = safeToDelete;
    this.mimeRegistry = contentLocation.getRepository().getMimeRegistry();
    if ( contentLocation instanceof FileContentLocation ) {
      final FileContentLocation fileContentLocation = (FileContentLocation) contentLocation;
      targetDirectory = (File) fileContentLocation.getContentId();
      if ( targetDirectory.isDirectory() == false ) {
        throw new NullPointerException();
      }
    }
    if ( targetDirectory == null ) {
      fallback = new DefaultNameGenerator( contentLocation );
    }
  }

  /**
   * Generates a new name for the location. The name-generator may use both the name-hint and mimetype to compute the
   * new name.
   * 
   * @param nameHint
   *          the name hint, usually a identifier for the new filename (can be null).
   * @param mimeType
   *          the mime type of the new filename. Usually used to compute a suitable file-suffix.
   * @return the generated name, never null.
   * @throws org.pentaho.reporting.libraries.repository.ContentIOException
   *           if the name could not be generated for any reason.
   */
  public String generateName( final String nameHint, final String mimeType ) throws ContentIOException {
    if ( fallback != null ) {
      return fallback.generateName( nameHint, mimeType );
    }
    final String suffix = mimeRegistry.getSuffix( mimeType );
    try {
      final File tempFile = File.createTempFile( nameHint, "." + suffix, targetDirectory );
      if ( safeToDelete ) {
        final ITempFileDeleter deleter = PentahoSystem.get( ITempFileDeleter.class );
        if ( deleter != null ) {
          deleter.trackTempFile( tempFile );
        }
      }
      return tempFile.getName();
    } catch ( IOException e ) {
      throw new ContentIOException( "Unable to generate a name for the data file", e );
    }
  }
}
