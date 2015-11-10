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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.repository;

import junit.framework.TestCase;
import org.junit.Before;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.ContentCreationException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentLocationTest extends TestCase {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;

  @Before
  protected void setUp() throws Exception {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "contentId" ).when( repositoryFile ).getId();
    doReturn( "contentName" ).when( repositoryFile ).getName();
    doReturn( "version" ).when( repositoryFile ).getVersionId();
    reportContentRepository = mock( ReportContentRepository.class );
    reportContentLocation = new ReportContentLocation( repositoryFile, reportContentRepository );
  }

  public void testIsHiddenExtension() throws Exception {
    assertTrue( reportContentLocation.isHiddenExtension( ".jpe" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".jpeg" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".jpg" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".png" ) );
    assertTrue( reportContentLocation.isHiddenExtension( ".css" ) );
    assertFalse( reportContentLocation.isHiddenExtension( "" ) );
  }

  public void testDelete() throws Exception {
    assertFalse( reportContentLocation.delete() );
  }

  public void testGetParent() throws Exception {
    assertNull( reportContentLocation.getParent() );
  }

  public void testGetRepository() throws Exception {
    assertEquals( reportContentRepository, reportContentLocation.getRepository() );
  }

  public void testSetAttribute() throws Exception {
    assertFalse( reportContentLocation.setAttribute( "", "", null ) );
  }

  public void testCreateLocation() throws Exception {
    try {
      reportContentLocation.createLocation( "" );
    } catch ( ContentCreationException ex ) {
      assertTrue( true );
    }
  }

  public void testGetName() throws Exception {
    assertEquals( "contentName", reportContentLocation.getName() );
  }

  public void testGetContentId() throws Exception {
    assertEquals( "contentId", reportContentLocation.getContentId() );
  }

  public void testGetAttribute() throws Exception {
    assertEquals( null, reportContentLocation.getAttribute( "", "" ) );
    assertEquals( null, reportContentLocation.getAttribute( "org.jfree.repository", "" ) );
    assertEquals( null, reportContentLocation.getAttribute( "", "version" ) );
    assertEquals( "version", reportContentLocation.getAttribute( "org.jfree.repository", "version" ) );
  }
}
