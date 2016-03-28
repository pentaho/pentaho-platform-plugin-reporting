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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.util.RepositoryPathEncoder;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by dima.prokopenko@gmail.com on 3/30/2016.
 */
public abstract class AbstractJobManager {

  private static final Log logger = LogFactory.getLog( AbstractJobManager.class );

  private Config config;

  protected ObjectWriter writer;

  protected AbstractJobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                                final long dialogThresholdMilliseconds ) {
    if ( !isSupportAsync ) {
      logger.info( "JobManager initialization: async mode marked as disabled." );
    }
    this.config = new Config( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMilliseconds );

    ObjectMapper mapper = new ObjectMapper();
    this.writer = mapper.writer();
  }

  @GET @Path( "config" ) public Response getConfig() {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return Response
          .ok( mapper.writeValueAsString( config ),
              MediaType.APPLICATION_JSON ).build();
    } catch ( final IOException e ) {
      logger.error( "Unable to parse config for job manager: ", e );
      return Response.serverError().build();
    }
  }

  public static Response.ResponseBuilder calculateContentDisposition( final Response.ResponseBuilder response,
      final IAsyncReportState state ) {
    final org.pentaho.reporting.libraries.base.util.IOUtils utils = org.pentaho.reporting.libraries
        .base.util.IOUtils.getInstance();

    final String targetExt = MimeHelper.getExtension( state.getMimeType() );
    final String fullPath = state.getPath();
    final String sourceExt = utils.getFileExtension( fullPath );
    String cleanFileName = utils.stripFileExtension( utils.getFileName( fullPath ) );
    if ( cleanFileName == null || cleanFileName.isEmpty() ) {
      cleanFileName = "content";
    }

    final String
        disposition =
        "inline; filename*=UTF-8''" + RepositoryPathEncoder
            .encode( RepositoryPathEncoder.encodeRepositoryPath( cleanFileName + targetExt ) );
    response.header( "Content-Disposition", disposition );

    response.header( "Content-Description", cleanFileName + sourceExt );

    return response;
  }

  public static Response.ResponseBuilder noCache( final Response.ResponseBuilder response ) {
    CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate( true );
    cacheControl.setMaxAge( 0 );
    cacheControl.setMustRevalidate( true );

    response.cacheControl( cacheControl );

    return response;
  }

  protected Response get404() {
    return Response.status( Response.Status.NOT_FOUND ).build();
  }

  protected IPentahoSession getSession() {
    return PentahoSessionHolder.getSession();
  }

  /**
   * In-place implementation to support streaming responses. By default - even InputStream passed - streaming is not
   * occurs.
   */
  public static final class StreamingOutputWrapper implements StreamingOutput {

    private InputStream input;

    public StreamingOutputWrapper( InputStream readFrom ) {
      this.input = readFrom;
    }

    @Override public void write( OutputStream outputStream ) throws IOException, WebApplicationException {
      try {
        IOUtils.copy( input, outputStream );
        outputStream.flush();
      } finally {
        IOUtils.closeQuietly( outputStream );
        IOUtils.closeQuietly( input );
      }
    }
  }

  public static class Config {
    private final boolean isSupportAsync;
    private final long pollingIntervalMilliseconds;
    private final long dialogThresholdMilliseconds;

    private Config( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
        final long dialogThresholdMilliseconds ) {
      this.isSupportAsync = isSupportAsync;
      this.pollingIntervalMilliseconds = pollingIntervalMilliseconds;
      this.dialogThresholdMilliseconds = dialogThresholdMilliseconds;
    }

    public boolean isSupportAsync() {
      return isSupportAsync;
    }

    public long getPollingIntervalMilliseconds() {
      return pollingIntervalMilliseconds;
    }

    public long getDialogThresholdMilliseconds() {
      return dialogThresholdMilliseconds;
    }
  }

}
