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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Future;

@Path( "/reporting/api/jobs" )
public class JobManager extends AbstractJobManager {

  private static final Log logger = LogFactory.getLog( JobManager.class );

  public JobManager() {
    this( true, 1000, 1500 );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                     final long dialogThresholdMilliseconds ) {
    super( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMilliseconds );
  }

  @POST @Path( "{job_id}/content" ) public Response getContent( @PathParam( "job_id" ) final String job_id )
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
    final IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      return Response.serverError().build();
    }

    final IPentahoSession session = getSession();

    final Future<IFixedSizeStreamingContent> future = executor.getFuture( uuid, session );
    if ( future == null || !future.isDone() ) {
      logger.warn( "Attempt to get content while execution is not done. Called by: " + session.getName() );
      return get404();
    }

    final IAsyncReportState state = executor.getReportState( uuid, session );
    if ( state == null ) {
      return get404();
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

    final StreamingOutput stream = new StreamingOutputWrapper( input.getStream() );

    MediaType mediaType = null;
    Response.ResponseBuilder response = null;
    try {
      mediaType = MediaType.valueOf( state.getMimeType() );
      response = Response.ok( stream, mediaType );
    } catch ( Exception e ) {
      logger.error( "can't determine JAX-RS media type for: " + state.getMimeType() );
      response = Response.ok( stream, state.getMimeType() );
    }

    response = AbstractJobManager.noCache( response );
    response = AbstractJobManager.calculateContentDisposition( response, state );
    response.header( "Content-Length", input.getContentSize() );

    return response.build();
  }

  @GET @Path( "{job_id}/status" ) @Produces( "application/json" )
  public Response getStatus( @PathParam( "job_id" ) final String job_id ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    final IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final IAsyncReportState responseJson = executor.getReportState( uuid, session );
    if ( responseJson == null ) {
      return get404();
    }
    // ...someday refactor it to convenient jax-rs way.
    String json = null;
    try {
      json = writer.writeValueAsString( responseJson );
    } catch ( Exception e ) {
      logger.error( "unable to deserialize to json : " + responseJson.toString() );
      Response.serverError().build();
    }
    return Response.ok( json ).build();
  }

  @GET @Path( "{job_id}/cancel" ) public Response cancel( @PathParam( "job_id" ) final String job_id ) {
    UUID uuid = null;
    try {
      uuid = UUID.fromString( job_id );
    } catch ( Exception e ) {
      logger.error( "Status: invalid UUID: " + job_id );
      return get404();
    }

    final IPentahoAsyncExecutor executor = getExecutor();
    if ( executor == null ) {
      // where is my bean?
      return Response.serverError().build();
    }
    final IPentahoSession session = PentahoSessionHolder.getSession();

    final Future<IFixedSizeStreamingContent> future = executor.getFuture( uuid, session );
    final IAsyncReportState state = executor.getReportState( uuid, session );
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
    final IPentahoSession session = getSession();
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

  private IPentahoAsyncExecutor getExecutor() {
    return PentahoSystem.get( IPentahoAsyncExecutor.class, PentahoAsyncExecutor.BEAN_NAME, null );
  }
}
