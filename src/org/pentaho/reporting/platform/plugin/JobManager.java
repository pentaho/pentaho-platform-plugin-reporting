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
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.RepositoryPathEncoder;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.Future;

@Path( "/reporting/api/jobs" )
public class JobManager {

  private static final Log logger = LogFactory.getLog( JobManager.class );
  private static final String INVALID_UUID = "Invalid UUID: ";
  private static final String ASYNC_DISABLED = "JobManager initialization: async mode marked as disabled.";
  private static final String ERROR_GENERATING_REPORT = "Error generating report";
  private static final String UNABLE_TO_SERIALIZE_TO_JSON = "Unable to serialize to json : ";
  private static final String UNCKNOWN_MEDIA_TYPE = "Can't determine JAX-RS media type for: ";
  private final Config config;

  public JobManager() {
    this( true, 500, 1500 );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                     final long dialogThresholdMillisecond ) {
    if ( !isSupportAsync ) {
      logger.info( ASYNC_DISABLED );
    }
    this.config = new Config( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMillisecond );
  }

  @GET @Path( "config" ) public Response getConfig() {
    final ObjectMapper mapper = new ObjectMapper();
    try {
      return Response
        .ok( mapper.writeValueAsString( config ),
          MediaType.APPLICATION_JSON ).build();
    } catch ( final IOException e ) {
      e.printStackTrace();
    }
    return null;
  }


  @GET @Path( "{job_id}/content" )
  public Response getPDFContent( @PathParam( "job_id" ) final String job_id ) throws IOException {
    logger.debug( "Chrome pdf viewer workaround. See BACKLOG-7598 for details" );

    return this.getContent( job_id );
  }

  @SuppressWarnings( "unchecked" )
  @POST @Path( "{job_id}/content" ) public Response getContent( @PathParam( "job_id" ) final String jobId )
    throws IOException {

    final IPentahoAsyncExecutor executor = getExecutor();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final UUID uuid;
    try {
      uuid = UUID.fromString( jobId );
    } catch ( final Exception e ) {
      logger.error( INVALID_UUID + jobId );
      return get404();
    }


    final Future<IFixedSizeStreamingContent> future = executor.getFuture( uuid, session );

    final IAsyncReportState state = executor.getReportState( uuid, session );
    if ( state == null ) {
      return get404();
    }

    if ( !AsyncExecutionStatus.FINISHED.equals( state.getStatus() ) ) {
      return Response.status( Response.Status.ACCEPTED ).build();
    }

    final IFixedSizeStreamingContent input;
    try {
      input = future.get();
    } catch ( final Exception e ) {
      logger.error( ERROR_GENERATING_REPORT, e );
      return Response.serverError().build();
    }

    final StreamingOutput stream = new StreamingOutputWrapper( input.getStream() );

    final MediaType mediaType;
    Response.ResponseBuilder response;
    try {
      mediaType = MediaType.valueOf( state.getMimeType() );
      response = Response.ok( stream, mediaType );
    } catch ( final Exception e ) {
      logger.error( UNCKNOWN_MEDIA_TYPE + state.getMimeType() );
      response = Response.ok( stream, state.getMimeType() );
    }

    response = noCache( response );
    response = calculateContentDisposition( response, state );

    return response.build();
  }


  @GET @Path( "{job_id}/status" ) @Produces( "application/json" )
  public Response getStatus( @PathParam( "job_id" ) final String jobId ) {
    final IPentahoAsyncExecutor executor = getExecutor();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final UUID uuid;
    try {
      uuid = UUID.fromString( jobId );
    } catch ( final Exception e ) {
      logger.error( INVALID_UUID + jobId );
      return get404();
    }

    final IAsyncReportState responseJson = executor.getReportState( uuid, session );
    if ( responseJson == null ) {
      return get404();
    }

    final ObjectMapper mapper = new ObjectMapper();
    String json = null;
    try {
      json = mapper.writeValueAsString( responseJson );
    } catch ( final Exception e ) {
      logger.error( UNABLE_TO_SERIALIZE_TO_JSON + responseJson.toString() );
      Response.serverError().build();
    }
    return Response.ok( json ).build();
  }

  protected IPentahoAsyncExecutor getExecutor() {
    return PentahoSystem.get( IPentahoAsyncExecutor.class );
  }

  @SuppressWarnings( "unchecked" )
  @GET @Path( "{job_id}/cancel" ) public Response cancel( @PathParam( "job_id" ) final String jobId ) {
    final IPentahoAsyncExecutor executor = getExecutor();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final UUID uuid;
    try {
      uuid = UUID.fromString( jobId );
    } catch ( final Exception e ) {
      logger.error( INVALID_UUID + jobId );
      return get404();
    }

    final Future<InputStream> future = executor.getFuture( uuid, session );
    final IAsyncReportState state = executor.getReportState( uuid, session );

    if ( state == null ) {
      return get404();
    }

    logger.debug( "Cancellation of report: " + state.getPath() + ", requested by : " + session.getName() );

    future.cancel( true );

    return Response.ok().build();
  }


  @GET @Path( "{job_id}/requestPage/{page}" ) @Produces( "text/text" )
  public Response requestPage( @PathParam( "job_id" ) final String jobId, @PathParam( "page" ) final int page ) {
    final IPentahoAsyncExecutor executor = getExecutor();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final UUID uuid;
    try {
      uuid = UUID.fromString( jobId );
    } catch ( final Exception e ) {
      logger.error( INVALID_UUID + jobId );
      return get404();
    }

    executor.requestPage( uuid, session, page );

    return Response.ok( String.valueOf( page ) ).build();
  }


  @GET @Path( "{job_id}/schedule" ) @Produces( "text/text" )
  public Response schedule( @PathParam( "job_id" ) final String jobId ) {
    final IPentahoAsyncExecutor executor = getExecutor();
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final UUID uuid;
    try {
      uuid = UUID.fromString( jobId );
    } catch ( final Exception e ) {
      logger.error( INVALID_UUID + jobId );
      return get404();
    }

    executor.schedule( uuid, session );
    return Response.ok().build();
  }

  protected final Response get404() {
    return Response.status( Response.Status.NOT_FOUND ).build();
  }


  /**
   * In-place implementation to support streaming responses. By default - even InputStream passed - streaming is not
   * occurs.
   */
  protected static final class StreamingOutputWrapper implements StreamingOutput {

    private InputStream input;

    public StreamingOutputWrapper( final InputStream readFrom ) {
      this.input = readFrom;
    }

    @Override public void write( final OutputStream outputStream ) throws IOException, WebApplicationException {
      try {
        IOUtils.copy( input, outputStream );
        outputStream.flush();
      } finally {
        IOUtils.closeQuietly( outputStream );
        IOUtils.closeQuietly( input );
      }
    }
  }

  protected static Response.ResponseBuilder noCache( final Response.ResponseBuilder response ) {
    // no cache
    final CacheControl cacheControl = new CacheControl();
    cacheControl.setPrivate( true );
    cacheControl.setMaxAge( 0 );
    cacheControl.setMustRevalidate( true );

    response.cacheControl( cacheControl );
    return response;
  }

  protected static Response.ResponseBuilder calculateContentDisposition( final Response.ResponseBuilder response,
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

  private class Config {
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
