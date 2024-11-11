/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.async;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UpdateSchedulingLocationListenerTest {

  private static MicroPlatform microPlatform;
  private IUnifiedRepository repository;

  @Before
  public void setUp() throws PlatformInitializationException {
    microPlatform = MicroPlatformFactory.create();
    repository = mock( IUnifiedRepository.class );
    final RepositoryFile file = mock( RepositoryFile.class );
    when( file.isFolder() ).thenReturn( false );
    when( file.getName() ).thenReturn( "birdy" );
    when( repository.getFileById( "test.prpt" ) ).thenReturn( file );
    final RepositoryFile folder = mock( RepositoryFile.class );
    when( folder.isFolder() ).thenReturn( true );
    when( repository.getFileById( "/test" ) ).thenReturn( folder );
    when( repository.getFileById( "null" ) ).thenReturn( null );

    microPlatform.defineInstance( "IUnifiedRepository", repository );

    microPlatform.start();
  }

  @After
  public void tearDown() {
    microPlatform.stop();
    microPlatform = null;
  }


  @Test( expected = ArgumentNullException.class )
  public void testNullPath() {
    new UpdateSchedulingLocationListener( null, "test" );
  }

  @Test( expected = ArgumentNullException.class )
  public void testNullNewName() {
    new UpdateSchedulingLocationListener( "test", null );
  }


  @Test
  public void onSchedulingCompleted() throws Exception {


    final UpdateSchedulingLocationListener listener =
      new UpdateSchedulingLocationListener( "/notexists", "test" );

    //no report
    listener.onSchedulingCompleted( "notexists.prpt" );
    listener.onSchedulingCompleted( "null" );
    verify( repository, times( 4 ) ).getFileById( any() );
    verify( repository, times( 0 ) ).moveFile( any(), any(), any() );

    final UpdateSchedulingLocationListener listener3 =
      new UpdateSchedulingLocationListener( "null", "test" );
    listener3.onSchedulingCompleted( "notexists.prpt" );
    listener3.onSchedulingCompleted( "null" );
    listener3.onSchedulingCompleted( "test.prpt" );
    verify( repository, times( 0 ) ).moveFile( any(), any(), any() );

    final UpdateSchedulingLocationListener listener2 =
      new UpdateSchedulingLocationListener( "/test", "test" );
    //Valid key but dir not file
    listener2.onSchedulingCompleted( "/test" );
    listener2.onSchedulingCompleted( "null" );
    verify( repository, times( 0 ) ).moveFile( any(), any(), any() );
    //Success
    listener2.onSchedulingCompleted( "test.prpt" );
    //Temp with uuid  + valid location
    verify( repository, times( 2 ) ).moveFile( any(), any(), any() );


  }

  @Test
  public void testUuidClash() throws Exception {
    final int[] counter = { 0 };
    final RepositoryFile mock = mock( RepositoryFile.class );
    when( repository.getFile( "null/testtest" ) ).thenReturn( mock );
    final UpdateSchedulingLocationListener updateSchedulingLocationListener =
      new UpdateSchedulingLocationListener( "/test", "test" ) {
        @Override protected String getUuidAsString() {
          if ( counter[ 0 ] == 0 ) {
            counter[ 0 ]++;
            return "test";
          } else {
            counter[ 0 ]++;
            return super.getUuidAsString();
          }
        }
      };
    updateSchedulingLocationListener.onSchedulingCompleted( "test.prpt" );
    assertEquals( 2, counter[ 0 ] );

  }

  @Test
  public void testNameClash() throws Exception {
    final RepositoryFile mock = mock( RepositoryFile.class );
    when( repository.getFile( "null/1" ) ).thenReturn( mock );
    final UpdateSchedulingLocationListener updateSchedulingLocationListener =
      new UpdateSchedulingLocationListener( "/test", "1" );
    updateSchedulingLocationListener.onSchedulingCompleted( "test.prpt" );
    verify( repository ).moveFile( null, "null/1(1)", "Moved to the location selected by user" );
  }
}
