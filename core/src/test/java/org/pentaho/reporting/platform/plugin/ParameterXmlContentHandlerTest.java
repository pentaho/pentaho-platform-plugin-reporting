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

import com.google.common.collect.Lists;
import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.DefaultReportEnvironment;
import org.pentaho.reporting.engine.classic.core.DefaultResourceBundleFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.ResourceBundleFactory;
import org.pentaho.reporting.engine.classic.core.StaticDataRow;
import org.pentaho.reporting.engine.classic.core.TableDataFactory;
import org.pentaho.reporting.engine.classic.core.metadata.DataFactoryMetaData;
import org.pentaho.reporting.engine.classic.core.modules.misc.tablemodel.GeneratorTableModel;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.PlainParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationMessage;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.engine.classic.core.util.beans.BeanException;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.util.HashNMap;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
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
    final ResourceKey rkey = new ResourceKey( "", "", Collections.emptyMap() );
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
    final Map<String, Object> inputs = new HashMap<String, Object>( ) {
      {
        put( "changed_unverified", "value" );
        put( "unchanged_unverified", "value1" );
        put( "verified", "value2" );
        put( "plain", "value3" );
      }
    };

    final Set<Object> changedParameters = Collections.singleton( "changed_unverified" );

    final ParameterDefinitionEntry changed =
        new DefaultListParameter( "query", "keyColumn", "textColumn", "changed_unverified", false, false, String.class );
    final ParameterDefinitionEntry unchanged =
      new DefaultListParameter( "query", "keyColumn", "textColumn", "unchanged_unverified", false, false, String.class );
    final ParameterDefinitionEntry verified =
      new DefaultListParameter( "query", "keyColumn", "textColumn", "verified", false, true, String.class );
    final ParameterDefinitionEntry plain = new PlainParameter( "plain", String.class );

    //Initial call
    final Object changedResult = handler.getSelections( changed, null, inputs );
    final Object unchangedResult = handler.getSelections( unchanged, null, inputs );
    final Object verifiedResult = handler.getSelections( verified, null, inputs );
    final Object plainResult = handler.getSelections( plain, null, inputs );

    assertEquals( "value", changedResult );
    assertEquals( "value1", unchangedResult );
    assertEquals( "value2", verifiedResult );
    assertEquals( "value3", plainResult );


    //Changed call
    final Object changedResult1 = handler.getSelections( changed, changedParameters, inputs );
    final Object unchangedResult1 = handler.getSelections( unchanged, changedParameters, inputs );
    final Object verifiedResult1 = handler.getSelections( verified, changedParameters, inputs );
    final Object plainResult1 = handler.getSelections( plain, changedParameters, inputs );

    assertEquals( "value", changedResult1 );
    assertEquals( null, unchangedResult1 );
    assertEquals( "value2", verifiedResult1 );
    assertEquals( "value3", plainResult1 );
  }

  @Test
  public void testParameterDependencies() throws ReportDataFactoryException {

    final ReportParameterValues computedParameterValues = mock( ReportParameterValues.class );
    final ParameterContext parameterContext = mock( ParameterContext.class );

    final ParameterDefinitionEntry[] entries = getDefinitions();
    when( definition.getParameterDefinitions() ).thenReturn( entries );

    // this factory will be used to get parameter names
    final CompoundDataFactory localFactory1 = mock( CompoundDataFactory.class );
    when( factory.getDataFactoryForQuery( eq( "query1" ) ) ).thenReturn( localFactory1 );
    final DataFactoryMetaData meth1 = mock( DataFactoryMetaData.class );
    when( localFactory1.getMetaData() ).thenReturn( meth1 );
    when( meth1.getReferencedFields( any( DataFactory.class ), anyString(), any( DataRow.class ) ) )
      .thenReturn( new String[] { "f1", "f2" } );

    final CompoundDataFactory localFactory2 = mock( CompoundDataFactory.class );
    when( factory.getDataFactoryForQuery( eq( "query2" ) ) ).thenReturn( localFactory2 );
    final DataFactoryMetaData meth2 = mock( DataFactoryMetaData.class );
    when( localFactory2.getMetaData() ).thenReturn( meth2 );
    when( meth2.getReferencedFields( any( DataFactory.class ), anyString(), any( DataRow.class ) ) )
      .thenReturn( new String[] { "g1", "g2" } );

    final HashNMap<String, String> parameters =
      handler.getDependentParameters( computedParameterValues, parameterContext, report );

    assertNotNull( parameters );
    assertFalse( parameters.isEmpty() );
    assertEquals( 4, parameters.keySet().size() );

    final List<String> list1 = Lists.newArrayList( parameters.getAll( "f1" ) );
    assertFalse( list1.isEmpty() );
    assertEquals( 1, list1.size() );
    assertTrue( list1.contains( "name1" ) );

    final List<String> list11 = Lists.newArrayList( parameters.getAll( "f2" ) );
    assertFalse( list11.isEmpty() );
    assertEquals( 1, list11.size() );
    assertTrue( list11.contains( "name1" ) );

    final List<String> list2 = Lists.newArrayList( parameters.getAll( "g1" ) );
    assertFalse( list2.isEmpty() );
    assertEquals( 1, list2.size() );
    assertTrue( list2.contains( "name2" ) );

    final List<String> list22 = Lists.newArrayList( parameters.getAll( "g1" ) );
    assertFalse( list22.isEmpty() );
    assertEquals( 1, list22.size() );
    assertTrue( list22.contains( "name2" ) );
  }

  private ParameterDefinitionEntry[] getDefinitions() {
    final ParameterDefinitionEntry entry1 = mock( ParameterDefinitionEntry.class );
    when( entry1.getName() ).thenReturn( "someName" );

    final ParameterDefinitionEntry entry2 = new DefaultListParameter( "query1", "keyColumn1", "textColumn1", "name1",
      false, true, String.class );

    final ParameterDefinitionEntry entry3 = new DefaultListParameter( "query2", "keyColumn2", "textColumn2", "name2",
      false, true, String.class );

    return new ParameterDefinitionEntry[] { entry1, entry2, entry3 };
  }

  @Test
  public void testComputeNormalizeLinage() {
    final ParameterContext pc = mock( ParameterContext.class );
    final ParameterDefinitionEntry pe = mock( ParameterDefinitionEntry.class );
    final HashNMap<String, String> map = new HashNMap<>();

    when( pe.getName() ).thenReturn( "aname" );

    when( pe.getParameterAttribute( eq( ParameterAttributeNames.Core.NAMESPACE ),
      eq( ParameterAttributeNames.Core.DEFAULT_VALUE_FORMULA ),
      eq( pc ) ) ).thenReturn( "=LEN([sPostal1])" );

    when( pe.getParameterAttribute( eq( ParameterAttributeNames.Core.NAMESPACE ),
      eq( ParameterAttributeNames.Core.POST_PROCESSOR_FORMULA ),
      eq( pc ) ) ).thenReturn( "=LEN([sPostal2])" );

    when( pe.getParameterAttribute( eq( ParameterAttributeNames.Core.NAMESPACE ),
      eq( ParameterAttributeNames.Core.DISPLAY_VALUE_FORMULA ),
      eq( pc ) ) ).thenReturn( "=LEN([sPostal3])" );

    handler.computeNormalLineage( pc, pe, map );

    assertNotNull( map );
    assertFalse( map.isEmpty() );
    assertEquals( 3, map.keySet().size() );

    final List<List<String>> list = new ArrayList<>();
    list.add( Lists.newArrayList( map.getAll( "sPostal1" ) ) );
    list.add( Lists.newArrayList( map.getAll( "sPostal2" ) ) );
    list.add( Lists.newArrayList( map.getAll( "sPostal3" ) ) );

    list.stream().forEach( i -> assertNotNull( i ) );
    list.stream().forEach( i -> assertFalse( i.isEmpty() ) );
    list.stream().forEach( i -> assertEquals( 1, i.size() ) );

    final List empty = list.stream().filter( i -> i.contains( "aname" ) && ( i.size() == 1 ) ).collect( Collectors.toList() );
    assertEquals( 3, list.size() );
  }

  /**
   * Test that 'skip' validation messages does not mess up with errors.
   * <p>
   * generated xml should look like:
   * <errors>
   * <error message="" parameter="parameter3"/>
   * <error message="not good at all" parameter="parameter2"/>
   * <error message="not good" parameter="parameter1"/>
   * <global-error message="kernel panic"/>
   * </errors>
   *
   * @throws ParserConfigurationException
   */
  @Test
  public void createErrorElementsTest() throws ParserConfigurationException, XPathExpressionException {
    final ValidationResult vr = new ValidationResult();
    vr.addError( "parameter1", new ValidationMessage( "not good" ) );
    vr.addError( "parameter2", new ValidationMessage( "not good at all" ) );
    vr.addError( new ValidationMessage( "kernel panic" ) );

    // save parameter name - attribute value mapping
    final Map<String, String> attrMap = new HashMap();
    attrMap.put( "parameter3", "" );
    attrMap.put( "parameter2", "not good at all" );
    attrMap.put( "parameter1", "not good" );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    final Element el = handler.createErrorElements( vr );
    handler.document.appendChild( el );

    assertNotNull( el );
    assertEquals( 3, el.getChildNodes().getLength() );

    // use xpath for future validation just to get rid of numerous for() loops in DOM api
    final XPath xpath = xpathFactory.newXPath();

    final NodeList found = NodeList.class.cast( xpath.evaluate( "/errors/error", handler.document, XPathConstants.NODESET ) );
    assertNotNull( found );

    for ( int i = 0; i < found.getLength(); i++ ) {
      final Node node = found.item( i );
      assertEquals( "error", node.getNodeName() );
      final Element oneError = (Element) node;
      final String paramName = oneError.getAttribute( "parameter" );
      assertTrue( attrMap.containsKey( paramName ) );
      assertEquals( attrMap.get( paramName ), oneError.getAttribute( "message" ) );
    }

    final Node globalError = (Node) xpath.evaluate( "/errors/global-error", handler.document, XPathConstants.NODE );
    assertNotNull( globalError );

    assertEquals( "global-error", globalError.getNodeName() );

    final Element globalErrEl = Element.class.cast( globalError );

    assertEquals( "kernel panic", globalErrEl.getAttribute( "message" ) );
  }

  /**
   * Creates simple parameter xml. Output should look like:
   * <pre>
   * {@code
   *
   * <parameter is-list="true" is-mandatory="false" is-multi-select="true" is-strict="true" name="name"
   * type="java.lang.String">
   * <attribute name="role" namespace="http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core"
   * value="user"/>
   * <values>
   *   <value label="c20" null="false" selected="false" type="java.lang.String" value="c10"/>
   *   <value label="c21" null="false" selected="false" type="java.lang.String" value="c11"/>
   * </values>
   * </parameter> }
   * </pre>
   *
   * @throws BeanException
   * @throws ReportDataFactoryException
   * @throws ParserConfigurationException
   */
  @Test
  public void createParameterElementTest()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException {
    final DefaultListParameter parameter =
      new DefaultListParameter( "query", "c1", "c2", "name", true, true, String.class );

    final ParameterContext context = getTestParameterContext();

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "name", "dep1" );
    dependencies.add( "name", "dep2" );

    final Element element = handler.createParameterElement( parameter, context, null, dependencies, false );
    assertNotNull( element );
    handler.document.appendChild( element );
    handler.createParameterDependencies( element, parameter, new HashNMap() );

    final String xml = toString( handler.document );

    examineStandardXml( handler.document );
    assertTrue( isThereAttributes( handler.document ) );
  }

  /**
   * Creates simple parameter xml. Output should look like:
   * <p>
   * <pre>
   *   {@code
   *
   * <parameter is-list="true" is-mandatory="false" is-multi-select="true" is-strict="true" name="name"
   * type="java.lang.String">
   *  <attribute name="role" namespace="http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core"
   *  value="user"/>
   *  <attribute name="must-validate-on-server" namespace="http://reporting.pentaho
   *  .org/namespaces/engine/parameter-attributes/server" value="true"/>
   *  <attribute name="has-downstream-dependent-parameter" namespace="http://reporting.pentaho
   *  .org/namespaces/engine/parameter-attributes/server" value="true"/>
   *  <values>
   *   <value label="c20" null="false" selected="false" type="java.lang.String" value="c10"/>
   *   <value label="c21" null="false" selected="false" type="java.lang.String" value="c11"/>
   *  </values>
   *  <dependencies>
   *   <name>dep1</name>
   *   <name>dep2</name>
   *  </dependencies>
   * </parameter>
   *   }
   * </pre>
   *
   * @throws BeanException
   * @throws ReportDataFactoryException
   * @throws ParserConfigurationException
   * @throws XPathExpressionException
   */
  @Test
  public void createParameterWithDependenciesTest()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException {
    final DefaultListParameter parameter =
      new DefaultListParameter( "query", "c1", "c2", "name", true, true, String.class );

    final ParameterContext context = getTestParameterContext();

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "name", "dep0" );
    dependencies.add( "name", "dep1" );

    final Element element = handler.createParameterElement( parameter, context, null, dependencies, false );
    assertNotNull( element );
    handler.document.appendChild( element );
    handler.createParameterDependencies( element, parameter, dependencies );

    examineStandardXml( handler.document );
    assertTrue( isThereAttributes( handler.document ) );

    final String xml = toString( handler.document );

    // test dependencies specific elements:
    final XPath xpath = xpathFactory.newXPath();

    final NodeList list =
      (NodeList) xpath.evaluate( "/parameter/dependencies/name", handler.document, XPathConstants.NODESET );
    assertNotNull( list );
    assertEquals( 2, list.getLength() );

    for ( int i = 0; i < list.getLength(); i++ ) {
      final Node node = list.item( i );
      assertNotNull( node );
      assertEquals( "dep" + i, node.getTextContent() );
    }

    // specific attributes: must-validate-on-server
    Node attr1 = (Node) xpath
      .evaluate( "/parameter/attribute[@name='must-validate-on-server']", handler.document, XPathConstants.NODE );
    assertNotNull( attr1 );
    Element elAttr1 = (Element) attr1;
    assertEquals( "true", elAttr1.getAttribute( "value" ) );

    // specific attributes: has-downstream-dependent-parameter
    attr1 = (Node) xpath
      .evaluate( "/parameter/attribute[@name='has-downstream-dependent-parameter']", handler.document,
        XPathConstants.NODE );
    assertNotNull( attr1 );
    elAttr1 = (Element) attr1;
    assertEquals( "true", elAttr1.getAttribute( "value" ) );
  }

  private void examineStandardXml( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameter/values/value", doc, XPathConstants.NODESET );
    assertNotNull( nodeList );
    assertEquals( 2, nodeList.getLength() );

    for ( int i = 0; i < nodeList.getLength(); i++ ) {
      final Element item = (Element) nodeList.item( i );
      assertEquals( "c1" + i, item.getAttribute( "value" ) );
    }
  }

  /**
   * Creates very simple parameter context.
   *
   * @return
   * @throws ReportDataFactoryException
   */
  private DefaultParameterContext getTestParameterContext() throws ReportDataFactoryException {
    final GeneratorTableModel model =
      new GeneratorTableModel( new String[] { "c1", "c2" }, new Class[] { String.class, String.class }, 2 );
    final DataFactory df = new TableDataFactory( "query", model );
    final DataRow dr = new StaticDataRow( new String[] { "1" }, new Object[] { 1 } );
    final Configuration conf = ClassicEngineBoot.getInstance().getExtendedConfig();
    final ResourceBundleFactory factory = new DefaultResourceBundleFactory();
    final ResourceManager manager = new ResourceManager();
    final ResourceKey key = new ResourceKey( "", "", Collections.emptyMap() );
    final ReportEnvironment env = new DefaultReportEnvironment( conf );
    return new DefaultParameterContext( df, dr, conf, factory, manager, key, env );
  }

  @Test
  public void createParameterElementNoAttributesTest()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException {
    final DefaultListParameter parameter =
      new DefaultListParameter( "query", "c1", "c2", "name", true, true, String.class );

    final ParameterContext context = getTestParameterContext();

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "name", "dep1" );
    dependencies.add( "name", "dep2" );

    final Element element = handler.createParameterElement( parameter, context, null, dependencies, Boolean.TRUE );
    assertNotNull( element );
    handler.document.appendChild( element );
    handler.createParameterDependencies( element, parameter, new HashNMap() );

    examineStandardXml( handler.document );
    assertFalse( isThereAttributes( handler.document ) );
  }

  @Test
  public void appendInitialParametersList()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap();

    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "second", "third" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, new HashMap(  ), null  );
    examineInitialParameters( handler.document );
  }

  @Test
  public void appendChangedParametersList()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap();

    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "first", "fourth" );
    dependencies.add( "second", "third" );
    dependencies.add( "third", "seventh" );
    dependencies.add( "second", "fourth" );
    //How about some cycles?
    dependencies.add( "fourth", "third" );
    dependencies.add( "third", "fourth" );
    //Independent parameters
    dependencies.add( "fifth", "sixth" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, new HashMap(  ), new String[]{"first"} );
    examineChangedDependentParameters( handler.document );
  }

  @Test
  public void appendChangedIndependentParametersList()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap();

    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "second", "third" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, new HashedMap(  ), new String[]{"fourth"} );
    examineChangedIndependentParameters( handler.document );
  }


  @Test
  public void resetInputParentChanged()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap( false );


    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "second", "third" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    final Map<String, Object> inputs = new HashMap<>(  );
    inputs.put( "first", new String[]{ "c11", "c10" } );
    inputs.put( "second", new String[]{ "c11", "c10" } );
    inputs.put( "third", new String[]{ "c11", "c10" } );
    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, inputs, new String[]{"first"} );
    examineChangedResetParameters( handler.document );
  }

  @Test
  public void resetInputParentChangedChildSentFromUI()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap( false );


    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "second", "third" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    final Map<String, Object> inputs = new HashMap<>(  );
    inputs.put( "first", new String[]{ "c11", "c10" } );
    inputs.put( "second", new String[]{ "c11", "c10" } );
    inputs.put( "third", new String[]{ "c11", "c10" } );
    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, inputs, new String[]{"first", "second"} );
    examineChangedResetParameters( handler.document );
  }


  @Test
  public void resetInputParentInTheMiddleChangedChildSentFromUI()
    throws BeanException, ReportDataFactoryException, ParserConfigurationException, XPathExpressionException,
    CloneNotSupportedException {

    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = getParametersMap( false );


    final DefaultParameterContext context = getTestParameterContext();
    final ValidationResult vr = new ValidationResult();
    vr.setParameterValues( new ReportParameterValues(  ) );

    handler.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

    final HashNMap<String, String> dependencies = new HashNMap<>();
    dependencies.add( "first", "second" );
    dependencies.add( "second", "third" );

    final Element parameters = handler.document.createElement( "parameters" );
    handler.document.appendChild( parameters );

    final Map<String, Object> inputs = new HashMap<>(  );
    inputs.put( "first", new String[]{ "c11", "c10" } );
    inputs.put( "second", new String[]{ "c11", "c10" } );
    inputs.put( "third", new String[]{ "c11", "c10" } );
    handler.appendParametersList( context, vr, parameters, dependencies, parameterDefinitions, inputs, new String[]{"third", "second"} );
    examineChangedMiddleResetParameters( handler.document );
  }

  private LinkedHashMap<String, ParameterDefinitionEntry> getParametersMap() {
    return getParametersMap( true );
  }

  private LinkedHashMap<String, ParameterDefinitionEntry> getParametersMap( boolean isStrict ) {
    final LinkedHashMap<String, ParameterDefinitionEntry> parameterDefinitions = new LinkedHashMap<>(  );
    final DefaultListParameter parameter =
      new DefaultListParameter( "query", "c1", "c2", "first", true, isStrict, String.class );
    final DefaultListParameter parameter1 =
      new DefaultListParameter( "query", "c1", "c2", "second", true, isStrict, String.class );
    final DefaultListParameter parameter2 =
      new DefaultListParameter( "query", "c1", "c2", "third", true, isStrict, String.class );
    final DefaultListParameter parameter3 =
      new DefaultListParameter( "query", "c1", "c2", "fourth", true, isStrict, String.class );
    final DefaultListParameter parameter4 =
      new DefaultListParameter( "query", "c1", "c2", "fifth", true, isStrict, String.class );
    final DefaultListParameter parameter5 =
      new DefaultListParameter( "query", "c1", "c2", "sixth", true, isStrict, String.class );
    final DefaultListParameter parameter6 =
      new DefaultListParameter( "query", "c1", "c2", "seventh", true, isStrict, String.class );
    parameterDefinitions.put( "first", parameter );
    parameterDefinitions.put( "second", parameter1 );
    parameterDefinitions.put( "third", parameter2 );
    parameterDefinitions.put( "fourth", parameter3 );
    parameterDefinitions.put( "fifth", parameter4 );
    parameterDefinitions.put( "sixth", parameter5 );
    parameterDefinitions.put( "seventh", parameter6 );
    return parameterDefinitions;
  }

  private void examineInitialParameters( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList root = (NodeList) xpath.evaluate( "/parameters", doc, XPathConstants.NODESET );
    assertEquals( 1, root.getLength() );
    final Node minimized = root.item( 0 ).getAttributes().getNamedItem( "minimized" );
    assertNull( minimized );
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameters/parameter", doc, XPathConstants.NODESET );
    assertEquals( 7, nodeList.getLength() );
    final NodeList first =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']", doc, XPathConstants.NODESET );
    assertEquals( 1, first.getLength() );
    final NodeList second =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']", doc, XPathConstants.NODESET );
    assertEquals( 1, second.getLength() );
    final NodeList third =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']", doc, XPathConstants.NODESET );
    assertEquals( 1, third.getLength() );
    final NodeList fourth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fourth']", doc, XPathConstants.NODESET );
    assertEquals( 1, fourth.getLength() );
    final NodeList attributes = (NodeList) xpath.evaluate( "/parameters/parameter/attribute", doc, XPathConstants.NODESET );
    assertTrue( attributes.getLength() != 0 );
    final NodeList dependencies = (NodeList) xpath.evaluate( "/parameters/parameter/dependencies", doc, XPathConstants.NODESET );
    assertTrue( dependencies.getLength() != 0 );
  }

  private void examineChangedDependentParameters( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList root = (NodeList) xpath.evaluate( "/parameters", doc, XPathConstants.NODESET );
    assertEquals( 1, root.getLength() );
    final Node minimized = root.item( 0 ).getAttributes().getNamedItem( "minimized" );
    assertNotNull( minimized );
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameters/parameter", doc, XPathConstants.NODESET );
    assertEquals( 5, nodeList.getLength() );
    final NodeList first =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']", doc, XPathConstants.NODESET );
    assertEquals( 1, first.getLength() );
    final NodeList second =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']", doc, XPathConstants.NODESET );
    assertEquals( 1, second.getLength() );
    final NodeList third =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']", doc, XPathConstants.NODESET );
    assertEquals( 1, third.getLength() );
    final NodeList fourth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fourth']", doc, XPathConstants.NODESET );
    assertEquals( 1, fourth.getLength() );
    final NodeList fifth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fifth']", doc, XPathConstants.NODESET );
    assertEquals( 0, fifth.getLength() );
    final NodeList sixth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='sixth']", doc, XPathConstants.NODESET );
    assertEquals( 0, sixth.getLength() );
    final NodeList seventh =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='seventh']", doc, XPathConstants.NODESET );
    assertEquals( 1, seventh.getLength() );
    final NodeList attributes = (NodeList) xpath.evaluate( "/parameters/parameter/attribute", doc, XPathConstants.NODESET );
    assertFalse( attributes.getLength() != 0 );
    final NodeList dependencies = (NodeList) xpath.evaluate( "/parameters/parameter/dependencies", doc, XPathConstants.NODESET );
    assertFalse( dependencies.getLength() != 0 );
  }

  private void examineChangedIndependentParameters( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList root = (NodeList) xpath.evaluate( "/parameters", doc, XPathConstants.NODESET );
    assertEquals( 1, root.getLength() );
    final Node minimized = root.item( 0 ).getAttributes().getNamedItem( "minimized" );
    assertNotNull( minimized );
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameters/parameter", doc, XPathConstants.NODESET );
    assertEquals( 1, nodeList.getLength() );
    final NodeList first =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']", doc, XPathConstants.NODESET );
    assertEquals( 0, first.getLength() );
    final NodeList second =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']", doc, XPathConstants.NODESET );
    assertEquals( 0, second.getLength() );
    final NodeList third =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']", doc, XPathConstants.NODESET );
    assertEquals( 0, third.getLength() );
    final NodeList fourth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fourth']", doc, XPathConstants.NODESET );
    assertEquals( 1, fourth.getLength() );
    final NodeList attributes = (NodeList) xpath.evaluate( "/parameters/parameter/attribute", doc, XPathConstants.NODESET );
    assertFalse( attributes.getLength() != 0 );
    final NodeList dependencies = (NodeList) xpath.evaluate( "/parameters/parameter/dependencies", doc, XPathConstants.NODESET );
    assertFalse( dependencies.getLength() != 0 );
  }


  private void examineChangedResetParameters( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList root = (NodeList) xpath.evaluate( "/parameters", doc, XPathConstants.NODESET );
    assertEquals( 1, root.getLength() );
    final Node minimized = root.item( 0 ).getAttributes().getNamedItem( "minimized" );
    assertNotNull( minimized );
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameters/parameter", doc, XPathConstants.NODESET );
    assertEquals( 3, nodeList.getLength() );
    final NodeList first =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']", doc, XPathConstants.NODESET );
    assertEquals( 1, first.getLength() );
    final NodeList firstValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']/values/value", doc, XPathConstants.NODESET );
    assertEquals( 2, firstValues.getLength() );
    final NodeList firstSelectedValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']/values/value[@selected='true']", doc, XPathConstants.NODESET );
    assertEquals( 2, firstSelectedValues.getLength() );
    final NodeList second =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']", doc, XPathConstants.NODESET );
    assertEquals( 1, second.getLength() );
    final NodeList secondValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']/values/value", doc, XPathConstants.NODESET );
    assertEquals( 2, secondValues.getLength() );
    //Values are reset
    final NodeList secondSelectedValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']/values/value[@selected='true']", doc, XPathConstants.NODESET );
    assertEquals( 0, secondSelectedValues.getLength() );
    final NodeList third =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']", doc, XPathConstants.NODESET );
    assertEquals( 1, third.getLength() );
    final NodeList thirdValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']/values/value", doc, XPathConstants.NODESET );
    assertEquals( 2, thirdValues.getLength() );
    //Deeper levels are also reset
    final NodeList thirdSelectedValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']/values/value[@selected='true']", doc, XPathConstants.NODESET );
    assertEquals( 0, thirdSelectedValues.getLength() );
    final NodeList fourth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fourth']", doc, XPathConstants.NODESET );
    assertEquals( 0, fourth.getLength() );
    final NodeList attributes = (NodeList) xpath.evaluate( "/parameters/parameter/attribute", doc, XPathConstants.NODESET );
    assertFalse( attributes.getLength() != 0 );
    final NodeList dependencies = (NodeList) xpath.evaluate( "/parameters/parameter/dependencies", doc, XPathConstants.NODESET );
    assertFalse( dependencies.getLength() != 0 );
  }


  private void examineChangedMiddleResetParameters( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList root = (NodeList) xpath.evaluate( "/parameters", doc, XPathConstants.NODESET );
    assertEquals( 1, root.getLength() );
    final Node minimized = root.item( 0 ).getAttributes().getNamedItem( "minimized" );
    assertNotNull( minimized );
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameters/parameter", doc, XPathConstants.NODESET );
    assertEquals( 2, nodeList.getLength() );
    final NodeList first =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='first']", doc, XPathConstants.NODESET );
    assertEquals( 0, first.getLength() );
    final NodeList second =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']", doc, XPathConstants.NODESET );
    assertEquals( 1, second.getLength() );
    final NodeList secondValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']/values/value", doc, XPathConstants.NODESET );
    assertEquals( 2, secondValues.getLength() );
    //Values are not reset
    final NodeList secondSelectedValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='second']/values/value[@selected='true']", doc, XPathConstants.NODESET );
    assertEquals( 2, secondSelectedValues.getLength() );
    final NodeList third =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']", doc, XPathConstants.NODESET );
    assertEquals( 1, third.getLength() );
    final NodeList thirdValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']/values/value", doc, XPathConstants.NODESET );
    assertEquals( 2, thirdValues.getLength() );
    //Deeper levels are reset
    final NodeList thirdSelectedValues =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='third']/values/value[@selected='true']", doc, XPathConstants.NODESET );
    assertEquals( 0, thirdSelectedValues.getLength() );
    final NodeList fourth =
      (NodeList) xpath.evaluate( "/parameters/parameter[@name='fourth']", doc, XPathConstants.NODESET );
    assertEquals( 0, fourth.getLength() );
    final NodeList attributes = (NodeList) xpath.evaluate( "/parameters/parameter/attribute", doc, XPathConstants.NODESET );
    assertFalse( attributes.getLength() != 0 );
    final NodeList dependencies = (NodeList) xpath.evaluate( "/parameters/parameter/dependencies", doc, XPathConstants.NODESET );
    assertFalse( dependencies.getLength() != 0 );
  }

  private boolean isThereAttributes( final Document doc ) throws XPathExpressionException {
    final XPath xpath = xpathFactory.newXPath();
    final NodeList nodeList = (NodeList) xpath.evaluate( "/parameter/attribute", doc, XPathConstants.NODESET );
    return nodeList.getLength() != 0;
  }
}
