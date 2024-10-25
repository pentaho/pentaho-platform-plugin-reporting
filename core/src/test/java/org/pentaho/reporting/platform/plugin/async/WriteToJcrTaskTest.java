/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.input.NullInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.engine.classic.core.event.async.IAsyncReportState;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class WriteToJcrTaskTest {

  private static MicroPlatform microPlatform;

  @Before
  public void setUp() throws PlatformInitializationException {
    microPlatform = MicroPlatformFactory.create();
    final IUnifiedRepository repository = mock( IUnifiedRepository.class );
    final ISchedulingDirectoryStrategy strategy = mock( ISchedulingDirectoryStrategy.class );
    final RepositoryFile targetDir = mock( RepositoryFile.class );
    when( strategy.getSchedulingDir( repository ) ).thenReturn( targetDir );
    when( targetDir.getPath() ).thenReturn( "/test" );
    microPlatform.defineInstance( "IUnifiedRepository", repository );
    microPlatform.defineInstance( "ISchedulingDirectoryStrategy", strategy );
    microPlatform.start();
  }

  @After
  public void tearDown() {
    microPlatform.stop();
  }

  @Ignore
  @Test
  public void testPositiveScenario() throws Exception {
    final UUID uuid = UUID.randomUUID();
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getId() ).thenReturn( uuid.toString() );
    when( PentahoSystem.get( IUnifiedRepository.class ).getFile( "/test/report.pdf" ) ).thenReturn(
      file );
    final FakeLocation fakeLocation = new FakeLocation();
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( "application/pdf" );
    when( state.getPath() ).thenReturn( "report.prpt" );
    when( reportExecution.getState() ).thenReturn( state );
    final NullInputStream inputStream = new NullInputStream( 100 );
    when( content.getStream() ).thenReturn( inputStream );
    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
    when( contentRepository.getRoot() ).thenReturn( fakeLocation );
    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, inputStream ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };
    assertNotNull( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "report.pdf" ) );
  }

  @Ignore
  @Test
  public void testNoNames() throws Exception {
    final UUID uuid = UUID.randomUUID();
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getId() ).thenReturn( uuid.toString() );
    when( PentahoSystem.get( IUnifiedRepository.class ).getFile( "/test/content.txt" ) ).thenReturn(
      file );
    final FakeLocation fakeLocation = new FakeLocation();
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( null );
    when( state.getPath() ).thenReturn( "/" );
    when( reportExecution.getState() ).thenReturn( state );
    final NullInputStream inputStream = new NullInputStream( 100 );
    when( content.getStream() ).thenReturn( inputStream );
    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
    when( contentRepository.getRoot() ).thenReturn( fakeLocation );
    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, inputStream ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };

    assertEquals( uuid.toString(), toJcrTask.call() );
    assertTrue( fakeLocation.exists( "content.txt" ) );
  }

  @Ignore
  @Test
  public void testAlreadyExists() throws Exception {
    final UUID uuid = UUID.randomUUID();
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getId() ).thenReturn( uuid.toString() );
    when( PentahoSystem.get( IUnifiedRepository.class ).getFile( "/test/report.pdf" ) ).thenReturn(
      file );
    when( PentahoSystem.get( IUnifiedRepository.class ).getFile( "/test/report(1).pdf" ) ).thenReturn(
      file );
    final FakeLocation fakeLocation = new FakeLocation();
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( "application/pdf" );
    when( state.getPath() ).thenReturn( "report.prpt" );
    when( reportExecution.getState() ).thenReturn( state );
    final NullInputStream inputStream = new NullInputStream( 100 );
    when( content.getStream() ).thenReturn( inputStream );
    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
    when( contentRepository.getRoot() ).thenReturn( fakeLocation );
    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, inputStream ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };
    assertNotNull( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "report.pdf" ) );
    assertNotNull( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "report.pdf" ) );
    assertTrue( fakeLocation.exists( "report(1).pdf" ) );
  }

  @Test
  public void testFail() throws Exception {
    final FakeLocation fakeLocation = new FakeLocation();
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( "application/pdf" );
    when( state.getPath() ).thenReturn( "report.prpt" );
    when( reportExecution.getState() ).thenReturn( state );
    final InputStream inputStream = mock( InputStream.class );
    when( inputStream.read( any() ) ).thenThrow( new IOException( "Test" ) );
    when( content.getStream() ).thenReturn( inputStream );
    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
    when( contentRepository.getRoot() ).thenReturn( fakeLocation );
    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, inputStream ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };
    assertNull( toJcrTask.call() );
  }


  @Ignore
  @Test
  public void testConcurrentSave() throws Exception {

    final UUID uuid = UUID.randomUUID();
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.getId() ).thenReturn( uuid.toString() );
    when( PentahoSystem.get( IUnifiedRepository.class ).getFile( startsWith( "/test" ) ) ).thenReturn(
      file );

    final CountDownLatch latch1 = new CountDownLatch( 1 );

    final FakeLocation fakeLocation = new FakeLocation( latch1 );


    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( "application/pdf" );
    when( state.getPath() ).thenReturn( "report.prpt" );
    when( reportExecution.getState() ).thenReturn( state );

    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );

    when( contentRepository.getRoot() ).thenReturn( fakeLocation );


    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, new InputStream() {
      @Override public int read() throws IOException {
        try {
          Thread.sleep( 100 );
        } catch ( final InterruptedException e ) {
          e.printStackTrace();
        }
        return -1;
      }
    } ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };

    final ExecutorService executorService = Executors.newFixedThreadPool( 10 );

    final List<Future<Serializable>> results = new ArrayList<>();

    for ( int i = 0; i < 10; i++ ) {
      results.add( executorService.submit( toJcrTask ) );
    }


    latch1.countDown();

    for ( final Future<Serializable> res : results ) {
      assertNotNull( res.get() );
    }


  }

  @Test
  public void testNullStream() throws Exception {

    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( "application/pdf" );
    when( state.getPath() ).thenReturn( "report.prpt" );
    when( reportExecution.getState() ).thenReturn( state );
    final InputStream inputStream = mock( InputStream.class );
    when( inputStream.read( any() ) ).thenThrow( new IOException( "Test" ) );
    when( content.getStream() ).thenReturn( inputStream );
    final ReportContentRepository contentRepository = mock( ReportContentRepository.class );
    final ContentLocation contentLocation = mock( ContentLocation.class );
    final ContentItem contentItem = mock( ContentItem.class );
    when( contentItem.getOutputStream() ).thenReturn( null );
    when( contentLocation.createItem( any() ) ).thenReturn( contentItem );
    when( contentRepository.getRoot() ).thenReturn( contentLocation );
    final WriteToJcrTask toJcrTask = new WriteToJcrTask( reportExecution, inputStream ) {
      @Override protected ReportContentRepository getReportContentRepository( final RepositoryFile outputFolder ) {
        return contentRepository;
      }
    };
    assertNull( toJcrTask.call() );
  }


}
