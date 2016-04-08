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
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
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
  private final Config config;

  public JobManager() {
    this( true, 1000, 1500 );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                     final long dialogThresholdMilliseconds ) {
    if ( !isSupportAsync ) {
      logger.info( "JobManager initialization: async mode marked as disabled." );
    }
    this.config = new Config( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMilliseconds );
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

  @POST @Path( "{job_id}/content" ) public Response getContent( @PathParam( "job_id" ) String job_id )
    throws IOException {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Content: invalid UUID: " + job_id );
      // The 422 (Unprocessable Entity) status code
      return get404();
    }

    // get async bean:
    IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      return Response.serverError().build();
    }

    final IPentahoSession session = PentahoSessionHolder.getSession();

    Future<IFixedSizeStreamingContent> future = executor.getFuture( uuid, session );

    IAsyncReportState state = executor.getReportState( uuid, session );
    if ( state == null ) {
      return get404();
    }

    if( !AsyncExecutionStatus.FINISHED.equals( state.getStatus() ) ){
      return Response.status( Response.Status.ACCEPTED ).build();
    }

    IFixedSizeStreamingContent input = null;
    try {
      input = future.get();
    } catch ( Exception e ) {
      logger.error( "Error generating report", e );
      return Response.serverError().build();
    }
    // ok we have InputStream so future will not be used anymore.
    // release internal links to objects
    executor.cleanFuture( uuid, session );

    StreamingOutput stream = new StreamingOutputWrapper( input.getStream() );

    MediaType mediaType = null;
    Response.ResponseBuilder response = null;
    try {
      mediaType = MediaType.valueOf( state.getMimeType() );
      response = Response.ok( stream, mediaType );
    } catch ( Exception e ) {
      logger.error( "can't determine JAX-RS media type for: " + state.getMimeType() );
      response = Response.ok( stream, state.getMimeType() );
    }

    response = noCache( response );
    response = calculateContentDisposition( response, state );

    return response.build();
  }

  protected static Response.ResponseBuilder noCache( Response.ResponseBuilder response ) {
    // no cache
    CacheControl cacheControl = new CacheControl();
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

  @GET @Path( "{job_id}/status" ) @Produces( "application/json" )
  public Response getStatus( @PathParam( "job_id" ) String job_id ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();
    IAsyncReportState responseJson = executor.getReportState( uuid, session );
    if ( responseJson == null ) {
      return get404();
    }
    // ...someday refactor it to convenient jax-rs way.
    ObjectMapper mapper = new ObjectMapper();
    String json = null;
    try {
      json = mapper.writeValueAsString( responseJson );
    } catch ( Exception e ) {
      logger.error( "unable to deserialize to json : " + responseJson.toString() );
      Response.serverError().build();
    }
    return Response.ok( json ).build();
  }

  @GET @Path( "{job_id}/cancel" ) public Response cancel( @PathParam( "job_id" ) String job_id ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();

    Future<InputStream> future = executor.getFuture( uuid, session );
    IAsyncReportState state = executor.getReportState( uuid, session );
    if ( state == null ) {
      return get404();
    }

    logger.debug( "Cancellation of report: " + state.getPath() + ", requested by : " + session.getName() );
    future.cancel( true );

    return Response.ok().build();
  }


  @GET @Path( "{job_id}/requestPage/{page}" ) @Produces( "text/text" )
  public Response requestPage( @PathParam( "job_id" ) final String job_id, @PathParam( "page" ) final int page ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( final Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    final IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();
    executor.requestPage( uuid, session, page );

    return Response.ok( String.valueOf( page ) ).build();
  }

  @GET @Path( "{job_id}/clean" ) public Response clean( @PathParam( "job_id" ) final String job_id ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( final Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    final IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();

    executor.cleanFuture( uuid, session );

    return Response.ok().build();
  }

  protected final Response get404() {
    return Response.status( Response.Status.NOT_FOUND ).build();
  }

  protected IPentahoAsyncExecutor getExecutor() {
    return PentahoSystem.get( IPentahoAsyncExecutor.class, PentahoAsyncExecutor.BEAN_NAME, null );
  }

  /**
   * In-place implementation to support streaming responses. By default - even InputStream passed - streaming is not
   * occurs.
   */
  public static final class StreamingOutputWrapper implements StreamingOutput {

    private InputStream input;
    public static final byte[] BUFFER = new byte[ 8192 ];

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
