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

public class PentahoJndiConnectionReadHandlerTest extends TestCase {
  PentahoJndiConnectionReadHandler handler;

  @Before
  public void setUp() throws Exception {
    handler = new PentahoJndiConnectionReadHandler();
  }

  @Test
  public void testGetHandlerForChild() throws Exception {
    Attributes attrs = mock( Attributes.class );
    assertNull( handler.getHandlerForChild( "uri", "tagName", attrs ) );
    assertNotNull( handler.getHandlerForChild( null, "path", attrs ) );
    assertNotNull( handler.getHandlerForChild( null, "username", attrs ) );
    assertNotNull( handler.getHandlerForChild( null, "password", attrs ) );
    assertNull( handler.getHandlerForChild( null, null, attrs ) );
  }

  @Test
  public void testDoneParsing() throws Exception {
    Attributes attrs = mock( Attributes.class );

    assertNull( handler.getProvider() );

    handler.getHandlerForChild( null, "path", attrs );
    handler.doneParsing();
    assertNotNull( handler.getProvider() );
    assertNotNull( handler.getObject() );

    handler.getHandlerForChild( null, "username", attrs );
    handler.doneParsing();
    assertNotNull( handler.getProvider() );
    assertNotNull( handler.getObject() );

    handler.getHandlerForChild( null, "password", attrs );
    handler.doneParsing();
    assertNotNull( handler.getProvider() );
    assertNotNull( handler.getObject() );
  }
}
