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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.libraries.base.util.ArgumentNullException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

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
    verify( repository, times( 1 ) ).moveFile( any(), any(), any() );


  }


}
