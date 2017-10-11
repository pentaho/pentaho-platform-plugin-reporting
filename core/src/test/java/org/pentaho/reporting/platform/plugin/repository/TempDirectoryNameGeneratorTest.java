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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.repository;

import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.MimeRegistry;
import org.pentaho.reporting.libraries.repository.Repository;
import org.pentaho.reporting.libraries.repository.file.FileContentLocation;

import java.io.File;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class TempDirectoryNameGeneratorTest extends TestCase {
  TempDirectoryNameGenerator generator;
  String tmpFileName;
  final String directory = "../../../target/test/resource/solution/test/tmp";

  @Before
  public void setUp() throws Exception {
    generator = new TempDirectoryNameGenerator();
  }

  @After
  public void tearDown() {
    if ( tmpFileName != null && !tmpFileName.isEmpty() ) {
      File tmpFile = new File( directory + "/" + tmpFileName );
      tmpFile.delete();
    }
  }

  @Test
  public void testGenerateNameWithNoDirectory() throws Exception {
    FileContentLocation fileLocation = mock( FileContentLocation.class );
    Repository repository = mock( Repository.class );
    MimeRegistry mimeRegistry = mock( MimeRegistry.class );
    doReturn( mimeRegistry ).when( repository ).getMimeRegistry();
    doReturn( repository ).when( fileLocation ).getRepository();

    File file = mock( File.class );
    doReturn( file ).when( fileLocation ).getContentId();
    doReturn( false ).when( file ).isDirectory();

    try {
      generator.initialize( fileLocation, true );
    } catch ( NullPointerException ex ) {
      assertTrue( true );
    }
  }

  @Test
  public void testGenerateNameWithDefaultNameGenerator() throws Exception {
    Repository repository = mock( Repository.class );
    MimeRegistry mimeRegistry = mock( MimeRegistry.class );
    ContentLocation location = mock( ContentLocation.class );
    doReturn( mimeRegistry ).when( repository ).getMimeRegistry();
    doReturn( repository ).when( location ).getRepository();

    generator.initialize( location, true );
    String name = generator.generateName( "hint", "" );

    assertEquals( "hint.null", name );
  }
}
