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
 * Copyright (c) 2002-2017 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.repository;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.libraries.repository.ContentCreationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.mockito.Matchers;
import org.mockito.Mockito;

public class ReportContentLocationTest {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;
  IUnifiedRepository repository;

  @Before
  public void setUp() throws Exception {
    PentahoSystem.shutdown();
    repositoryFile = Mockito.mock( RepositoryFile.class );
    Mockito.doReturn( "contentId" ).when( repositoryFile ).getId();
    Mockito.doReturn( "contentName" ).when( repositoryFile ).getName();
    Mockito.doReturn( "version" ).when( repositoryFile ).getVersionId();
    reportContentRepository = Mockito.mock( ReportContentRepository.class );
    reportContentLocation = new ReportContentLocation( repositoryFile, reportContentRepository );
    repository = Mockito.mock( IUnifiedRepository.class );
    PentahoSystem.registerObject( repository, IUnifiedRepository.class );
  }

  @Test
  public void testIsHiddenExtension() throws Exception {
    Assert.assertTrue( reportContentLocation.isHiddenExtension( ".jpe" ) );
    Assert.assertTrue( reportContentLocation.isHiddenExtension( ".jpeg" ) );
    Assert.assertTrue( reportContentLocation.isHiddenExtension( ".jpg" ) );
    Assert.assertTrue( reportContentLocation.isHiddenExtension( ".png" ) );
    Assert.assertTrue( reportContentLocation.isHiddenExtension( ".css" ) );
    Assert.assertFalse( reportContentLocation.isHiddenExtension( "" ) );
  }

  @Test
  public void testDelete() throws Exception {
    Assert.assertFalse( reportContentLocation.delete() );
  }

  @Test
  public void testGetParent() throws Exception {
    Assert.assertNull( reportContentLocation.getParent() );
  }

  @Test
  public void testGetRepository() throws Exception {
    Assert.assertEquals( reportContentRepository, reportContentLocation.getRepository() );
  }

  @Test
  public void testSetAttribute() throws Exception {
    Assert.assertFalse( reportContentLocation.setAttribute( "", "", null ) );
  }

  @Test
  public void testCreateLocation() throws Exception {
    try {
      reportContentLocation.createLocation( "" );
    } catch ( ContentCreationException ex ) {
      Assert.assertTrue( true );
    }
  }

  @Test
  public void testGetName() throws Exception {
    Assert.assertEquals( "contentName", reportContentLocation.getName() );
  }

  @Test
  public void testGetContentId() throws Exception {
    Assert.assertEquals( "contentId", reportContentLocation.getContentId() );
  }

  @Test
  public void testGetAttribute() throws Exception {
    Assert.assertEquals( null, reportContentLocation.getAttribute( "", "" ) );
    Assert.assertEquals( null, reportContentLocation.getAttribute( "org.jfree.repository", "" ) );
    Assert.assertEquals( null, reportContentLocation.getAttribute( "", "version" ) );
    Assert.assertEquals( "version", reportContentLocation.getAttribute( "org.jfree.repository", "version" ) );
  }

  @Test( expected = NullPointerException.class )
  public void testNullRepo() {
    new ReportContentLocation( Mockito.mock( RepositoryFile.class ), null );
  }

  @Test( expected = NullPointerException.class )
  public void testNullLocation() {
    new ReportContentLocation( null, Mockito.mock( ReportContentRepository.class ) );
  }


  @Test
  public void testList() throws ContentIOException {


    final ArrayList<RepositoryFile> repositoryFiles = new ArrayList<>();
    final RepositoryFile repositoryFile = Mockito.mock( RepositoryFile.class );
    final String value = UUID.randomUUID().toString();
    Mockito.when( repositoryFile.getName() ).thenReturn( value );
    repositoryFiles.add( repositoryFile );
    Mockito.when( repository.getChildren( Matchers.any( Serializable.class ) ) ).thenReturn( repositoryFiles );

    final ReportContentLocation reportContentLocation =
            new ReportContentLocation( this.repositoryFile, reportContentRepository );

    final ContentEntity[] contentEntities = reportContentLocation.listContents();

    Assert.assertNotNull( contentEntities );
    Assert.assertEquals( 1, contentEntities.length );
    Assert.assertEquals( value, contentEntities[ 0 ].getName() );
  }


  @Test( expected = ContentIOException.class )
  public void testGetEntryNotExist() throws ContentIOException {

    final ReportContentLocation reportContentLocation =
            new ReportContentLocation( this.repositoryFile, reportContentRepository );

    reportContentLocation.getEntry( "test" );
  }

  @Test
  public void testGetEntry() throws ContentIOException {


    final RepositoryFile repositoryFile = Mockito.mock( RepositoryFile.class );
    final String value = UUID.randomUUID().toString();
    Mockito.when( repositoryFile.getName() ).thenReturn( value );

    Mockito.when( repository.getFile( "null/test" ) ).thenReturn( repositoryFile );

    final ReportContentLocation reportContentLocation =
            new ReportContentLocation( this.repositoryFile, reportContentRepository );

    final ContentEntity test = reportContentLocation.getEntry( "test" );

    Assert.assertNotNull( test );
    Assert.assertEquals( value, test.getName() );

  }

  @Test
  public void testCreateItem() throws Exception {
    final HashMap<String, Serializable> metadata = new HashMap<>();
    Mockito.when( repository.getFile( Matchers.anyString() ) ).thenReturn( repositoryFile );
    Mockito.when( repository.getFileMetadata( Matchers.any( Class.class ) ) ).thenReturn( metadata );
    Mockito.when( repositoryFile.getPath() ).thenReturn( "/testPath" );
    reportContentLocation.createItem( "testName" );
    Assert.assertTrue( repository.getFileMetadata( repositoryFile.getId() ).containsKey( ReportContentLocation.RESERVEDMAPKEY_LINEAGE_ID ) );
  }

}

