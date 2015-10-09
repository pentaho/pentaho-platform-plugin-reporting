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
import org.pentaho.platform.api.repository2.unified.RepositoryFile;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentItemTest extends TestCase {
  ReportContentItem reportContentItem;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;

  protected void setUp() {
    repositoryFile = mock( RepositoryFile.class );
    reportContentLocation = mock( ReportContentLocation.class );
    reportContentItem = new ReportContentItem( repositoryFile, reportContentLocation, "" );
  }

  public void testSetMimeType() throws Exception {
    assertEquals( "", reportContentItem.getMimeType() );
    String newMimeType = "new-mime-type"; //$NON-NLS-1$
    reportContentItem.setMimeType( newMimeType );
    assertEquals( newMimeType, reportContentItem.getMimeType() );
  }

  public void testGetContentId() throws Exception {
    doReturn( "c:\\Dev\\" ).when( repositoryFile ).getPath();
    String expectedResult = "c%3A%255CDev%255C";

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

}
