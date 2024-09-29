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

import org.junit.Test;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.reporting.libraries.repository.DefaultMimeRegistry;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportContentRepositoryTest {
  @Test
  public void getRoot() throws Exception {

    final RepositoryFile mock = mock( RepositoryFile.class );
    when( mock.getName() ).thenReturn( UUID.randomUUID().toString() );
    final ReportContentRepository reportContentRepository = new ReportContentRepository( mock );
    assertEquals( mock.getName(), reportContentRepository.getRoot().getName() );

  }

  @Test
  public void getMimeRegistry() throws Exception {
    final RepositoryFile mock = mock( RepositoryFile.class );
    final ReportContentRepository reportContentRepository = new ReportContentRepository( mock );
    assertTrue( reportContentRepository.getMimeRegistry() instanceof DefaultMimeRegistry );
  }

}
