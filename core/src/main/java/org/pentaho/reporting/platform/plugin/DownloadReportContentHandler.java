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


package org.pentaho.reporting.platform.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.data.simple.SimpleRepositoryFileData;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.RepositoryPathEncoder;

public class DownloadReportContentHandler {
  private IPentahoSession userSession;
  private IParameterProvider pathProvider;

  public DownloadReportContentHandler( final IPentahoSession userSession, final IParameterProvider pathProvider ) {
    if ( userSession == null ) {
      throw new NullPointerException();
    }
    if ( pathProvider == null ) {
      throw new NullPointerException();
    }
    this.userSession = userSession;
    this.pathProvider = pathProvider;
  }

  public void createDownloadContent( final OutputStream outputStream, final String path ) throws IOException {
    final IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class, userSession );
    final RepositoryFile file = repository.getFile( idTopath( path ) );
    final HttpServletResponse response = (HttpServletResponse) pathProvider.getParameter( "httpresponse" ); //$NON-NLS-1$ //$NON-NLS-2$

    // if the user has PERM_CREATE, we'll allow them to pull it for now, this is as relaxed
    // as I am comfortable with but I can imagine a PERM_READ or PERM_EXECUTE being used
    // in the future
    if ( !file.isFolder() && !file.getPath().equals( "/" ) ) {
      SimpleRepositoryFileData fileData = repository.getDataForRead( file.getId(), SimpleRepositoryFileData.class );
      InputStream input = fileData.getStream();
      final byte[] data = input.toString().getBytes();
      if ( data == null ) {
        response.setStatus( HttpServletResponse.SC_BAD_REQUEST );
      } else {
        response.setHeader( "Content-Disposition", "attach; filename=\"" + file.getName() + "\"" ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        response.setHeader( "Content-Description", file.getName() ); //$NON-NLS-1$
        response.setDateHeader( "Last-Modified", file.getLastModifiedDate().getTime() ); //$NON-NLS-1$
        response.setContentLength( data.length );
        response.setHeader( "Cache-Control", "private, max-age=0, must-revalidate" ); //$NON-NLS-1$ //$NON-NLS-2$
        outputStream.write( data );
      }
    } else {
      response.setStatus( HttpServletResponse.SC_FORBIDDEN );
    }
  }

  private String idTopath( String id ) {
    String path = RepositoryPathEncoder.encode( id );
    if ( path != null && path.length() > 0 && path.charAt( 0 ) != '/' ) {
      path = "/" + path;
    }
    return path;
  }
}
