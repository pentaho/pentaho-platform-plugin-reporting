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

import static org.mockito.Mockito.mock;

public class ReportContentLocationTest extends TestCase {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;

  protected void setUp() {
    repositoryFile = mock( RepositoryFile.class );
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
}
