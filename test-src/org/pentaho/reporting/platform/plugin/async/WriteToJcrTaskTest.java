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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith( Parameterized.class )
public class WriteToJcrTaskTest {

  private String mime;
  private String file;

  public WriteToJcrTaskTest( final String mime, final String file ) {
    this.mime = mime;
    this.file = file;
  }

  @Parameterized.Parameters
  public static Collection primeNumbers() {
    return Arrays.asList( new Object[][] {
      { "application/pdf", "report.prpt" },
      { "toinfinitynadbeyond", "" }
    } );
  }


  private static MicroPlatform microPlatform;

  @BeforeClass
  public static void setUp() throws PlatformInitializationException {
    new File( "./resource/solution/system/tmp" ).mkdirs();
    microPlatform = MicroPlatformFactory.create();
    final IUnifiedRepository repository = mock( IUnifiedRepository.class );
    final ISchedulingDirectoryStrategy strategy = mock( ISchedulingDirectoryStrategy.class );
    final RepositoryFile file = mock( RepositoryFile.class );
    when( strategy.getSchedulingDir( repository ) ).thenReturn( file );
    when( repository.getFile( matches( "^[a-z/\\\\]+\\.{1}[a-z]+$" ) ) ).thenReturn( file );
    microPlatform.defineInstance( "IUnifiedRepository", repository );
    microPlatform.defineInstance( "ISchedulingDirectoryStrategy", strategy );
    microPlatform.start();
  }

  @AfterClass
  public static void tearDown() {
    microPlatform.stop();
  }


  @Test
  public void call() throws Exception {


    final PentahoAsyncExecutor pentahoAsyncExecutor = new PentahoAsyncExecutor( 10 );
    final IFixedSizeStreamingContent content = mock( IFixedSizeStreamingContent.class );
    final IAsyncReportExecution reportExecution = mock( IAsyncReportExecution.class );
    final IAsyncReportState state = mock( IAsyncReportState.class );
    when( state.getMimeType() ).thenReturn( mime );
    when( state.getPath() ).thenReturn( file );
    when( reportExecution.getState() ).thenReturn( state );
    final InputStream stream = mock( InputStream.class );
    when( content.getStream() ).thenReturn( stream );
    final StandaloneSession session = new StandaloneSession( "test" );
    PentahoSessionHolder.setSession( session );
    final Callable writeTask =
      pentahoAsyncExecutor.getWriteTask( content, UUID.randomUUID(), session, reportExecution );
    writeTask.call();


  }


}
