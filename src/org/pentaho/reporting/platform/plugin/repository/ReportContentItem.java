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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.repository2.unified.fileio.RepositoryFileOutputStream;
import org.pentaho.platform.util.RepositoryPathEncoder;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.LibRepositoryBoot;
import org.pentaho.reporting.libraries.repository.Repository;

/**
 * Creation-Date: 05.07.2007, 14:54:08
 * 
 * @author Thomas Morgner
 */
public class ReportContentItem implements ContentItem {

  private ReportContentLocation parent;
  private RepositoryFile file;
  private String mimeType;

  public ReportContentItem( final RepositoryFile file, final ReportContentLocation parent, final String mimeType ) {
    this.file = file;
    this.parent = parent;
    this.mimeType = mimeType;
  }

  public void setMimeType( final String mimeType ) {
    this.mimeType = mimeType;
  }

  public String getMimeType() throws ContentIOException {
    return mimeType;
  }

  public OutputStream getOutputStream() throws ContentIOException, IOException {
    return new RepositoryFileOutputStream( file );
  }

  public InputStream getInputStream() throws ContentIOException, IOException {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    SimpleRepositoryFileData data = repo.getDataForRead( file.getId(), SimpleRepositoryFileData.class );
    return data.getInputStream();
  }

  public boolean isReadable() {
    return false;
  }

  public boolean isWriteable() {
    return true;
  }

  public String getName() {
    return file.getName();
  }

  public Object getAttribute( final String domain, final String key ) {
    if ( LibRepositoryBoot.REPOSITORY_DOMAIN.equals( domain ) ) {
      if ( LibRepositoryBoot.SIZE_ATTRIBUTE.equals( key ) ) {
        return new Long( file.getFileSize() );
      } else if ( LibRepositoryBoot.VERSION_ATTRIBUTE.equals( key ) ) {
        return file.getLastModifiedDate();
      }
    }
    return null;
  }

  public boolean setAttribute( final String domain, final String key, final Object object ) {
    return false;
  }

  public ContentLocation getParent() {
    return parent;
  }

  public Repository getRepository() {
    return parent.getRepository();
  }

  public boolean delete() {
    IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
    repo.deleteFile( file.getId(), "PRE:DELETE" );
    return true;
  }

  public Object getContentId() {
    return RepositoryPathEncoder.encode( file.getPath() );
  }
}
