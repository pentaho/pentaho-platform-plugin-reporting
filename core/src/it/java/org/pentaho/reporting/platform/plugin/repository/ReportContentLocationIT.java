/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin.repository;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.core.system.boot.PlatformInitializationException;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentIOException;
import org.pentaho.reporting.libraries.repository.ContentItem;
import org.pentaho.reporting.platform.plugin.MicroPlatformFactory;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class ReportContentLocationIT {
  ReportContentRepository reportContentRepository;
  RepositoryFile repositoryFile;
  ReportContentLocation reportContentLocation;
  private static MicroPlatform microPlatform;

  @BeforeClass
  public static void init() throws PlatformInitializationException {
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
  }

  @Before
  public void setUp() throws Exception {
    repositoryFile = mock( RepositoryFile.class );
    doReturn( "test" ).when( repositoryFile ).getId();
    doReturn( "" ).when( repositoryFile ).getId();
    doReturn( "target/test/resource" ).when( repositoryFile ).getPath();

    reportContentRepository = mock( ReportContentRepository.class );
    reportContentLocation = new ReportContentLocation( repositoryFile, reportContentRepository );


    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession( session );
  }

  @AfterClass
  public static void tearDown() throws Exception {
    microPlatform.stop();
    microPlatform = null;
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
