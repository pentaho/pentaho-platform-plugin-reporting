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
   * verifies cases 
   * 
   * http://jira.pentaho.com/browse/PRD-3882
   * values containing illegal control chars are base64 encoded, and that the "encoded=true" attribute is
   * set as expected.
   * For example, <value encoded="true" label="Gg==" null="false" selected="false" type="java.lang.String" value="Gg=="/> 
   *
   * http://jira.pentaho.com/browse/PPP-3343
   * that Japanese values are not encoded
   * 
   * http://jira.pentaho.com/browse/BISERVER-11918
   * that special characters are not encoded 
   */
  public void testEncodedParameterValues() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final ParameterContentGenerator contentGenerator = new ParameterContentGenerator();
    final ParameterXmlContentHandler handler = new ParameterXmlContentHandler( contentGenerator, false );
    handler.createParameterContent( baos, "resource/solution/test/reporting/prd3882.prpt",
        "resource/solution/test/reporting/prd3882.prpt", false, null );

    Document doc =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse( new ByteArrayInputStream( baos.toByteArray() ) );

    String[] expectedVal = new String[] { "1qA", "+ / : ; = ? [ ] ^ \\", "果物" };
    //this label is shown user! you should be able to read items!
    String[] expectedLab = new String[] { "1qA", "+ / : ; = ? [ ] ^ \\", "果物" };
    String[] expectedEncoded = new String[] { null, null, null };
    for ( int i = 0; i < expectedVal.length; i++ ) {
      String value = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttribute( "value" );
      Node encoded = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttributeNode( "encoded" );
      String label = ( (Element) doc.getElementsByTagName( "value" ).item( i ) ).getAttribute( "label" );

      assertEquals( expectedVal[i], value );
      assertEquals( expectedLab[i], label );
      assertEquals( expectedEncoded[i], encoded == null ? encoded : encoded.getTextContent() );
    }
  }

}
