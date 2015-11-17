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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentLocationIT extends TestCase {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;
  private MicroPlatform microPlatform;

  @Before
  protected void setUp() throws Exception {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "test" ).when( repositoryFile ).getId();
    doReturn( "" ).when( repositoryFile ).getId();
    doReturn( "./resource" ).when( repositoryFile ).getPath();

    reportContentRepository = mock( ReportContentRepository.class );
    reportContentLocation = new ReportContentLocation( repositoryFile, reportContentRepository );

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @After
  public void tearDown() throws Exception {
    microPlatform.stop();
  }

  @Test
  public void testGetEntry() throws Exception {
    try {
      reportContentLocation.getEntry( "test" );
    } catch ( ContentIOException ex ) {
      assertTrue( true );
    }

    ContentEntity entity = reportContentLocation.getEntry( "report.html" );
    assertNotNull( entity );
    assertEquals( "text/html", ( (ReportContentItem) entity ).getMimeType() );
  }

  @Test
  public void testCreateItem() throws Exception {
    ContentItem item = reportContentLocation.createItem( "report.html" );
    assertEquals( "text/html", item.getMimeType() );
  }

  @Test
  public void testExists() throws Exception {
    assertTrue( reportContentLocation.exists( "report.html" ) );
  }
}
