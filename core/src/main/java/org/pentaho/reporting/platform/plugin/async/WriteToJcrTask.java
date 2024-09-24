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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.Callable;

public class WriteToJcrTask implements Callable<Serializable> {
  private static final String CANT_CREATE_FILE_IN_JCR = "Can't create file in JCR";
  private static Log log = LogFactory.getLog( WriteToJcrTask.class );


  private static final String FORMAT = "%s(%d)%s";
  private static final String TXT = ".txt";
  private static final String DEFAULT_NAME = "content";
  private static final String CANT_PERSIST_MSG = "Cant't persist report: ";
  private final IAsyncReportExecution<? extends IAsyncReportState> parentTask;
  private final InputStream inputStream;


  public WriteToJcrTask(
    final IAsyncReportExecution<? extends IAsyncReportState> parentTask,
    final InputStream inputStream ) {

    this.parentTask = parentTask;
    this.inputStream = inputStream;
  }


  @Override public Serializable call() throws Exception {

    try {

      final IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );


      final org.pentaho.reporting.libraries.base.util.IOUtils utils = org.pentaho.reporting.libraries
        .base.util.IOUtils.getInstance();


      final ISchedulingDirectoryStrategy directoryStrategy = PentahoSystem.get( ISchedulingDirectoryStrategy.class );

      final RepositoryFile outputFolder = directoryStrategy.getSchedulingDir( repo );

      final ReportContentRepository repository = getReportContentRepository( outputFolder );
      final ContentLocation dataLocation = repository.getRoot();


      final IAsyncReportState state = parentTask.getState();

      final String extension = MimeHelper.getExtension( state.getMimeType() );
      final String targetExt = extension != null ? extension : TXT;
      final String fullPath = state.getPath();
      String cleanFileName = utils.stripFileExtension( utils.getFileName( fullPath ) );
      if ( StringUtil.isEmpty( cleanFileName ) ) {
        cleanFileName = DEFAULT_NAME;
      }

      String targetName = cleanFileName + targetExt;

      int copy = 1;

      final OutputStream outputStream;

      synchronized ( FORMAT ) {
        while ( dataLocation.exists( targetName ) ) {
          targetName = String.format( FORMAT, cleanFileName, copy, targetExt );
          copy++;
        }
        outputStream = dataLocation.createItem( targetName ).getOutputStream();
      }

      if ( outputStream != null ) {
        try {
          IOUtils.copy( inputStream, outputStream );
          outputStream.flush();
          final RepositoryFile targetFile = repo.getFile( outputFolder.getPath() + "/" + targetName );
          return targetFile.getId();
        } finally {
          IOUtils.closeQuietly( outputStream );
        }
      } else {
        throw new IOException( CANT_CREATE_FILE_IN_JCR );
      }

    } catch ( final Exception e ) {
      log.error( CANT_PERSIST_MSG, e );
    } finally {
      IOUtils.closeQuietly( inputStream );
    }

    return null;
  }

  protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
    return new ReportContentRepository( outputFolder );
  }
}
