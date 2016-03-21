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
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
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
    this( true, 1000 );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds ) {
    if ( !isSupportAsync ) {
      logger.info( "JobManager initialization: async mode marked as disabled." );
    }
    this.config = new Config( isSupportAsync, pollingIntervalMilliseconds );
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

    Future<InputStream> future = executor.getFuture( uuid, session );
    if ( future == null || !future.isDone() ) {
      logger.warn( "Attempt to get content while execution is not done. Called by: " + session.getName() );
      return get404();
    }

    IAsyncReportState state = executor.getReportState( uuid, session );
    if ( state == null ) {
      return get404();
    }

    InputStream input = null;
    try {
      input = future.get();
    } catch ( Exception e ) {
      logger.error( "Error generating report", e );
      return Response.serverError().build();
    }
    // ok we have InputStream so future will not be used anymore.
    // release internal links to objects
    executor.cleanFuture( uuid, session );

    StreamingOutput stream = new StreamingOutputWrapper( input );

    MediaType mediaType = null;
    try {
      mediaType = MediaType.valueOf( state.getMimeType() );
    } catch ( Exception e ) {
      logger.error( "can't determine JAX-RS media type for: " + state.getMimeType() );
      // may be this will work?
      return Response.ok( stream, state.getMimeType() ).build();
    }

    return Response.ok( stream, mediaType ).build();
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

  private Response get404() {
    return Response.status( Response.Status.NOT_FOUND ).build();
  }

  private IPentahoAsyncExecutor getExecutor() {
    return PentahoSystem.get( IPentahoAsyncExecutor.class, PentahoAsyncExecutor.BEAN_NAME, null );
  }

  /**
   * In-place implementation to support streaming responses.
   * By default - even InputStream passed - streaming is not occurs.
   *
   */
  public static final class StreamingOutputWrapper implements StreamingOutput {

    private InputStream input;
    public static final byte[] BUFFER = new byte[8192];

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

    private Config( final boolean isSupportAsync, final long pollingIntervalMilliseconds ) {
      this.isSupportAsync = isSupportAsync;
      this.pollingIntervalMilliseconds = pollingIntervalMilliseconds;
    }


    public boolean isSupportAsync() {
      return isSupportAsync;
    }

    public long getPollingIntervalMilliseconds() {
      return pollingIntervalMilliseconds;
    }
  }
}
