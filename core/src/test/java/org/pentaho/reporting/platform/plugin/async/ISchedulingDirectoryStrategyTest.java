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
 * Copyright 2006 - 2018 Hitachi Vantara.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.async;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

public class ISchedulingDirectoryStrategyTest {

  private static MicroPlatform microPlatform;

  @Before
  public void setUp() throws Exception {

    final IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @BeforeClass
  public static void init() throws PlatformInitializationException {
    new File( "target/test/resource/solution/system/tmp" ).mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    microPlatform.stop();
    microPlatform = null;
  }

  @Test
  public void testHomeStrategy() {
    final ISchedulingDirectoryStrategy home = new HomeSchedulingDirStrategy();
    final IUnifiedRepository mockRepo = mock( IUnifiedRepository.class );
    final RepositoryFile schedulingDir = home.getSchedulingDir( mockRepo );
    verify( mockRepo, times( 1 ) ).getFile( "/home/unknown" );
  }

  @Test
  public void testProvidedAbsolute() {

    final IUnifiedRepository mockRepo = mock( IUnifiedRepository.class );
    final RepositoryFile mockFolder = mock( RepositoryFile.class );
    when( mockFolder.isFolder() ).thenReturn( true );

    when( mockRepo.getFile( "/public" ) ).thenReturn( mockFolder );
    when( mockRepo.getFile( "/undefined" ) ).thenReturn( null );


    final ISchedulingDirectoryStrategy publicDir = new ProvidedSchedulingDirStrategy( "/public" );
    assertEquals( mockFolder, publicDir.getSchedulingDir( mockRepo ) );

    verify( mockRepo, times( 0 ) ).getFile( "/home/unknown" );
    verify( mockRepo, times( 1 ) ).getFile( "/public" );

    final ISchedulingDirectoryStrategy undefinedDir = new ProvidedSchedulingDirStrategy( "/undefined" );
    assertNull( undefinedDir.getSchedulingDir( mockRepo ) );

    verify( mockRepo, times( 1 ) ).getFile( "/home/unknown" );
    verify( mockRepo, times( 1 ) ).getFile( "/undefined" );
  }


  @Test
  public void testProvidedNotDir() {

    final IUnifiedRepository mockRepo = mock( IUnifiedRepository.class );
    final RepositoryFile mockFile = mock( RepositoryFile.class );

    when( mockRepo.getFile( "/test/1.html" ) ).thenReturn( mockFile );


    final ISchedulingDirectoryStrategy file = new ProvidedSchedulingDirStrategy( "/test/1.html" );
    assertNull( file.getSchedulingDir( mockRepo ) );

    verify( mockRepo, atLeastOnce() ).getFile( "/home/unknown" );
    verify( mockRepo, times( 1 ) ).getFile( "/test/1.html" );
  }


  @Test
  public void testProvidedRelative() {

    final IUnifiedRepository mockRepo = mock( IUnifiedRepository.class );
    final RepositoryFile mockFolder = mock( RepositoryFile.class );
    when( mockFolder.isFolder() ).thenReturn( true );
    when( mockFolder.getPath() ).thenReturn( "/home/unknown" );

    when( mockRepo.getFile( "/home/unknown" ) ).thenReturn( mockFolder );


    final ISchedulingDirectoryStrategy testDir = new ProvidedSchedulingDirStrategy( "test" );
    assertEquals( mockFolder, testDir.getSchedulingDir( mockRepo ) );

    verify( mockRepo, atLeastOnce() ).getFile( "/home/unknown" );
    verify( mockRepo, times( 1 ) ).getFile( "/home/unknown/test" );
  }


  @Test
  public void testNoSession() {
    PentahoSessionHolder.removeSession();
    final ISchedulingDirectoryStrategy home = new HomeSchedulingDirStrategy();
    assertNull( home.getSchedulingDir( mock( IUnifiedRepository.class ) ) );
  }
}
