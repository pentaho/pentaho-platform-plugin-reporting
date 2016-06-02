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

package org.pentaho.reporting.platform.plugin.repository;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.ContentCreationException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentLocationTest {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;

  @Before
  public void setUp() throws Exception {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "contentId" ).when( repositoryFile ).getId();
    doReturn( "contentName" ).when( repositoryFile ).getName();
    doReturn( "version" ).when( repositoryFile ).getVersionId();
    reportContentRepository = mock( ReportContentRepository.class );
    reportContentLocation = new ReportContentLocation( repositoryFile, reportContentRepository );
  }

  @Test
  public void testIsHiddenExtension() throws Exception {
    assertTrue( reportContentLocation.isHiddenExtension( ".jpe" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".jpeg" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".jpg" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".png" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".css" ) );
    assertFalse( reportContentLocation.isHiddenExtension( "" ) );
  }

  @Test
  public void testDelete() throws Exception {
    assertFalse( reportContentLocation.delete() );
  }

  @Test
  public void testGetParent() throws Exception {
    assertNull( reportContentLocation.getParent() );
  }

  @Test
  public void testGetRepository() throws Exception {
    assertEquals( reportContentRepository, reportContentLocation.getRepository() );
  }

  @Test
  public void testSetAttribute() throws Exception {
    assertFalse( reportContentLocation.setAttribute( "", "", null ) );
  }

  @Test
  public void testCreateLocation() throws Exception {
    try {
      reportContentLocation.createLocation( "" );
    } catch ( ContentCreationException ex ) {
      assertTrue( true );
    }
  }

  @Test
  public void testGetName() throws Exception {
    assertEquals( "contentName", reportContentLocation.getName() );
  }

  @Test
  public void testGetContentId() throws Exception {
    assertEquals( "contentId", reportContentLocation.getContentId() );
  }

  @Test
  public void testGetAttribute() throws Exception {
    assertEquals( null, reportContentLocation.getAttribute( "", "" ) );
    assertEquals( null, reportContentLocation.getAttribute( "org.jfree.repository", "" ) );
    assertEquals( null, reportContentLocation.getAttribute( "", "version" ) );
    assertEquals( "version", reportContentLocation.getAttribute( "org.jfree.repository", "version" ) );
  }

  @Test( expected = NullPointerException.class )
  public void testNullRepo() {
    new ReportContentLocation( mock( RepositoryFile.class ), null );
  }

  @Test( expected = NullPointerException.class )
  public void testNullLocation() {
    new ReportContentLocation( null, mock( ReportContentRepository.class ) );
  }
}

