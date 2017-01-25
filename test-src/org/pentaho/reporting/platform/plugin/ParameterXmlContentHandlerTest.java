/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2017 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.ResourceBundleFactory;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.resourceloader.ParameterKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.w3c.dom.Document;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * see backlog-7980
 * <p>
 * Created by dima.prokopenko@gmail.com on 6/27/2016.
 */
public class ParameterXmlContentHandlerTest {

  private static XPathFactory xpathFactory = XPathFactory.newInstance();
  private ParameterXmlContentHandler handler;
  private MasterReport report;
  private ReportParameterDefinition definition;
  private CompoundDataFactory factory;

  @BeforeClass
  public static void beforeClass() {
    ClassicEngineBoot.getInstance().start();
  }

  // helper method
  private String toString( final Document doc ) {
    try {
      final StringWriter stringWriter = new StringWriter();
      final TransformerFactory factory = TransformerFactory.newInstance();
      final Transformer transformer = factory.newTransformer();
      transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
      transformer.setOutputProperty( OutputKeys.METHOD, "xml" );
      transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
      transformer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );

      transformer.transform( new DOMSource( doc ), new StreamResult( stringWriter ) );
      return stringWriter.toString();
    } catch ( final Exception ex ) {
      // no op
      return "fail";
    }
  }

  @Before
  public void before() {
    final ParameterContentGenerator generator = mock( ParameterContentGenerator.class );
    handler = new ParameterXmlContentHandler( generator, true );

    report = mock( MasterReport.class );

    final Configuration conf = mock( Configuration.class );

    final ResourceManager rmanager = new ResourceManager();
    final ResourceKey rkey = new ResourceKey( "", "", Collections.<ParameterKey, Object>emptyMap() );
    final ResourceBundleFactory resBFactory = mock( ResourceBundleFactory.class );
    final ReportEnvironment environment = mock( ReportEnvironment.class );

    when( report.getConfiguration() ).thenReturn( conf );
    when( report.getResourceManager() ).thenReturn( rmanager );
    when( report.getContentBase() ).thenReturn( rkey );
    when( report.getResourceBundleFactory() ).thenReturn( resBFactory );
    when( report.getReportEnvironment() ).thenReturn( environment );

    definition = mock( ReportParameterDefinition.class );
    when( report.getParameterDefinition() ).thenReturn( definition );

    factory = mock( CompoundDataFactory.class );
    // preparation of data factory to fetch parameter names
    // the actual factory for query will be returned when 'getDataFactoryForQuery' called.
    when( factory.isNormalized() ).thenReturn( true );
    when( factory.derive() ).thenReturn( factory );
    when( report.getDataFactory() ).thenReturn( factory );
  }

  @Test
  public void testGetSelections() throws ReportDataFactoryException, BeanException {
    final Map<String, ?> inputs = Collections.singletonMap( "name", "value" );

    ParameterDefinitionEntry rp =
      new DefaultListParameter( "query", "keyColumn", "textColumn", "name", false, true, String.class );


    rp = new DefaultListParameter( "query", "keyColumn", "textColumn", "name", false, false, String.class );
    Object result = handler.getSelections( rp, inputs );
    assertEquals( null, result );
  }

  @Test
  public void testGetSelectionsPlain() throws ReportDataFactoryException, BeanException {
    final Map<String, String> inputs = Collections.singletonMap( "name", "value" );

    ParameterDefinitionEntry rp = new PlainParameter( "name", String.class );

    rp = new PlainParameter( "name", String.class );
    Object result = handler.getSelections( rp, inputs );
    assertEquals( "value", result );
  }


}
