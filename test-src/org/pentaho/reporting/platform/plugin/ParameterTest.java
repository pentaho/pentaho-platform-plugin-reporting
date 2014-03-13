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
 * Copyright (c) 2002-2013 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.test.platform.engine.core.MicroPlatform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ParameterTest extends TestCase {
  private MicroPlatform microPlatform;

  public ParameterTest() {

  }

  @Override
  protected void setUp() throws Exception {
    new File( "./resource/solution/system/tmp" ).mkdirs();

    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();
    IPentahoSession session = new StandaloneSession( "test user" );
    PentahoSessionHolder.setSession( session );
  }

  @Override
  protected void tearDown() throws Exception {
    microPlatform.stop();
  }

  public void testParameterProcessing() throws Exception {
    final ParameterContentGenerator contentGenerator = new ParameterContentGenerator();
    final ParameterXmlContentHandler handler = new ParameterXmlContentHandler( contentGenerator, false );
    handler.createParameterContent( System.out, "resource/solution/test/reporting/Product Sales.prpt",
        "resource/solution/test/reporting/Product Sales.prpt", false, null );
  }

  /**
   * verifies that values containing illegal control chars are base64 encoded, and that the "encoded=true" attribute is
   * set as expected.
   * 
   * For example, <value encoded="true" label="Gg==" null="false" selected="false" type="java.lang.String" value="Gg=="
   * /> http://jira.pentaho.com/browse/PRD-3882
   */
  public void testEncodedParameterValues() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ParameterContentGenerator contentGenerator = new ParameterContentGenerator();
    final ParameterXmlContentHandler handler = new ParameterXmlContentHandler( contentGenerator, false );
    // this test report has 3 param values, one "good" and two "bad": { 1234, x001a, x001a }
    handler.createParameterContent( baos, "resource/solution/test/reporting/prd3882.prpt",
        "resource/solution/test/reporting/prd3882.prpt", false, null );

    Document doc =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse( new ByteArrayInputStream( baos.toByteArray() ) );

    NodeList list = doc.getElementsByTagName( "values" );
    doc.getElementsByTagName( "value" ).item( 0 ).getAttributes().item( 4 );

    String[] expectedVal = new String[] { "1234", "Gg==", "Gg==" };
    String[] expectedLab = new String[] { "1234", "Gg&#x3d;&#x3d;", "Gg&#x3d;&#x3d;" }; // Label is no longer necessarily the same as the value
    String[] expectedEncoded = new String[] { "", "true", "true" };
    for ( int i = 0; i < 3; i++ ) {
      String value = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttribute( "value" );
      Node encoded = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttributeNode( "encoded" );
      String label = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttribute( "label" );

      assertEquals( expectedVal[i], value );
      assertEquals( expectedLab[i], label );
      if ( i == 0 ) {
        // first value does not need to be encoded
        assertEquals( null, encoded );
      } else {
        assertEquals( expectedEncoded[i], encoded.getTextContent() );
      }
    }
  }

}
