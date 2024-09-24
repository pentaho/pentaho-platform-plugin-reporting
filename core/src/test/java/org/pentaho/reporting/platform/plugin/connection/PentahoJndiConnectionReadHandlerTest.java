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
