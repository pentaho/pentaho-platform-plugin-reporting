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
import org.pentaho.reporting.libraries.xmlns.parser.RootXmlReadHandler;
import org.xml.sax.helpers.AttributesImpl;

import static org.mockito.Mockito.mock;

public class PentahoPmdConfigReadHandlerTest extends TestCase {
  PentahoPmdConfigReadHandler handler;

  protected void setUp() {
    handler = new PentahoPmdConfigReadHandler();
  }

  public void testStartParsing() throws Exception {
    AttributesImpl attrs = new AttributesImpl();
    attrs.addAttribute( "label-mapping", "label-mapping", "", "", "true" );
    attrs.addAttribute( "xmi-file", "xmi-file", "", "", "xmi-file" );
    attrs.addAttribute( "domain", "domain", "", "", "domain" );

    handler.init( mock( RootXmlReadHandler.class ), "label-mapping", "" );
    handler.startParsing( attrs );
    assertTrue( handler.getLabelMapping() );
    assertTrue( handler.isLabelMapping() );

    handler.init( mock( RootXmlReadHandler.class ), "xmi-file", "" );
    handler.startParsing( attrs );
    assertEquals( "xmi-file", handler.getXmiFile() );

    handler.init( mock( RootXmlReadHandler.class ), "domain", "" );
    handler.startParsing( attrs );
    assertEquals( "domain", handler.getDomain() );
  }

  public void testGetObject() throws Exception {
    assertNull( handler.getObject() );
  }
}
