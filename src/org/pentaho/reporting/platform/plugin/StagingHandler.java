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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.UUIDUtil;
import org.pentaho.reporting.engine.classic.core.util.StagingMode;
import org.pentaho.reporting.libraries.base.util.MemoryByteArrayOutputStream;

public class StagingHandler {
  private static final Log logger = LogFactory.getLog( StagingHandler.class );

  private OutputStream destination;
  private TrackingOutputStream stagingStream;
  private File tmpFile;
  private StagingMode mode;
  private IPentahoSession userSession;

  public StagingHandler( final OutputStream outputStream, final StagingMode stagingMode,
      final IPentahoSession userSession ) throws IOException {
    if ( outputStream == null ) {
      throw new NullPointerException();
    }
    if ( stagingMode == null ) {
      throw new NullPointerException();
    }

    this.userSession = userSession;
    this.destination = outputStream;
    initialize( stagingMode );
  }

  public StagingMode getStagingMode() {
    return this.mode;
  }

  public boolean isFullyBuffered() {
    return mode != StagingMode.THRU;
  }

  public boolean canSendHeaders() {
    if ( ( mode == StagingMode.THRU ) && ( getWrittenByteCount() > 0 ) ) {
      return false;
    } else {
      return true;
    }
  }

  private void initialize( final StagingMode mode ) throws IOException {
    this.mode = mode;
    logger.trace( "Staging mode set - " + mode ); //$NON-NLS-1$
    if ( mode == StagingMode.MEMORY ) {
      createTrackingProxy( new MemoryByteArrayOutputStream() );
    } else if ( mode == StagingMode.TMPFILE ) {
      final IApplicationContext appCtx = PentahoSystem.getApplicationContext();
      // Use the deleter framework for safety...
      if ( userSession.getId().length() >= 10 ) {
        tmpFile = appCtx.createTempFile( userSession, "repstg", ".tmp", true ); //$NON-NLS-1$ //$NON-NLS-2$
      } else {
        // Workaround bug in appContext.createTempFile ... :-(
        final File parentDir = new File( appCtx.getSolutionPath( "system/tmp" ) ); //$NON-NLS-1$
        final ITempFileDeleter fileDeleter =
            (ITempFileDeleter) userSession.getAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE );
        final String newPrefix =
            new StringBuilder()
                .append( "repstg" ).append( UUIDUtil.getUUIDAsString().substring( 0, 10 ) ).append( '-' ).toString(); //$NON-NLS-1$
        tmpFile = File.createTempFile( newPrefix, ".tmp", parentDir ); //$NON-NLS-1$
        if ( fileDeleter != null ) {
          fileDeleter.trackTempFile( tmpFile );
        } else {
          // There is no deleter, so cleanup on VM exit. (old behavior)
          tmpFile.deleteOnExit();
        }
      }

      createTrackingProxy( new BufferedOutputStream( new FileOutputStream( tmpFile ) ) );
    } else {
      createTrackingProxy( destination );
    }
  }

  public OutputStream getStagingOutputStream() {
    return this.stagingStream;
  }

  private void createTrackingProxy( final OutputStream streamToTrack ) {
    this.stagingStream = new TrackingOutputStream( streamToTrack );
  }

  public void complete() throws IOException {
    if ( mode == StagingMode.MEMORY ) {
      final MemoryByteArrayOutputStream stream = (MemoryByteArrayOutputStream) stagingStream.getWrappedStream();
      final byte[] bytes = stream.getRaw();
      destination.write( bytes, 0, stream.getLength() );
      destination.flush();
    } else if ( mode == StagingMode.TMPFILE ) {
      // Close the stream so we can use the file as input.
      IOUtils.closeQuietly( stagingStream );
      stagingStream = null;
      final BufferedInputStream bis = new BufferedInputStream( new FileInputStream( tmpFile ) );
      try {
        IOUtils.copy( bis, destination );
      } finally {
        IOUtils.closeQuietly( bis );
      }
    }
    // Nothing to do for THRU - the output already has it's stuff

    close();

  }

  public void close() {
    if ( ( this.stagingStream != null ) && ( mode == StagingMode.TMPFILE ) ) {
      IOUtils.closeQuietly( stagingStream );
      stagingStream = null;
    }
    if ( tmpFile != null ) {
      if ( tmpFile.exists() ) {
        try {
          tmpFile.delete();
        } catch ( Exception ignored ) {
          // I can't delete it, perhaps the deleter can delete it.
          CommonUtil.checkStyleIgnore();
        }
      }
      tmpFile = null;
    }
  }

  public int getWrittenByteCount() {
    assert stagingStream != null;
    return stagingStream.getTrackingSize();
  }

}
