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
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

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
