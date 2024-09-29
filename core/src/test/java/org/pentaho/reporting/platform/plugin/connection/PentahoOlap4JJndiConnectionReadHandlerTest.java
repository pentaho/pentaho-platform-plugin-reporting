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


package org.pentaho.reporting.platform.plugin.connection;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;

import static org.mockito.Mockito.mock;

public class PentahoOlap4JJndiConnectionReadHandlerTest extends TestCase {
  PentahoOlap4JJndiConnectionReadHandler handler;

  @Before
  public void setUp() throws Exception {
    handler = new PentahoOlap4JJndiConnectionReadHandler();
  }

  @Test
  public void testGetHandlerForChild() throws Exception {
    Attributes attrs = mock( Attributes.class );
    assertNull( handler.getHandlerForChild( "uri", "tagName", attrs ) );
    assertNotNull( handler.getHandlerForChild( null, "path", attrs ) );
    assertNull( handler.getHandlerForChild( null, null, attrs ) );
  }
}
