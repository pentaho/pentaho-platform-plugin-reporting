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
 * Copyright 2006 - 2019 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.util.web.MimeHelper;
import org.pentaho.reporting.platform.plugin.async.AsyncExecutionStatus;
import org.pentaho.reporting.platform.plugin.async.AsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.ISchedulingDirectoryStrategy;
import org.pentaho.reporting.platform.plugin.async.JobIdGenerator;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;


@RunWith( PowerMockRunner.class )
@PowerMockIgnore( "jdk.internal.reflect.*" )
@PrepareForTest( PentahoSessionHolder.class )
public class JobManagerTest {

  private static final IPentahoSession session = mock( IPentahoSession.class );

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
    PentahoSystem.shutdown();
  }

  @Before
  public void before() {
    reset( executor );
    when( executor.getReportState( any( UUID.class ), any( IPentahoSession.class ) ) ).thenReturn( STATE );
    when( executor.schedule( any(), any() ) ).thenAnswer( i -> {
      STATUS = AsyncExecutionStatus.SCHEDULED;
      return true;
    } );
    when( executor.preSchedule( any(), any() ) ).thenAnswer( i -> {
      STATUS = AsyncExecutionStatus.PRE_SCHEDULED;
      return true;
    } );
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getPath() ).thenReturn( "/" );
    when( schedulingDirectoryStrategy.getSchedulingDir( any() ) ).thenReturn( file );
    STATUS = AsyncExecutionStatus.FAILED;
  }

  @Test public void testGetStatus() throws IOException {
    final JobManager jobManager = new JobManager();
    final Response response = jobManager.getStatus( uuid.toString() );
    assertNotNull( response );
    assertTrue( response.hasEntity() );

    final String json = response.readEntity( String.class );

    // currently no simple way to restore to AsyncReportState interface here
    // at least we get uuid in return.
    assertTrue( json.contains( uuid.toString() ) );
  }

  @Test
  public void calculateContentDisposition() throws Exception {
    final IAsyncReportState state1 =
      new AsyncReportState( UUID.randomUUID(), "/somepath/anotherlevel/file.prpt", AsyncExecutionStatus.FINISHED, 0, 0,
        0, 0, 0, 0, "", MimeHelper.MIMETYPE_CSV, "", false );

    final Response.ResponseBuilder builder1 = mock( Response.ResponseBuilder.class );

    JobManager.calculateContentDisposition( builder1, state1 );

    ArgumentCaptor<String> argument = ArgumentCaptor.forClass( String.class );

    verify( builder1, times( 1 ) ).header( eq( "Content-Description" ), argument.capture() );
    assertTrue( argument.getValue().contains( "file.prpt" ) );

    verify( builder1, times( 1 ) ).header( eq( "Content-Disposition" ), argument.capture() );
    assertTrue( argument.getValue().contains( "attachment; filename*=UTF-8''file.csv" ) );

    final IAsyncReportState state2 =
      new AsyncReportState( UUID.randomUUID(), "/somepath/anotherlevel/file.prpt", AsyncExecutionStatus.FINISHED, 0, 0,
        0, 0, 0, 0, "", MimeHelper.MIMETYPE_PDF, "", false );

    final Response.ResponseBuilder builder2 = mock( Response.ResponseBuilder.class );

    JobManager.calculateContentDisposition( builder2, state2 );

    argument = ArgumentCaptor.forClass( String.class );

    verify( builder2, times( 1 ) ).header( eq( "Content-Description" ), argument.capture() );
    assertTrue( argument.getValue().contains( "file.prpt" ) );

    verify( builder2, times( 1 ) ).header( eq( "Content-Disposition" ), argument.capture() );
    assertTrue( argument.getValue().contains( "inline; filename*=UTF-8''file.pdf" ) );
  }


  @Test public void testRequestPage() throws IOException {
    final JobManager jobManager = new JobManager();
    final Response response = jobManager.requestPage( uuid.toString(), 100 );
    assertNotNull( response );
    assertTrue( response.hasEntity() );

    final String page = response.readEntity( String.class );

    assertEquals( "100", page );
  }

  @Test public void testSchedule() throws IOException {
    final JobManager jobManager = new JobManager();
    final Response response = jobManager.schedule( uuid.toString(), true );
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );
  }

  @Test
  public void testInvalidUUID() {
    final JobManager jobManager = new JobManager();
    final Response response1 = jobManager.requestPage( "notauuid", 100 );
    assertEquals( response1.getStatus(), 404 );

    final Response response2 = jobManager.schedule( "notauuid", true );
    assertEquals( response2.getStatus(), 404 );

    final Response response3 = jobManager.getStatus( "notauuid" );
    assertEquals( response3.getStatus(), 404 );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmScheduling() throws Exception {
    final JobManager jobManager = new JobManager( true, 1000, 1000, true );

    STATUS = AsyncExecutionStatus.QUEUED;


    final UUID folderId = UUID.randomUUID();


    final Response response = jobManager.confirmSchedule( uuid.toString(), true, false, folderId.toString(), "test" );
    assertEquals( 200, response.getStatus() );
    verify( executor, times( 1 ) ).schedule( any(), any() );
    verify( executor, times( 1 ) )
      .updateSchedulingLocation( any(), any(), any(), any() );

    STATUS = AsyncExecutionStatus.FAILED;

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateLocationotScheduled() throws Exception {
    final JobManager jobManager = new JobManager( true, 1000, 1000, true );

    final UUID folderId = UUID.randomUUID();

    final Response response =
      jobManager.confirmSchedule( UUID.randomUUID().toString(), false, false, folderId.toString(), "test" );
    assertEquals( 404, response.getStatus() );


    STATUS = AsyncExecutionStatus.FAILED;

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testUpdateScheduleLocationDisabledPrompting() throws Exception {

    final JobManager jobManager = new JobManager();
    final UUID folderId = UUID.randomUUID();

    final Response response = jobManager.confirmSchedule( uuid.toString(), true, false, folderId.toString(), "test" );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;
  }

  @Test
  public void testGetExec() throws Exception {
    final JobManager jobManager = new JobManager();
    assertEquals( jobManager.getExecutor(), executor );
  }

  @Test public void testCancel() throws IOException {
    setSession();

    final UUID uuid = UUID.randomUUID();
    final JobManager jobManager = new JobManager();

    final Future future = mock( Future.class );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );


    final Response response = jobManager.cancel( uuid.toString() );
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );

    verify( future, times( 1 ) ).cancel( true );

  }


  @Test public void testContent() throws IOException, ExecutionException, InterruptedException {
    setSession();

    final UUID uuid = UUID.randomUUID();
    final JobManager jobManager = new JobManager();

    final Future future = mock( Future.class );
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    when( future.get() ).thenReturn( content );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );


    final Response response = jobManager.getContent( uuid.toString() );
    assertNotNull( response );
    assertEquals( 202, response.getStatus() );

    STATUS = AsyncExecutionStatus.FINISHED;

    final Response response1 = jobManager.getContent( uuid.toString() );

    assertNotNull( response1 );
    assertEquals( 200, response1.getStatus() );

  }


  @Test public void testFlowNoPropting() throws IOException, ExecutionException, InterruptedException {
    setSession();

    final UUID uuid = UUID.randomUUID();
    final JobManager jobManager = new JobManager();

    final Future future = mock( Future.class );
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    when( future.get() ).thenReturn( content );
    when( executor.getFuture( uuid, session ) ).thenReturn( future );


    final Response response = jobManager.getContent( uuid.toString() );
    assertNotNull( response );

    assertEquals( 202, response.getStatus() );

    STATUS = AsyncExecutionStatus.FINISHED;

    final Response response1 = jobManager.getContent( uuid.toString() );

    assertNotNull( response1 );
    assertEquals( 200, response1.getStatus() );

  }


  @Test public void testPreSchedule() throws IOException {
    final JobManager jobManager = new JobManager();

    final Response response = jobManager.schedule( UUID.randomUUID().toString(), false );
    assertNotNull( response );
    assertEquals( 200, response.getStatus() );
    assertEquals( STATUS, AsyncExecutionStatus.PRE_SCHEDULED );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmSchedulingNoFolderId() throws Exception {
    final JobManager jobManager = new JobManager( true, 1000, 1000, true );


    Response response = jobManager.confirmSchedule( UUID.randomUUID().toString(), true, true, null, "test" );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testConfirmSchedulingNoNewName() throws Exception {
    final JobManager jobManager = new JobManager( true, 1000, 1000, true );


    final Response response =
      jobManager.confirmSchedule( UUID.randomUUID().toString(), true, true, UUID.randomUUID().toString(), null );
    assertEquals( 404, response.getStatus() );

    STATUS = AsyncExecutionStatus.FAILED;

  }

  @Test
  public void testRecalcNotFinished() throws Exception {
    final JobManager jobManager = new JobManager( true, 1000, 1000, true );

    STATUS = AsyncExecutionStatus.QUEUED;


    final UUID folderId = UUID.randomUUID();


    final Response response = jobManager.confirmSchedule( uuid.toString(), true, true, folderId.toString(), "test" );
    assertEquals( 200, response.getStatus() );
    verify( executor, times( 0 ) ).recalculate( any(), any() );
    verify( executor, times( 1 ) ).schedule( any(), any() );
    verify( executor, times( 1 ) )
      .updateSchedulingLocation( any(), any(), any(), any() );

    STATUS = AsyncExecutionStatus.FAILED;

  }


  @Test
  public void testRecalcFinishedExecutorFailed() throws Exception {

    setSession();

    final JobManager jobManager = new JobManager( true, 1000, 1000, true );

    STATUS = AsyncExecutionStatus.FINISHED;


    final UUID folderId = UUID.randomUUID();
    final Response response = jobManager.confirmSchedule( uuid.toString(), true, true, folderId.toString(), "test" );
    assertEquals( 200, response.getStatus() );
    verify( executor, times( 1 ) ).recalculate( any(), any() );
    verify( executor, times( 1 ) ).schedule( uuid, session );
    verify( executor, times( 1 ) )
      .updateSchedulingLocation( any(), any(), any(), any() );

    STATUS = AsyncExecutionStatus.FAILED;

  }

  @Test
  public void testRecalcFinished() throws Exception {

    setSession();

    final JobManager jobManager = new JobManager( true, 1000, 1000, true );

    STATUS = AsyncExecutionStatus.FINISHED;

    final UUID reclcId = UUID.randomUUID();
    when( executor.recalculate( any(), any() ) ).thenReturn( reclcId );


    final UUID folderId = UUID.randomUUID();
    final Response response = jobManager.confirmSchedule( uuid.toString(), true, true, folderId.toString(), "test" );
    assertEquals( 200, response.getStatus() );
    verify( executor, times( 1 ) ).recalculate( any(), any() );
    verify( executor, times( 1 ) ).schedule( reclcId, session );
    verify( executor, times( 1 ) )
      .updateSchedulingLocation( reclcId, session, folderId.toString(), "test" );

    STATUS = AsyncExecutionStatus.FAILED;

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
    setSession();

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
    PowerMockito.mockStatic( PentahoSessionHolder.class );
    when( PentahoSessionHolder.getSession() ).thenReturn( null );

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
      setSession();
      final JobManager jobManager = new JobManager( false, 1000, 1000 );
      final Response response = jobManager.reserveId();
      assertEquals( 404, response.getStatus() );
    } finally {
      when( objFactory.get( any( Class.class ), eq( "IJobIdGenerator" ), any( IPentahoSession.class ) ) )
        .thenReturn( new JobIdGenerator() );
    }
  }

  private void setSession() {
    PowerMockito.mockStatic( PentahoSessionHolder.class );
    when( PentahoSessionHolder.getSession() ).thenReturn( session );
    assertEquals( session, PentahoSessionHolder.getSession() );
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
