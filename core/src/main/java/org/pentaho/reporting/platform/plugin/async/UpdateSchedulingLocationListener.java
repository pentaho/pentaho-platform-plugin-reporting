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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;

import java.io.Serializable;

/**
 * Moves report to the location provided by user
 */
class UpdateSchedulingLocationListener implements ISchedulingListener {

  private static final String TARGET_LOCATION = "targetLocation";
  private static final String MOVE_MSG = "Moved to the location selected by user";
  private static final String ERROR_MSG = "Can't move report to selected location: ";
  private static final String NEW_NAME = "newName";
  private static Log log = LogFactory.getLog( UpdateSchedulingLocationListener.class );
  private static final String FORMAT = "%s(%d)%s";


  private final Serializable targetfolderId;
  private final String newName;

  UpdateSchedulingLocationListener( final Serializable targetfolderId, final String newName ) {
    ArgumentNullException.validate( NEW_NAME, newName );
    ArgumentNullException.validate( TARGET_LOCATION, targetfolderId );
    this.newName = newName;
    this.targetfolderId = targetfolderId;
  }

  @Override
  public void onSchedulingCompleted( final Serializable fileId ) {


    try {
      final IUnifiedRepository repo = PentahoSystem.get( IUnifiedRepository.class );
      final RepositoryFile savedFile = repo.getFileById( fileId );
      final RepositoryFile outputFolder = repo.getFileById( targetfolderId );

      final org.pentaho.reporting.libraries.base.util.IOUtils utils = org.pentaho.reporting.libraries
        .base.util.IOUtils.getInstance();


      if ( savedFile != null && !savedFile.isFolder() && outputFolder != null && outputFolder.isFolder() && !StringUtil
        .isEmpty( newName ) ) {

        //InteliJ inspection states that we don't need StringBuilder here
        final String fileExtension = utils.getFileExtension( savedFile.getName() );
        final String folderPath = outputFolder.getPath() + "/";
        String newPath = folderPath + newName + fileExtension;

        int count = 1;

        synchronized ( FORMAT ) {
          /* Let's move file to temp location to handle situation
          when the name is not changed */
          String uuidAsString = getUuidAsString();
          while ( null != repo.getFile( newPath + uuidAsString ) ) {
            uuidAsString = getUuidAsString();
          }

          repo.moveFile( savedFile.getId(), newPath + uuidAsString, MOVE_MSG );

          while ( null != repo.getFile( newPath ) ) {
            newPath = String
              .format( FORMAT, folderPath + newName, count, fileExtension );
            count++;
          }
          repo.moveFile( savedFile.getId(), newPath, MOVE_MSG );
        }


      }
    } catch ( final Exception e ) {
      log.error( ERROR_MSG, e );
    }


  }

  protected String getUuidAsString() {
    return UUIDUtil.getUUIDAsString();
  }
}

