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
/*
import org.apache.commons.io.input.NullInputStream;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.libraries.repository.ContentCreationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.Repository;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.repository.ReportContentRepository;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;*/


public class WriteToJcrTaskTest {
/*

  private static MicroPlatform microPlatform;

  @Before
  public void setUp() throws PlatformInitializationException {
    microPlatform = MicroPlatformFactory.create();
    final IUnifiedRepository repository = mock( IUnifiedRepository.class );
    final ISchedulingDirectoryStrategy strategy = mock( ISchedulingDirectoryStrategy.class );
    microPlatform.defineInstance( "IUnifiedRepository", repository );
    microPlatform.defineInstance( "ISchedulingDirectoryStrategy", strategy );
    microPlatform.start();
  }

  @After
  public void tearDown() {
    microPlatform.stop();
  }


  @Test
  public void testPositiveScenario() throws Exception {

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
    assertTrue( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "report.pdf" ) );
  }

  @Test
  public void testNoNames() throws Exception {

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
    assertTrue( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "content.txt" ) );
  }


  @Test
  public void testAlreadyExists() throws Exception {

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
    assertTrue( toJcrTask.call() );
    assertTrue( fakeLocation.exists( "report.pdf" ) );
    assertTrue( toJcrTask.call() );
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
    assertFalse( toJcrTask.call() );
  }


  private class FakeLocation implements ContentLocation {

    private ConcurrentHashSet<String> files = new ConcurrentHashSet<>();

    @Override public ContentEntity[] listContents() throws ContentIOException {
      throw new UnsupportedOperationException();
    }

    @Override public ContentEntity getEntry( final String s ) throws ContentIOException {
      throw new UnsupportedOperationException();
    }

    @Override public ContentItem createItem( final String s ) throws ContentCreationException {
      if ( exists( s ) ) {
        throw new ContentCreationException();
      } else {
        files.add( s );
      }
      final ContentItem mock = mock( ContentItem.class );
      try {
        when( mock.getOutputStream() ).thenReturn( new org.apache.commons.io.output.NullOutputStream() );
      } catch ( ContentIOException | IOException e ) {
        e.printStackTrace();
      }
      return mock;
    }


    @Override public ContentLocation createLocation( final String s ) throws ContentCreationException {
      throw new UnsupportedOperationException();
    }

    @Override public boolean exists( final String s ) {
      return files.contains( s );
    }

    @Override public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override public Object getContentId() {
      throw new UnsupportedOperationException();
    }

    @Override public Object getAttribute( final String s, final String s1 ) {
      return null;
    }

    @Override public boolean setAttribute( final String s, final String s1, final Object o ) {
      throw new UnsupportedOperationException();
    }

    @Override public ContentLocation getParent() {
      throw new UnsupportedOperationException();
    }

    @Override public Repository getRepository() {
      throw new UnsupportedOperationException();
    }

    @Override public boolean delete() {
      throw new UnsupportedOperationException();
    }
  }

*/
}
