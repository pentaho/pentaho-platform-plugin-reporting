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

package org.pentaho.reporting.platform.plugin;

import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.pentaho.platform.api.repository2.unified.Converter;
import org.pentaho.platform.api.repository2.unified.IRepositoryContentConverterHandler;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.UnifiedRepositoryException;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceLoadingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.resourceloader.loader.AbstractResourceData;


/**
 * This class is implemented to support loading solution files from the pentaho repository into JFreeReport
 * 
 * @author Will Gorman/Michael D'Amour
 */
public class RepositoryResourceData extends AbstractResourceData {

  public static final String PENTAHO_REPOSITORY_KEY = "pentahoRepositoryKey"; //$NON-NLS-1$

  private String filename;
  private ResourceKey key;

  /**
   * constructor which takes a resource key for data loading specifics
   * 
   * @param key
   *          resource key
   */
  public RepositoryResourceData( final ResourceKey key ) {
    if ( key == null ) {
      throw new NullPointerException();
    }

    this.key = key;
    this.filename = (String) key.getIdentifier();
  }

  /**
   * gets a resource stream from the runtime context.
   * 
   * @param caller
   *          resource manager
   * @return input stream
   */
  public InputStream getResourceAsStream( ResourceManager caller ) throws ResourceLoadingException {
    IUnifiedRepository unifiedRepository = null;
    try {
      unifiedRepository = PentahoSystem.get( IUnifiedRepository.class );
      RepositoryFile repositoryFile = unifiedRepository.getFile( key.getIdentifierAsString() );
      if ( repositoryFile == null ) {
        repositoryFile = unifiedRepository.getFileById( key.getIdentifierAsString() );
      }
      if ( repositoryFile == null ) {
        throw new ResourceLoadingException();
      }

      // BISERVER-11908 (KTR/KJB as a datasource)
      InputStream stream = convert( repositoryFile );
      if ( stream != null ) {
        return stream;
      }
      
      SimpleRepositoryFileData fileData =
          unifiedRepository.getDataForRead( repositoryFile.getId(), SimpleRepositoryFileData.class );
      return fileData.getStream();
    } catch ( UnifiedRepositoryException ex ) {
      // might be due to access denial
      throw new ResourceLoadingException( ex.getLocalizedMessage(), ex );
    }
  }

  private InputStream convert( RepositoryFile repositoryFile ) {
    
    IRepositoryContentConverterHandler converterHandler = PentahoSystem.get( IRepositoryContentConverterHandler.class);
    if ( converterHandler != null ) {
      String extension = FilenameUtils.getExtension( repositoryFile.getPath() );
      Converter converter = converterHandler.getConverter( extension );
      
      if ( converter != null ) {
        InputStream stream = converter.convert( repositoryFile.getId() );
        return stream;
      }
    }
    return null;
  }
  
  /**
   * returns a requested attribute, currently only supporting filename.
   * 
   * @param lookupKey
   *          attribute requested
   * @return attribute value
   */
  public Object getAttribute( final String lookupKey ) {
    if ( lookupKey.equals( ResourceData.FILENAME ) ) {
      return filename;
    }
    return null;
  }

  /**
   * return the version number
   * 
   * @param caller
   *          resource manager
   * 
   * @return version
   */
  public long getVersion( ResourceManager caller ) throws ResourceLoadingException {
    IUnifiedRepository unifiedRepository =
        PentahoSystem.get( IUnifiedRepository.class, PentahoSessionHolder.getSession() );
    RepositoryFile repositoryFile = null;
    try {
      // if we got a FileNotFoundException on getResourceInputStream then we will get a null file; avoid NPE
      repositoryFile = unifiedRepository.getFile( key.getIdentifier().toString() );
      if ( repositoryFile != null ) {
        return repositoryFile.getLastModifiedDate().getTime();
      } else {
        return -1;
      }
    } catch ( UnifiedRepositoryException ex ) {
      try {
        repositoryFile = unifiedRepository.getFileById( key.getIdentifier().toString() );
      } catch ( UnifiedRepositoryException exception ) {
        return -1;
      }
    }
    // if we got a FileNotFoundException on getResourceInputStream then we will get a null file; avoid NPE
    if ( repositoryFile != null ) {
      return repositoryFile.getLastModifiedDate().getTime();
    } else {
      return -1;
    }
  }

  /**
   * get the resource key
   * 
   * @return resource key
   */
  public ResourceKey getKey() {
    return key;
  }
}
