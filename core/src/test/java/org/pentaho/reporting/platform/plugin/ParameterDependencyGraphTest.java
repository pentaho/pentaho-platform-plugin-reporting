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
 * Copyright 2006 - 2018 Hitachi Vantara.  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import static org.junit.Assert.assertEquals;

public class ParameterDependencyGraphTest {

  Set<String> asSet( String... p ) {
    return new HashSet<>( Arrays.asList( p ) );
  }

  @Test
  public void testGetDependentParameterFor() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( true, "param1", "param2" );
    assertEquals( paramDepGrap.getDependentParameterFor( "param1" ), asSet( "param1", "param2" ) );

    paramDepGrap.setNoDependencyInformationAvailable( false );
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );
    assertEquals( paramDepGrap.getDependentParameterFor( "param1" ), asSet( "param2", "param3" ) );
    assertEquals( paramDepGrap.getDependentParameterFor( "param2" ), Collections.emptySet() );
  }

  @Test
  public void testGetAllDependencies() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( true, "param1", "param2" );
    assertEquals( paramDepGrap.getAllDependencies( "param1" ), asSet( "param1", "param2" ) );

    paramDepGrap.setNoDependencyInformationAvailable( false );
    assertEquals( paramDepGrap.getAllDependencies( asSet( "param1", "param1" ) ), Collections.emptySet() );
  }

  @Test
  public void testGetKnownParameter() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( true );
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    dependencyGraph.put( "param2", asSet( "param3", "param4" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );

    assertEquals( paramDepGrap.getKnownParameter(), asSet( "param1", "param2" ) );
  }

  @Test
  public void testAddDependency() {
    ParameterDependencyGraph paramDepGrap = new ParameterDependencyGraph( true );
    LinkedHashMap<String, Set<String>> dependencyGraph = new LinkedHashMap<>();
    dependencyGraph.put( "param1", asSet( "param2", "param3" ) );
    dependencyGraph.put( "param2", asSet( "param3", "param4" ) );
    paramDepGrap.setDependencyGraph( dependencyGraph );

    paramDepGrap.addDependency( "param3", "down1" );
    assertEquals( paramDepGrap.getKnownParameter(), asSet( "param1", "param2", "param3" ) );
  }

}
