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
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.ObjectMapper;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.RepositoryPathEncoder;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IJobIdGenerator;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.ISchedulingDirectoryStrategy;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Future;

@Path( "/reporting/api/jobs" )
public class JobManager {

  private static final Log logger = LogFactory.getLog( JobManager.class );
  private static final String ASYNC_DISABLED = "JobManager initialization: async mode marked as disabled.";
  private static final String ERROR_GENERATING_REPORT = "Error generating report";
  private static final String UNABLE_TO_SERIALIZE_TO_JSON = "Unable to serialize to json : ";
  private static final String UNCKNOWN_MEDIA_TYPE = "Can't determine JAX-RS media type for: ";
  private final Config config;

  public JobManager() {
    this( true, 500, 1500, false );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                     final long dialogThresholdMillisecond ) {
    this( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMillisecond, false );
  }

  public JobManager( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                     final long dialogThresholdMillisecond, final boolean promptForLocation ) {
    if ( !isSupportAsync ) {
      logger.info( ASYNC_DISABLED );
    }
    this.config = new Config( isSupportAsync, pollingIntervalMilliseconds, dialogThresholdMillisecond,
      promptForLocation );
  }


  @GET @Path( "config" ) public Response getConfig() {
    return getJson( config );
  }


  @GET @Path( "{job_id}/content" )
  public Response getPDFContent( @PathParam( "job_id" ) final String job_id ) throws IOException {
    logger.debug( "Chrome pdf viewer workaround. See BACKLOG-7598 for details" );

    return this.getContent( job_id );
  }

  @SuppressWarnings( "unchecked" )
  @POST @Path( "{job_id}/content" ) public Response getContent( @PathParam( "job_id" ) final String jobId )
    throws IOException {

    try {
      final ExecutionContext context = getContext( jobId );
      final Future<IFixedSizeStreamingContent> future = context.getFuture();
      final IAsyncReportState state = context.getReportState();

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

      MediaType mediaType;
      Response.ResponseBuilder response;

      try {
        mediaType = MediaType.valueOf( state.getMimeType() );
      } catch ( final Exception e ) {
        logger.warn( UNCKNOWN_MEDIA_TYPE + state.getMimeType(), e );
        //Downloadable type
        mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
      }

      response = Response.ok( stream, mediaType );

      response = noCache( response );
      response = calculateContentDisposition( response, state );

      return response.build();

    } catch ( final ContextFailedException | FutureNotFoundException e ) {
      return get404();
    }
  }


  @GET @Path( "{job_id}/status" ) @Produces( "application/json" )
  public Response getStatus( @PathParam( "job_id" ) final String jobId ) {

    try {

      final ExecutionContext context = getContext( jobId );

      final IAsyncReportState responseJson = context.getReportState();

      return getJson( responseJson );
    } catch ( final ContextFailedException e ) {
      return get404();
    }
  }

  private Response getJson( final Object responseJson ) {
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
    try {

      final ExecutionContext context = getContext( jobId );

      final Future<InputStream> future = context.getFuture();
      final IAsyncReportState state = context.getReportState();

      logger.debug( "Cancellation of report: " + state.getPath() + ", requested by : " + context.getSession() );

      future.cancel( true );

      return Response.ok().build();
    } catch ( final ContextFailedException e ) {
      return get404();
    } catch ( final FutureNotFoundException e ) {
      return Response.ok().build();
    }
  }


  @GET @Path( "{job_id}/requestPage/{page}" ) @Produces( "text/text" )
  public Response requestPage( @PathParam( "job_id" ) final String jobId, @PathParam( "page" ) final int page ) {
    try {

      final ExecutionContext context = getContext( jobId );

      context.requestPage( page );

      return Response.ok( String.valueOf( page ) ).build();
    } catch ( final ContextFailedException e ) {
      return get404();
    }
  }


  @GET @Path( "{job_id}/schedule" ) @Produces( "text/text" )
  public Response schedule( @PathParam( "job_id" ) final String jobId,
                            @DefaultValue( "true" )
                            @QueryParam( "confirm" ) final boolean confirm ) {
    try {
      ExecutionContext context = getContext( jobId );

      if ( confirm ) {
        if ( context.needRecalculation( Boolean.FALSE ) ) {
          //Get new job id
          final UUID recalculate = context.recalculate();
          if ( null != recalculate ) {
            context = getContext( recalculate.toString() );
          }
        }
        context.schedule();
      } else {
        context.preSchedule();
      }

      return Response.ok().build();
    } catch ( final ContextFailedException e ) {
      return get404();
    }
  }


  @POST @Path( "{job_id}/schedule" ) @Produces( "application/json" )
  public Response confirmSchedule( @PathParam( "job_id" ) final String jobId,
                                   @DefaultValue( "true" )
                                   @QueryParam( "confirm" ) final boolean confirm,
                                   @DefaultValue( "false" )
                                   @QueryParam( "recalculateFinished" ) final boolean recalculateFinished,
                                   @QueryParam( "folderId" ) final String folderId,
                                   @QueryParam( "newName" ) final String newName ) {
    try {

      //We can't go further without folder id and file name
      if ( StringUtil.isEmpty( folderId ) || StringUtil.isEmpty( newName ) ) {
        return get404();
      }

      ExecutionContext context = getContext( jobId );


      //The report can be already scheduled but we still may want to update the location
      if ( confirm ) {
        if ( context.needRecalculation( recalculateFinished ) ) {
          //Get new job id
          final UUID recalculate = context.recalculate();
          if ( null != recalculate ) {
            context = getContext( recalculate.toString() );
          }
        }
        context.schedule();
      }

      //Update the location
      context.updateSchedulingLocation( folderId, newName );

      return getJson( Collections.singletonMap( "uuid", context.jobId ) );
    } catch ( final ContextFailedException e ) {
      return get404();
    }
  }

  public ExecutionContext getContext( final String jobId ) throws ContextFailedException {
    final ExecutionContext executionContext = new ExecutionContext( jobId );
    executionContext.evaluate();
    return executionContext;
  }

  @POST @Path( "reserveId" ) @Produces( "application/json" )
  public Response reserveId() {
    final IPentahoSession session = PentahoSessionHolder.getSession();
    final IJobIdGenerator iJobIdGenerator = PentahoSystem.get( IJobIdGenerator.class );
    if ( session != null && iJobIdGenerator != null ) {
      final UUID reservedId = iJobIdGenerator.generateId( session );
      return getJson( Collections.singletonMap( "reservedId", reservedId.toString() ) );
    } else {
      return get404();
    }
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
    if ( StringUtil.isEmpty( cleanFileName ) ) {
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

  @JsonPropertyOrder( alphabetic = true ) //stable response structure
  private class Config {
    private final boolean isSupportAsync;
    private final long pollingIntervalMilliseconds;
    private final long dialogThresholdMilliseconds;
    private final boolean promptForLocation;


    private Config( final boolean isSupportAsync, final long pollingIntervalMilliseconds,
                    final long dialogThresholdMilliseconds, final boolean promptForLocation ) {
      this.isSupportAsync = isSupportAsync;
      this.pollingIntervalMilliseconds = pollingIntervalMilliseconds;
      this.dialogThresholdMilliseconds = dialogThresholdMilliseconds;
      this.promptForLocation = promptForLocation;
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

    public boolean isPromptForLocation() {
      return promptForLocation;
    }

    //Location can be changed at any time depending on ISchedulingDirectoryStrategy implementation
    public String getDefaultOutputPath() {
      return getLocation();
    }
  }

  private String getLocation() {
    final ISchedulingDirectoryStrategy directoryStrategy = PentahoSystem.get( ISchedulingDirectoryStrategy.class );
    final IUnifiedRepository repository = PentahoSystem.get( IUnifiedRepository.class );
    if ( directoryStrategy != null && repository != null ) {
      //We may consider caching in strategy implementation
      final RepositoryFile outputFolder = directoryStrategy.getSchedulingDir( repository );
      return outputFolder.getPath();
    }
    return "/";
  }

  /**
   * Used to get context for operation execution and validate it
   */
  public class ExecutionContext {
    private IPentahoSession session;
    private final String jobId;
    private UUID uuid = null;

    private ExecutionContext( final String jobId ) {
      this.jobId = jobId;
    }


    private void evaluate() throws ContextFailedException {
      try {
        this.session = PentahoSessionHolder.getSession();
        this.uuid = UUID.fromString( jobId );
      } catch ( final Exception e ) {
        logger.error( e );
        throw new ContextFailedException( e );
      }
    }


    public IPentahoSession getSession() {
      return session;
    }

    //Be sure to get it from context each time to make it work for PIR too
    private IPentahoAsyncExecutor getReportExecutor() {
      return getExecutor();
    }


    public Future getFuture() throws FutureNotFoundException {
      final Future future = getReportExecutor().getFuture( uuid, session );
      if ( future == null ) {
        throw new FutureNotFoundException( "Can't get future" );
      }
      return future;
    }

    public IAsyncReportState getReportState() throws ContextFailedException {
      final IAsyncReportState reportState = getReportExecutor().getReportState( uuid, session );
      if ( reportState == null ) {
        throw new ContextFailedException( "Can't get state" );
      }
      return reportState;
    }

    public void requestPage( final int page ) throws ContextFailedException {
      //Check if there is a task
      getReportState();
      getReportExecutor().requestPage( uuid, session, page );
    }

    public void schedule() throws ContextFailedException {
      //Check if there is a task
      final IAsyncReportState reportState = getReportState();
      if ( reportState.getStatus().equals( AsyncExecutionStatus.SCHEDULED ) ) {
        throw new ContextFailedException( "Report is already scheduled." );
      }
      getReportExecutor().schedule( uuid, session );
    }

    public void updateSchedulingLocation( final String folderId, final String newName ) throws ContextFailedException {
      if ( !config.isPromptForLocation() ) {
        throw new ContextFailedException( "Location update is disabled" );
      }
      //Check if there is a task
      final IAsyncReportState reportState = getReportState();
      if ( reportState.getStatus().equals( AsyncExecutionStatus.SCHEDULED ) ) {
        getReportExecutor().updateSchedulingLocation( uuid, session, folderId, newName );
      } else {
        throw new ContextFailedException( "Can't update the location of not scheduled report." );
      }
    }

    public void preSchedule() throws ContextFailedException {
      //Check if there is a task
      getReportState();
      getReportExecutor().preSchedule( uuid, session );
    }

    public UUID recalculate() throws ContextFailedException {
      //Check if there is a task
      getReportState();
      return getReportExecutor().recalculate( uuid, session );
    }

    public boolean needRecalculation( final boolean recalculateFinished ) throws ContextFailedException {
      return ( AsyncExecutionStatus.FINISHED.equals( getReportState().getStatus() ) && recalculateFinished )
        || isRowLimitRecalculationNeeded();
    }

    private boolean isRowLimitRecalculationNeeded() throws ContextFailedException {
      try {
        final IAsyncReportState state = this.getReportState();
        final String path = state.getPath();
        final MasterReport report = ReportCreator.createReportByName( path );
        final int queryLimit = report.getQueryLimit();
        if ( queryLimit > 0 ) {
          return Boolean.TRUE;
        } else {
          if ( state.getIsQueryLimitReached() ) {
            return Boolean.TRUE;
          }
        }
        return Boolean.FALSE;
      } catch ( ResourceException | IOException e ) {
        return Boolean.FALSE;
      }
    }
  }

  public static class ContextFailedException extends Exception {

    public ContextFailedException( final String message ) {
      super( message );
    }

    ContextFailedException( final Throwable cause ) {
      super( cause );
    }
  }

  private static class FutureNotFoundException extends Exception {
    public FutureNotFoundException( final String message ) {
      super( message );
    }
  }

}
