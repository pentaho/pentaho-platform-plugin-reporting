/*
 * ! ******************************************************************************
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


package org.pentaho.reporting.platform.plugin;

import java.io.InputStream;

import org.apache.commons.vfs2.FileObject;
import org.pentaho.reporting.libraries.resourceloader.FactoryParameterKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceData;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceLoadingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.resourceloader.loader.AbstractResourceData;


/**
 * This class is implemented to support loading solution files from the VFS (Virtual File System) into Pentaho Reporting
 * using KettleVFS with DefaultBowl
 * 
 * @author Pentaho
 */
public class VfsResourceData extends AbstractResourceData {

  public static final String FILE_OBJECT_KEY = "fileObject"; //$NON-NLS-1$

  private String filename;
  private ResourceKey key;
  private transient FileObject fileObject;

  /**
   * constructor which takes a resource key for data loading specifics
   * 
   * @param key
   *            resource key
   */
  public VfsResourceData( final ResourceKey key ) {
    if ( key == null ) {
      throw new NullPointerException();
    }

    this.key = key;
    this.filename = (String) key.getIdentifier();
    this.fileObject = (FileObject) key.getFactoryParameters().get( new FactoryParameterKey( FILE_OBJECT_KEY ) );
  }

  /**
   * gets a resource stream from the VFS using KettleVFS.
   * 
   * @param caller
   *               resource manager
   * @return input stream
   */
  public InputStream getResourceAsStream( ResourceManager caller ) throws ResourceLoadingException {
    try {
      if ( fileObject == null || !fileObject.exists() ) {
        throw new ResourceLoadingException( "VFS file not found: " + key.getIdentifierAsString() );
      }
      if ( !fileObject.isFile() ) {
        throw new ResourceLoadingException( "VFS path is not a file: " + key.getIdentifierAsString() );
      }
      if ( !fileObject.isReadable() ) {
        throw new ResourceLoadingException( "VFS file is not readable: " + key.getIdentifierAsString() );
      }
      return fileObject.getContent().getInputStream();
    } catch ( Exception ex ) {
      throw new ResourceLoadingException( "Failed to load VFS resource: " + ex.getLocalizedMessage(), ex );
    }
  }

  /**
   * returns a requested attribute, currently only supporting filename.
   *
   * @param lookupKey attribute requested
   * @return attribute value
   */
  public Object getAttribute( final String lookupKey ) {
    if ( lookupKey.equals( ResourceData.FILENAME ) ) {
      return filename;
    }
    if ( lookupKey.equals( ResourceData.CONTENT_LENGTH ) ) {
      try {
        if ( fileObject != null && fileObject.exists() ) {
          return fileObject.getContent().getSize();
        }
      } catch ( Exception ex ) {
        // Ignore and return null
      }
    }
    return null;
  }

  /**
   * return the version number
   * 
   * @param caller
   *               resource manager
   * 
   * @return version
   */
  public long getVersion( ResourceManager caller ) throws ResourceLoadingException {
    return -1; // no versions in vfs locations
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
