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

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.reporting.libraries.repository.ContentEntity;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.Repository;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class PentahoURLRewriterTest extends TestCase {
  PentahoURLRewriter rewriter;

  @Before
  public void setUp() {
    rewriter = new PentahoURLRewriter( null, false );
  }

  @Test
  public void testRewrite() throws Exception {
    ContentEntity contentEntity = mock( ContentEntity.class );
    ContentEntity dataEntity = mock( ContentEntity.class );

    Repository repository = mock( Repository.class );
    doReturn( repository ).when( dataEntity ).getRepository();

    String result = rewriter.rewrite( contentEntity, dataEntity );
    assertEquals( "null", result );

    ContentLocation location = mock( ContentLocation.class );
    doReturn( "parent" ).when( location ).getName();
    doReturn( location ).when( dataEntity ).getParent();
    doReturn( repository ).when( dataEntity ).getRepository();

    result = rewriter.rewrite( contentEntity, dataEntity );
    assertEquals( "parent/null", result );

    ContentLocation locationContent = mock( ContentLocation.class );
    doReturn( "parent" ).when( locationContent ).getName();
    doReturn( locationContent ).when( repository ).getRoot();
    doReturn( repository ).when( dataEntity ).getRepository();

    result = rewriter.rewrite( contentEntity, dataEntity );
    assertEquals( "null", result );

    ContentLocation locationParent = mock( ContentLocation.class );
    doReturn( "parent" ).when( locationParent ).getName();
    locationContent = mock( ContentLocation.class );
    doReturn( "content" ).when( locationContent ).getName();
    doReturn( locationContent ).when( repository ).getRoot();

    result = rewriter.rewrite( contentEntity, dataEntity );
    assertEquals( "parent/null", result );

    rewriter = new PentahoURLRewriter( "{0}", false );

    result = rewriter.rewrite( contentEntity, dataEntity );
    assertEquals( "parent/null", result );
  }
}
