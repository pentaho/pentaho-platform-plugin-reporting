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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import com.google.common.util.concurrent.ListenableFuture;
import org.apache.commons.io.input.NullInputStream;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.platform.util.StringUtil;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressEvent;
import org.pentaho.reporting.engine.classic.core.event.ReportProgressListener;
import org.pentaho.reporting.platform.plugin.AuditWrapper;
import org.pentaho.reporting.platform.plugin.JaxRsServerProvider;
import org.pentaho.reporting.platform.plugin.JobManager;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.SimpleReportingComponent;
import org.pentaho.reporting.platform.plugin.cache.FileSystemCacheBackend;
import org.pentaho.reporting.platform.plugin.cache.PluginCacheManagerImpl;
import org.pentaho.reporting.platform.plugin.cache.PluginSessionCache;
import org.pentaho.reporting.platform.plugin.output.FastExportReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.output.ReportOutputHandlerFactory;
import org.pentaho.reporting.platform.plugin.staging.AsyncJobFileStagingHandler;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class AsyncIT {

  private static final String URL_FORMAT = "/reporting/api/jobs/%1$s%2$s";
  private static final String MIME = "junit_mime";
  private static final String PATH = "junit_path";
  private static final NullInputStream NULL_INPUT_STREAM = new NullInputStream( 100 );
  private static int PAGE = 0;
  private static int PROGRESS = -113;
  private static AsyncExecutionStatus STATUS = AsyncExecutionStatus.FAILED;

  private static MicroPlatform microPlatform;
  private static FileSystemCacheBackend fileSystemCacheBackend;

  private static JaxRsServerProvider provider;
  private static IPentahoSession session;
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );
  AsyncJobFileStagingHandler handler = mock( AsyncJobFileStagingHandler.class );

  @BeforeClass
  public static void setUp() throws Exception {
    provider = new JaxRsServerProvider();
    provider.startServer( new JobManager() );
    fileSystemCacheBackend = new FileSystemCacheBackend();
    fileSystemCacheBackend.setCachePath( "/test-cache/" );
    microPlatform = MicroPlatformFactory.create();
    microPlatform.define( ReportOutputHandlerFactory.class, FastExportReportOutputHandlerFactory.class );
    microPlatform
      .define( "IPluginCacheManager", new PluginCacheManagerImpl( new PluginSessionCache( fileSystemCacheBackend ) ) );
    final PentahoAsyncExecutor<AsyncReportState> executor = new PentahoAsyncExecutor<>( 2 );
    microPlatform.define( "IPentahoAsyncExecutor", executor );
    microPlatform.define( "ISchedulingDirectoryStrategy", new HomeSchedulingDirStrategy() );
    microPlatform.addLifecycleListener( new AsyncSystemListener() );
    microPlatform.start();

    session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }


  @AfterClass
  public static void tearDown() throws PlatformInitializationException {
    PentahoSystem.shutdown();
    fileSystemCacheBackend.purge( Collections.singletonList( "" ) );
    microPlatform.stop();
    microPlatform = null;
    provider.stopServer();
    provider = null;
  }

  @Test
  public void testDefaultConfig() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "config", "" );
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertEquals( json,
      "{\"pollingIntervalMilliseconds\":500,\"dialogThresholdMilliseconds\":1500,\"supportAsync\":true}" );
  }

  @Test
  public void testCustomConfig() throws Exception {
    provider.stopServer();
    provider.startServer( new JobManager( false, 100L, 300L ) );
    final String config = String.format( URL_FORMAT, "config", "" );
    final WebClient client = provider.getFreshClient();
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertEquals( json,
      "{\"pollingIntervalMilliseconds\":100,\"dialogThresholdMilliseconds\":300,\"supportAsync\":false}" );
    provider.stopServer();
    provider.startServer( new JobManager() );
  }

  @Test
  public void testStatusNoTask() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/status" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }

  @Test
  public void testStatusInvalidId() {
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "noidhereman", "/status" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testQueueReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/status" );
    client.path( config );
    final Response response = client.get();
    final String json = response.readEntity( String.class );
    assertFalse( StringUtil.isEmpty( json ) );
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();


    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/schedule" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    WebClient client1 = provider.getFreshClient();
    client1.path( String.format( URL_FORMAT, uuid, "/status" ) );
    Response response1 = client1.get();
    final String json = response1.readEntity( String.class );
    assertTrue( json.contains( "SCHEDULED" ) );

    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/schedule" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 200, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testScheduleInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mockExec();

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/schedule" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testCancelReport() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<Void>() {
      @Override public Void answer( final InvocationOnMock invocation ) throws Throwable {
        STATUS = AsyncExecutionStatus.CANCELED;
        return null;
      }
    } ).when( listenableFuture ).cancel( true );

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/cancel" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/status" ) );
    response = client.get();
    final String json = response.readEntity( String.class );
    assertTrue( json.contains( "CANCELED" ) );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testCacelWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/cancel" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testCancelInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/cancel" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPage() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    doAnswer( new Answer() {
      @Override public Object answer( final InvocationOnMock invocation ) throws Throwable {
        final Object o = invocation.getArguments()[ 0 ];
        PAGE = (int) o;
        return o;
      }
    } ).when( mock ).requestPage( anyInt() );

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/requestPage/100" );
    client.path( config );
    Response response = client.get();
    assertEquals( 200, response.getStatus() );
    assertEquals( "100", response.readEntity( String.class ) );
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/status" ) );
    response = client.get();
    final String json = response.readEntity( String.class );
    assertTrue( json.contains( "\"page\":100" ) );
    STATUS = AsyncExecutionStatus.FAILED;
    PAGE = 0;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPageWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/requestPage/100" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 200, response.getStatus() );
    assertEquals( "100", response.readEntity( String.class ) );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testRequestPageInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/requestPage/1" );
    client.path( config );
    final Response response = client.get();
    assertEquals( 404, response.getStatus() );

  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContent() throws ExecutionException, InterruptedException {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<IFixedSizeStreamingContent>() {
      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
        return new IFixedSizeStreamingContent() {

          @Override public InputStream getStream() {
            return NULL_INPUT_STREAM;
          }

          @Override public long getContentSize() {
            return 0;
          }

          @Override public boolean cleanContent() {
            return false;
          }
        };

      }
    } ).when( listenableFuture ).get();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/content" );
    client.path( config );
    Response response = client.post( null );
    assertEquals( 202, response.getStatus() );
    STATUS = AsyncExecutionStatus.FINISHED;
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/content" ) );
    response = client.post( null );
    assertEquals( 200, response.getStatus() );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentGet() throws ExecutionException, InterruptedException {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final AbstractAsyncReportExecution mock = mock( AbstractAsyncReportExecution.class );
    final IAsyncReportState state = getState();
    when( mock.getState() ).thenReturn( state );
    final ListenableFuture listenableFuture = mock( ListenableFuture.class );
    when( mock.delegate( any( ListenableFuture.class ) ) ).thenReturn( listenableFuture );
    doAnswer( new Answer<IFixedSizeStreamingContent>() {
      @Override public IFixedSizeStreamingContent answer( final InvocationOnMock invocation ) throws Throwable {
        return new IFixedSizeStreamingContent() {

          @Override public InputStream getStream() {
            return NULL_INPUT_STREAM;
          }

          @Override public long getContentSize() {
            return 0;
          }

          @Override public boolean cleanContent() {
            return false;
          }
        };

      }
    } ).when( listenableFuture ).get();

    final UUID uuid = executor.addTask( mock, PentahoSessionHolder.getSession() );
    WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, uuid, "/content" );
    client.path( config );
    Response response = client.get();
    assertEquals( 202, response.getStatus() );
    STATUS = AsyncExecutionStatus.FINISHED;
    client = provider.getFreshClient();
    client.path( String.format( URL_FORMAT, uuid, "/content" ) );
    response = client.get();
    assertEquals( 200, response.getStatus() );
    STATUS = AsyncExecutionStatus.FAILED;
  }


  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentWrongID() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, UUID.randomUUID(), "/content" );
    client.path( config );
    final Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testContentInvalid() {
    final IPentahoAsyncExecutor executor = PentahoSystem.get( IPentahoAsyncExecutor.class );
    final IAsyncReportExecution mock = mock( IAsyncReportExecution.class );

    executor.addTask( mock, PentahoSessionHolder.getSession() );
    final WebClient client = provider.getFreshClient();
    final String config = String.format( URL_FORMAT, "hohoho", "/content" );
    client.path( config );
    final Response response = client.post( null );
    assertEquals( 404, response.getStatus() );

  }

  @Test public void testAutoScheduling() throws InterruptedException, ExecutionException {


    final List<Integer[]> objects = Arrays.asList( new Integer[][] {
      { 0, 0, 0 },
      { 0, Integer.MAX_VALUE, 0 },
      { 0, -1, 0 },
      { 1, 0, 0 },
      { 1, Integer.MAX_VALUE, 1 },
      { 1, -1, 0 }
    } );


    for ( final Integer[] params : objects ) {

      final PentahoAsyncExecutor exec = spy( new PentahoAsyncExecutor( 1, params[ 0 ] ) {
        @Override public void schedule( final UUID id, final IPentahoSession session ) {
        }
      } );

      ReportProgressEvent event = null;
      if ( params[ 1 ] >= 0 ) {
        event = mock( ReportProgressEvent.class );
        when( event.getMaximumRow() ).thenReturn( params[ 1 ] );
      }

      final ReportProgressEvent finalEvent = event;

      final UUID id =
        exec.addTask( new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
          AuditWrapper.NULL ) {
          @Override
          protected AsyncReportStatusListener createListener( final UUID id,
                                                              final List<? extends ReportProgressListener>
                                                                callbackListener ) {
            return new AsyncReportStatusListener( "display_path", id, "text/html", callbackListener );
          }

          @Override public IFixedSizeStreamingContent call() throws Exception {
            getListener().reportProcessingStarted( finalEvent );
            getListener().reportProcessingUpdate( finalEvent );
            getListener().reportProcessingFinished( finalEvent );
            verify( exec, times( params[ 2 ] ) ).schedule( any(), any() );
            return null;
          }
        }, session );
    }

  }

  private PentahoAsyncReportExecution mockExec() {
    return new PentahoAsyncReportExecution( "junit-path", component, handler, session, "not null",
      AuditWrapper.NULL ) {
      @Override
      protected AsyncReportStatusListener createListener( UUID id,
                                                          List<? extends ReportProgressListener> callbackListener ) {
        return new AsyncReportStatusListener( "display_path", id, "text/html", callbackListener );
      }

      @Override public IAsyncReportState getState() {
        return AsyncIT.this.getState();
      }

      @Override public boolean schedule() {
        STATUS = AsyncExecutionStatus.SCHEDULED;
        return true;
      }
    };
  }

  private static IAsyncReportState getState() {
    return new IAsyncReportState() {

      @Override public String getPath() {
        return PATH;
      }

      @Override public UUID getUuid() {
        return UUID.randomUUID();
      }

      @Override public AsyncExecutionStatus getStatus() {
        return STATUS;
      }

      @Override public int getProgress() {
        return PROGRESS;
      }

      @Override public int getPage() {
        return PAGE;
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

    };
  }

}
