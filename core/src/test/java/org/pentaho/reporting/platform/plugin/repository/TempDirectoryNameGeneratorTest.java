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
