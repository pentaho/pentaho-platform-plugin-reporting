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
