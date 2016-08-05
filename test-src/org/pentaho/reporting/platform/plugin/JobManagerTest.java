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

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.impl.ResponseBuilderImpl;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.AsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.ISchedulingDirectoryStrategy;
import org.pentaho.reporting.platform.plugin.async.JobIdGenerator;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

public class JobManagerTest {

  private static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";

  private static final IPentahoSession session = mock( IPentahoSession.class );

  private static JaxRsServerProvider provider;
  private static PentahoAsyncExecutor executor = null;
  private static final UUID uuid = UUID.randomUUID();

  private static final String MIME = "junit_mime";
  private static final String PATH = "junit_path";
  private static int PROGRESS = -113;
  private static AsyncExecutionStatus STATUS = AsyncExecutionStatus.FAILED;
  private static ISchedulingDirectoryStrategy schedulingDirectoryStrategy;

  private static volatile IAsyncReportState STATE;
  private static IPentahoObjectFactory objFactory;

  @BeforeClass public static void setUp() throws Exception {
    provider = new JaxRsServerProvider();
    provider.startServer( new JobManager() );
    STATE = getState();
    executor = mock( PentahoAsyncExecutor.class );
    schedulingDirectoryStrategy = mock( ISchedulingDirectoryStrategy.class );


    PentahoSystem.init();
    objFactory = mock( IPentahoObjectFactory.class );

    // return mock executor for any call to it's bean name.
    when( objFactory.objectDefined( anyString() ) ).thenReturn( true );
    when( objFactory.objectDefined( any( Class.class ) ) ).thenReturn( true );
    when( objFactory.get( any( Class.class ), eq( PentahoAsyncExecutor.BEAN_NAME ), any( IPentahoSession.class ) ) )
      .thenReturn( executor );

    when( objFactory.get( any( Class.class ), eq( "ISchedulingDirectoryStrategy" ), any( IPentahoSession.class ) ) )
      .thenReturn( schedulingDirectoryStrategy );

    final IUnifiedRepository repository = mock( IUnifiedRepository.class );
    when( objFactory.get( any( Class.class ), eq( "IUnifiedRepository" ), any( IPentahoSession.class ) ) )
      .thenReturn( repository );

    when( objFactory.get( any( Class.class ), eq( "IJobIdGenerator" ), any( IPentahoSession.class ) ) )
      .thenReturn( new JobIdGenerator() );

    PentahoSystem.registerObjectFactory( objFactory );
  }

  @AfterClass
  public static void tearDown() throws Exception {
    PentahoSessionHolder.removeSession();
    PentahoSystem.shutdown();
    provider.stopServer();
  }

  @Before
  public void before() {
    reset( executor );
    when( executor.getReportState( any( UUID.class ), any( IPentahoSession.class ) ) ).thenReturn( STATE );
    when( executor.schedule( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( final InvocationOnMock i ) throws Throwable {
        STATUS = AsyncExecutionStatus.SCHEDULED;
        return true;
      }
    } );
    when( executor.preSchedule( any(), any() ) ).thenAnswer( new Answer<Object>() {
      @Override public Object answer( final InvocationOnMock i ) throws Throwable {
        STATUS = AsyncExecutionStatus.PRE_SCHEDULED;
        return true;
      }
    } );
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getPath() ).thenReturn( "/" );
    when( schedulingDirectoryStrategy.getSchedulingDir( any() ) ).thenReturn( file );
    STATUS = AsyncExecutionStatus.FAILED;
    PentahoSessionHolder.setSession( session );
  }

  @Test public void testGetStatus() throws IOException {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, UUID.randomUUID().toString(), "/status" ) );
    final Response response = client.get();
    assertNotNull( response );
    assertTrue( response.hasEntity() );

    final String json = response.readEntity( String.class );

    // currently no simple way to restore to AsyncReportState interface here
    // at least we get uuid in return.
    assertTrue( json.contains( uuid.toString() ) );
  }

  @Test
  public void calculateContentDisposition() throws Exception {
    final IAsyncReportState state =
      new AsyncReportState( UUID.randomUUID(), "/somepath/anotherlevel/file.prpt", AsyncExecutionStatus.FINISHED, 0, 0,
        0, 0, 0, 0, "", "text/csv", "", false );

    final Response.ResponseBuilder builder = new ResponseBuilderImpl();

    JobManager.calculateContentDisposition( builder, state );
    final Response resp = builder.build();
    final MultivaluedMap<String, String> stringHeaders = resp.getStringHeaders();
    assertTrue( stringHeaders.get( "Content-Description" ).contains( "file.prpt" ) );
    assertTrue( stringHeaders.get( "Content-Disposition" ).contains( "inline; filename*=UTF-8''file.csv" ) );
    resp.close();
  }


  @Test public void testRequestPage() throws IOException {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, UUID.randomUUID().toString(), "/requestPage/100" ) );

    final Response response = client.get();
    assertNotNull( response );
    assertTrue( response.hasEntity() );

    final String page = response.readEntity( String.class );

    assertEquals( "100", page );
  }

  @Test public void testSchedule() throws IOException {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, UUID.randomUUID().toString(), "/schedule" ) );
    final Response response = client.get();
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );
  }

  @Test
  public void testInvalidUUID() {
    WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, null, "/requestPage/100" ) );
    final Response response1 = client.get();
    assertEquals( response1.getStatus(), 404 );
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, "", "/schedule" ) );
    final Response response2 = client.get();
    assertEquals( response2.getStatus(), 404 );
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, "not a uuid", "/status" ) );
    final Response response3 = client.get();
    assertEquals( response3.getStatus(), 404 );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmScheduling() throws Exception {
    try {


      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      STATUS = AsyncExecutionStatus.QUEUED;

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", true );
      client.query( "folderId", folderId );
      client.query( "newName", "test" );

      final Response response = client.post( null );
      assertEquals( 200, response.getStatus() );
      verify( executor, times( 1 ) ).schedule( any(), any() );
      verify( executor, times( 1 ) )
        .updateSchedulingLocation( any(), any(), any(), any() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateLocationotScheduled() throws Exception {
    try {


      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", false );
      client.query( "folderId", folderId );
      client.query( "newName", "blabla" );

      Response response = client.post( null );
      assertEquals( 404, response.getStatus() );


      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocationDisabledPrompting() throws Exception {

    WebClient client = provider.getFreshClient();
    final UUID folderId = UUID.randomUUID();
    final String config = String.format( URL_FORMAT, uuid, "/schedule" );
    client.path( config );
    client.query( "newName", folderId );
    client.query( "folderId", folderId );

    Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;
  }

  @Test
  public void testGetExec() throws Exception {
    final JobManager jobManager = new JobManager();
    assertEquals( jobManager.getExecutor(), executor );
  }

  @Test public void testCancel() throws IOException {
    final UUID uuid = UUID.randomUUID();
    final WebClient client = provider.getFreshClient();

    final Future future = mock( Future.class );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );
    final String config = String.format( URL_FORMAT, uuid, "/cancel" );


    client.path( config );

    final Response response = client.get();
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );

    verify( future, times( 1 ) ).cancel( true );

  }


  @Test public void testContent() throws IOException, ExecutionException, InterruptedException {
    final UUID uuid = UUID.randomUUID();
    final WebClient client = provider.getFreshClient();

    final Future future = mock( Future.class );
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    when( future.get() ).thenReturn( content );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );
    final String config = String.format( URL_FORMAT, uuid, "/content" );


    client.path( config );

    final Response response = client.get();
    assertNotNull( response );
    assertEquals( 202, response.getStatus() );

    STATUS = AsyncExecutionStatus.FINISHED;

    final Response response1 = client.get();

    assertNotNull( response1 );
    assertEquals( 200, response1.getStatus() );

  }


  @Test public void testFlowNoPropting() throws IOException, ExecutionException, InterruptedException {
    final UUID uuid = UUID.randomUUID();
    final WebClient client = provider.getFreshClient();

    final Future future = mock( Future.class );
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    when( future.get() ).thenReturn( content );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );
    final String config = String.format( URL_FORMAT, uuid, "/content" );

    client.path( config );

    final Response response = client.get();
    assertNotNull( response );

    assertEquals( 202, response.getStatus() );

    STATUS = AsyncExecutionStatus.FINISHED;

    final Response response1 = client.get();

    assertNotNull( response1 );
    assertEquals( 200, response1.getStatus() );

  }

  @Test public void testPreSchedule() throws IOException {
    final WebClient client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, UUID.randomUUID().toString(), "/schedule" ) );
    client.query( "confirm", false );
    final Response response = client.get();
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );
    assertEquals( STATUS, AsyncExecutionStatus.PRE_SCHEDULED );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmSchedulingNoFolderId() throws Exception {
    try {

      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", false );

      client.query( "newName", "blabla" );

      Response response = client.post( null );
      assertEquals( 404, response.getStatus() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmSchedulingNoNewName() throws Exception {
    try {

      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      WebClient client = provider.getFreshClient();

      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", false );
      client.query( "folderId", "blabla" );

      Response response = client.post( null );
      assertEquals( 404, response.getStatus() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @Test
  public void testRecalcNotFinished() throws Exception {
    try {


      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      STATUS = AsyncExecutionStatus.QUEUED;

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", true );
      client.query( "folderId", folderId );
      client.query( "newName", "test" );
      client.query( "recalculateFinished", "true" );

      final Response response = client.post( null );
      assertEquals( 200, response.getStatus() );
      verify( executor, times( 0 ) ).recalculate( any(), any() );
      verify( executor, times( 1 ) ).schedule( any(), any() );
      verify( executor, times( 1 ) )
        .updateSchedulingLocation( any(), any(), any(), any() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }


  @Test
  public void testRecalcFinishedExecutorFailed() throws Exception {
    try {
      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      STATUS = AsyncExecutionStatus.FINISHED;

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", true );
      client.query( "folderId", folderId );
      client.query( "newName", "test" );
      client.query( "recalculateFinished", "true" );

      final Response response = client.post( null );
      assertEquals( 200, response.getStatus() );
      verify( executor, times( 1 ) ).recalculate( any(), any() );
      verify( executor, times( 1 ) ).schedule( uuid, session );
      verify( executor, times( 1 ) )
        .updateSchedulingLocation( any(), any(), any(), any() );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @Test
  public void testRecalcFinished() throws Exception {
    try {
      provider.stopServer();
      provider.startServer( new JobManager( true, 1000, 1000, true ) );

      STATUS = AsyncExecutionStatus.FINISHED;

      final UUID reclcId = UUID.randomUUID();
      when( executor.recalculate( any(), any() ) ).thenReturn( reclcId );

      WebClient client = provider.getFreshClient();
      final UUID folderId = UUID.randomUUID();
      final String config = String.format( URL_FORMAT, uuid, "/schedule" );
      client.path( config );
      client.query( "confirm", true );
      client.query( "folderId", folderId );
      client.query( "newName", "test" );
      client.query( "recalculateFinished", "true" );

      final Response response = client.post( null );
      assertEquals( 200, response.getStatus() );
      verify( executor, times( 1 ) ).recalculate( any(), any() );
      verify( executor, times( 1 ) ).schedule( reclcId, session );
      verify( executor, times( 1 ) )
        .updateSchedulingLocation( reclcId, session, folderId.toString(), "test" );

      STATUS = AsyncExecutionStatus.FAILED;
    } finally {
      provider.stopServer();
      provider.startServer( new JobManager() );
    }
  }

  @Test
  public void testAsyncDisabled() throws Exception {
    final JobManager jobManager = new JobManager( false, 1000, 1000 );
    final Response config = jobManager.getConfig();
    assertNotNull( config );
    assertNotNull( config.getEntity() );
    assertTrue( config.getEntity() instanceof String );
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode jsonNode = mapper.readTree( (String) config.getEntity() );
    assertNotNull( jsonNode );
    assertFalse( jsonNode.get( "supportAsync" ).asBoolean() );
  }

  @Test
  public void testReserveId() throws Exception {
    final JobManager jobManager = new JobManager( false, 1000, 1000 );
    final Response response = jobManager.reserveId();
    assertEquals( 200, response.getStatus() );
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode jsonNode = mapper.readTree( (String) response.getEntity() );
    assertNotNull( jsonNode );
    assertNotNull( jsonNode.get( "reservedId" ).asText() );
  }


  @Test
  public void testReserveIdNoSession() throws Exception {
    PentahoSessionHolder.removeSession();
    assertNull( PentahoSessionHolder.getSession() );
    final JobManager jobManager = new JobManager( false, 1000, 1000 );
    final Response response = jobManager.reserveId();
    assertEquals( 404, response.getStatus() );
  }


  @Test
  public void testReserveIdNoGenerator() throws Exception {
    try {
      when( objFactory.get( any( Class.class ), eq( "IJobIdGenerator" ), any( IPentahoSession.class ) ) )
        .thenReturn( null );
      final JobManager jobManager = new JobManager( false, 1000, 1000 );
      final Response response = jobManager.reserveId();
      assertEquals( 404, response.getStatus() );
    } finally {
      when( objFactory.get( any( Class.class ), eq( "IJobIdGenerator" ), any( IPentahoSession.class ) ) )
        .thenReturn( new JobIdGenerator() );
    }
  }

  public static IAsyncReportState getState() {
    return new IAsyncReportState() {

      @Override public String getPath() {
        return PATH;
      }

      @Override public UUID getUuid() {
        return uuid;
      }

      @Override public AsyncExecutionStatus getStatus() {
        return STATUS;
      }

      @Override public int getProgress() {
        return PROGRESS;
      }

      @Override public int getPage() {
        return 0;
      }

      @Override public int getTotalPages() {
        return 0;
      }

      @Override public int getGeneratedPage() {
        return 0;
      }

      @Override public int getRow() {
        return 0;
      }

      @Override public int getTotalRows() {
        return 0;
      }

      @Override public String getActivity() {
        return null;
      }

      @Override public String getMimeType() {
        return MIME;
      }

      @Override public String getErrorMessage() {
        return null;
      }

      @Override public boolean getIsQueryLimitReached() {
        return false;
      }

    };
  }
}
