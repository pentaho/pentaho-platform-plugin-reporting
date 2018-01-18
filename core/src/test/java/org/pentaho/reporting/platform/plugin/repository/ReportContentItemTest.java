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
 * Copyright (c) 2002-2018 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.repository;

import junit.framework.TestCase;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.Repository;

import java.io.OutputStream;
import java.util.Date;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentItemTest extends TestCase {
  ReportContentItem reportContentItem;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;
  Repository repository;

  protected void setUp() {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "fileName" ).when( repositoryFile ).getName();

    reportContentLocation = mock( ReportContentLocation.class );
    repository = mock( Repository.class );
    doReturn( repository ).when( reportContentLocation ).getRepository();

    reportContentItem = new ReportContentItem( repositoryFile, reportContentLocation, "" );
  }

  public void testSetMimeType() throws Exception {
    assertEquals( "", reportContentItem.getMimeType() );
    String newMimeType = "new-mime-type"; //$NON-NLS-1$
    reportContentItem.setMimeType( newMimeType );
    assertEquals( newMimeType, reportContentItem.getMimeType() );
  }

  public void testGetContentId() throws Exception {
    doReturn( "/home/admin/namewithcolon:/picture118632338.png" ).when( repositoryFile ).getPath();
    String expectedResult = "%3Ahome%3Aadmin%3Anamewithcolon%09%3Apicture118632338.png";

    assertEquals( expectedResult, reportContentItem.getContentId() );
  }

  public void testIsReadable() throws Exception {
    assertFalse( reportContentItem.isReadable() );
  }

  public void testIsWritable() throws Exception {
    assertTrue( reportContentItem.isWriteable() );
  }

  public void testGetParent() throws Exception {
    assertEquals( reportContentLocation, reportContentItem.getParent() );
  }

  public void testSetAttribute() throws Exception {
    assertFalse( reportContentItem.setAttribute( "", "", null ) );
  }

  public void testGetName() throws Exception {
    assertEquals( "fileName", reportContentItem.getName() );
  }

  public void testGetRepository() throws Exception {
    assertEquals( repository, reportContentItem.getRepository() );
  }

  public void testGetAttribute() throws Exception {
    assertNull( reportContentItem.getAttribute( "", "" ) );

    Object result = reportContentItem.getAttribute( "org.jfree.repository", "size" );
    assertEquals( "0", result.toString() );

    Date date = new Date();
    doReturn( date ).when( repositoryFile ).getLastModifiedDate();

    result = reportContentItem.getAttribute( "org.jfree.repository", "version" );
    assertEquals( date, result );
  }
}
