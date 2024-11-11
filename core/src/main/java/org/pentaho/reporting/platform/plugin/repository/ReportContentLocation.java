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


package org.pentaho.reporting.platform.plugin.repository;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.repository2.unified.fileio.RepositoryFileOutputStream;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.libraries.base.util.IOUtils;
import org.pentaho.reporting.libraries.repository.ContentCreationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.LibRepositoryBoot;
import org.pentaho.reporting.libraries.repository.Repository;

/**
 * Creation-Date: 05.07.2007, 14:45:06
 *
 * @author Thomas Morgner
 */
public class ReportContentLocation implements ContentLocation {
  private RepositoryFile location;

  private ReportContentRepository repository;
  private String[] hiddenExtensions = { ".jpe", ".jpeg", ".jpg", ".png", ".css" };
  public static final String RESERVEDMAPKEY_LINEAGE_ID = "lineage-id";

  public ReportContentLocation( final RepositoryFile location, final ReportContentRepository repository ) {
    if ( location == null ) {
      throw new NullPointerException( "Content-Location cannot be null" );
    }
    if ( repository == null ) {
      throw new NullPointerException();
    }
    this.location = location;
    this.repository = repository;
  }

  public ContentEntity[] listContents() throws ContentIOException {
    final ArrayList<ReportContentItem> itemCollection = new ArrayList<ReportContentItem>();

    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );

    final Iterator<RepositoryFile> iterator = repo.getChildren( location.getId() ).iterator();
    while ( iterator.hasNext() ) {
      RepositoryFile child = iterator.next();
      itemCollection.add( new ReportContentItem( child, this, MimeHelper.getMimeTypeFromFileName( child.getName() ) ) );
    }
    return itemCollection.toArray( new ContentEntity[ itemCollection.size() ] );
  }

  public ContentEntity getEntry( final String name ) throws ContentIOException {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    String path = this.location.getPath() + "/" + name;
    final RepositoryFile rawFile = repo.getFile( path );
    if ( rawFile == null ) {
      throw new ContentIOException( "Could not get ContentItem entry" ); //$NON-NLS-1$
    }
    return new ReportContentItem( rawFile, this, MimeHelper.getMimeTypeFromFileName( name ) );
  }

  public ContentItem createItem( final String name ) throws ContentCreationException {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    final String extension = IOUtils.getInstance().getFileExtension( name );
    final String mimeType = MimeHelper.getMimeTypeFromExtension( extension );
    RepositoryFileOutputStream rfos = null;
    String path = this.location.getPath() + "/" + name;
    if ( repo.getFile( path ) == null ) {
      if ( isHiddenExtension( extension ) ) {
        rfos = new RepositoryFileOutputStream( path, true );
      } else {
        rfos = new RepositoryFileOutputStream( path, false );
      }

      try {
        rfos.close();
      } catch ( IOException e ) {
        throw new ContentCreationException( e.getMessage(), e );
      }
    }
    // We need to add RESERVEDMAPKEY_LINEAGE_ID to the generated file's meta, in order to find and delete it in
    // org.pentaho.platform.admin.GeneratedContentCleaner.findGeneratedContent()
    // see http://jira.pentaho.com/browse/BISERVER-12930 for more details.
    Map<String, Serializable> meta = repo.getFileMetadata( repo.getFile( path ).getId() );
    meta.put( RESERVEDMAPKEY_LINEAGE_ID, UUID.randomUUID().toString() );
    repo.setFileMetadata( repo.getFile( path ).getId(), meta );
    return new ReportContentItem( repo.getFile( path ), this, mimeType );
  }

  public ContentLocation createLocation( final String string ) throws ContentCreationException {
    throw new ContentCreationException( "Cannot create a content-location: " + string ); //$NON-NLS-1$
  }

  public boolean exists( final String name ) {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    String path = this.location.getPath() + "/" + name;
    return repo.getFile( path ) != null;
  }

  public String getName() {
    return this.location.getName();
  }

  public Object getContentId() {
    return this.location.getId();
  }

  public Object getAttribute( final String domain, final String key ) {
    if ( LibRepositoryBoot.REPOSITORY_DOMAIN.equals( domain ) ) {
      if ( LibRepositoryBoot.VERSION_ATTRIBUTE.equals( key ) ) {
        return location.getVersionId();
      }
    }
    return null;
  }

  public boolean setAttribute( final String domain, final String key, final Object object ) {
    return false;
  }

  public ContentLocation getParent() {
    // We have no parent ...
    return null;
  }

  public Repository getRepository() {
    return repository;
  }

  public boolean delete() {
    // cannot be deleted ..
    return false;
  }

  public boolean isHiddenExtension( String extension ) {
    for ( String ext : hiddenExtensions ) {
      if ( ext.equals( extension ) ) {
        return true;
      }
    }
    return false;
  }
}
