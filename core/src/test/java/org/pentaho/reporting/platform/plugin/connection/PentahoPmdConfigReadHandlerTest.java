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
