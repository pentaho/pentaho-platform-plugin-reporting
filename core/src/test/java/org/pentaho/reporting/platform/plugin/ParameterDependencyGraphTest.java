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

package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ParameterDependencyGraphTest {

  Set<String> asSet( String... p ) {
    return new HashSet<>( Arrays.asList( p ) );
  }

  @Test
  public void testGetDependentParameterFor() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( "param1", "param2" );
    assertEquals( paramDepGrap.getDependentParameterFor( "param1" ), asSet() );

    paramDepGrap.setAllParametersProcessed( true );
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );
    assertEquals( paramDepGrap.getDependentParameterFor( "param1" ), asSet( "param2", "param3" ) );
    paramDepGrap.setAllParametersProcessed( false );
    assertEquals( paramDepGrap.getDependentParameterFor( "param2" ), Collections.emptySet() );
  }

  @Test
  public void testGetAllDependencies() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( "param1", "param2" );
    paramDepGrap.setAllParametersProcessed( false );
    assertEquals( paramDepGrap.getAllDependencies( "param1" ), asSet( "param1", "param2" ) );

    paramDepGrap.setAllParametersProcessed( true );
    assertEquals( paramDepGrap.getAllDependencies( asSet( "param1", "param1" ) ), Collections.emptySet() );
  }

  @Test
  public void testGetKnownParameter() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph();
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    dependencyGraph.put( "param2", asSet( "param3", "param4" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );

    assertEquals( paramDepGrap.getKnownParameter(), asSet( "param1", "param2" ) );
  }

  @Test
  public void testAddDependency() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph();
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    dependencyGraph.put( "param2", asSet( "param3", "param4" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );

    paramDepGrap.addDependency( "param3", "down1" );
    assertEquals( paramDepGrap.getKnownParameter(), asSet( "param1", "param2", "param3" ) );
  }

  @Test
  public void testDoesDependencyExist() {
    final ParameterDependencyGraph dependencyGraph = new ParameterDependencyGraph( "param1",
      "param2", "param3" );
    assertFalse( dependencyGraph.doesDependencyExist( "param2" ) );
    dependencyGraph.addDependency( "param1", "param2" );
    assertTrue( dependencyGraph.doesDependencyExist( "param2" ) );
  }

  @Test
  public void testGetAllParameterNames() {
    Set<String> paramSet = new HashSet<>();
    paramSet.add( "param1" );
    paramSet.add( "param2" );
    paramSet.add( "param3" );

    final ParameterDependencyGraph dependencyGraph = new ParameterDependencyGraph( "param1",
      "param2", "param3" );
    assertEquals( paramSet, dependencyGraph.getAllParameterNames() );

    paramSet.add( "param4" );
    dependencyGraph.setAllParameterNames( paramSet );
    assertEquals( paramSet, dependencyGraph.getAllParameterNames() );

  }
}
