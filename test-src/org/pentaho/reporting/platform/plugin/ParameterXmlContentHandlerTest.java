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
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.ReportEnvironment;
import org.pentaho.reporting.engine.classic.core.ResourceBundleFactory;
import org.pentaho.reporting.engine.classic.core.metadata.DataFactoryMetaData;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
import org.pentaho.reporting.libraries.base.config.Configuration;
import org.pentaho.reporting.libraries.base.util.HashNMap;
import org.pentaho.reporting.libraries.resourceloader.ResourceKey;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

import java.util.Collections;
import java.util.List;

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

  private ParameterXmlContentHandler handler;

  private MasterReport report;
  private ReportParameterDefinition definition;
  private CompoundDataFactory factory;

  @BeforeClass
  public static void beforeClass() {
    ClassicEngineBoot.getInstance().start();
  }

  @Before
  public void before() {
    final ParameterContentGenerator generator = mock( ParameterContentGenerator.class );
    handler = new ParameterXmlContentHandler( generator, true );

    /*final ReportParameterValues computedParameterValues = mock( ReportParameterValues.class );
    final ParameterContext parameterContext = mock( ParameterContext.class );*/
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
    assertEquals( 2, parameters.keySet().size() );

    List<String> list1 = Lists.newArrayList( parameters.getAll( "name1" ) );
    assertFalse( list1.isEmpty() );
    assertEquals( 2, list1.size() );
    assertTrue( list1.contains( "f1" ) );
    assertTrue( list1.contains( "f2" ) );

    List<String> list2 = Lists.newArrayList( parameters.getAll( "name2" ) );
    assertFalse( list2.isEmpty() );
    assertEquals( 2, list2.size() );
    assertTrue( list2.contains( "g1" ) );
    assertTrue( list2.contains( "g2" ) );
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
    assertEquals( 1, map.keySet().size() );

    List<String> list = Lists.newArrayList( map.getAll( "aname" ) );
    assertNotNull( list );
    assertFalse( list.isEmpty() );
    assertEquals( 3, list.size() );

    assertTrue( list.contains( "sPostal1" ) );
    assertTrue( list.contains( "sPostal3" ) );
    assertTrue( list.contains( "sPostal2" ) );
  }
}
