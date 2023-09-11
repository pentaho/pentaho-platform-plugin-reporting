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
 * Copyright (c) 2002-2023 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.pentaho.reporting.engine.classic.core.CompoundDataFactory;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.DataRow;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.StaticDataRow;
import org.pentaho.reporting.engine.classic.core.TableDataFactory;
import org.pentaho.reporting.engine.classic.core.designtime.datafactory.DesignTimeDataFactoryContext;
import org.pentaho.reporting.engine.classic.core.function.FormulaExpression;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionPropertyMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ExpressionRegistry;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.libraries.base.util.DebugLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the dependency graph. This graph returns all fields a given parameter is dependent on.
 * So for a list parameter "City" with a query "SELECT * FROM Cities WHERE Country = ${Country}"
 * this would return a mapping "City -> [Country]".
 * <p>
 * If there is a problem with computing dependency information or if the queries use a datasource
 * that cannot provide dependency information, this implementation will return "false" on
 * "allParametersProcessed"
 * and will return all other parameters as dependent values (to indicate that any change may be a
 * cause of change in the parameter values).
 */
public class ParameterDependencyGraph {
  private static final String SYS_IGNORE_PARAM = "::org.pentaho.reporting";

  private boolean allParametersProcessed;
  private LinkedHashMap<String, Set<String>> dependencyGraph;
  private Set<String> allParameterNames;

  public ParameterDependencyGraph( final MasterReport report,
                                   final Map<String, ParameterDefinitionEntry> reportParameter,
                                   final ParameterContext parameterContext,
                                   final Map<String, Object> computedParameterValues ) {
    this.dependencyGraph = new LinkedHashMap<>();
    this.allParameterNames = new HashSet<>( reportParameter.keySet() );
    this.allParametersProcessed = processDependentParameters( report, reportParameter, parameterContext,
      new StaticDataRow( computedParameterValues ) );
  }

  /**
   * Test support ..
   *
   * @param allParameterNames
   */
  ParameterDependencyGraph( String... allParameterNames ) {
    this.dependencyGraph = new LinkedHashMap<>();
    this.allParametersProcessed = true;
    this.allParameterNames = new HashSet<>( Arrays.asList( allParameterNames ) );
  }

  void setAllParametersProcessed( boolean allParametersProcessed ) {
    this.allParametersProcessed = allParametersProcessed;
  }

  public LinkedHashMap<String, Set<String>> getDependencyGraph() {
    return dependencyGraph;
  }

  public void setDependencyGraph( LinkedHashMap<String, Set<String>> arg ) {
    this.dependencyGraph = arg;
  }

  public Set<String> getAllParameterNames() {
    return allParameterNames;
  }

  public void setAllParameterNames( Set<String> allParameterNames ) {
    this.allParameterNames = allParameterNames;
  }

  public Set<String> getDependentParameterFor( String parameterName ) {
    if ( !allParametersProcessed ) {
      return Collections.emptySet();
    }

    final Set<String> strings = dependencyGraph.getOrDefault( parameterName, Collections.emptySet() );
    return Collections.unmodifiableSet( strings );
  }

  public Set<String> getAllDependencies( String... parameterNames ) {
    return getAllDependencies( Arrays.asList( parameterNames ) );
  }

  public Set<String> getAllDependencies( Iterable<String> parameterNames ) {
    if ( !allParametersProcessed ) {
      return Collections.unmodifiableSet( allParameterNames );
    }

    LinkedHashSet<String> visited = new LinkedHashSet<>();
    LinkedHashSet<String> retval = new LinkedHashSet<>();
    for ( String parameterName : parameterNames ) {
      retval.addAll( getAllDependencies( parameterName, visited ) );
    }
    return Collections.unmodifiableSet( retval );
  }

  public Set<String> getKnownParameter() {
    return Collections.unmodifiableSet( dependencyGraph.keySet() );
  }

  /**
   * Verify that the parameter exists as a dependency in the dependency graph
   * @param parameterName
   * @return
   */
  public boolean doesDependencyExist( String parameterName ) {
    return dependencyGraph
          .values()
          .stream()
          .flatMap( Set::stream )
          .anyMatch( s -> s.equals( parameterName ) );
  }
  /**
   * Recursively collects all dependencies, and avoids visiting parameters twice and thus
   * wont crash on circular dependencies.
   *
   * @param parameterName
   * @param visited
   * @return
   */
  private Set<String> getAllDependencies( String parameterName, Set<String> visited ) {
    Set<String> p = getDependentParameterFor( parameterName );

    final Set<String> retval = new LinkedHashSet<>();
    retval.addAll( p );

    for ( String d : p ) {
      if ( visited.contains( d ) ) {
        continue;
      }
      visited.add( d );
      retval.addAll( getAllDependencies( d, visited ) );
    }
    return retval;
  }

  /**
   * Declare that 'downstream' is dependent on information provided by 'parameter'.
   * Therefore changes to 'parameter' should trigger a recomputation of 'downstream'.
   *
   * @param parameter
   * @param downstream
   */
  void addDependency( String parameter, String downstream ) {
    Set<String> deps = dependencyGraph.computeIfAbsent( parameter, k -> new LinkedHashSet<>() );
    deps.add( downstream );
  }

  private boolean processDependentParameters( MasterReport report,
                                              Map<String, ParameterDefinitionEntry> reportParameter,
                                              ParameterContext parameterContext,
                                              DataRow parameterValues ) {

    boolean isDependencyInfoAvail = true;
    try {
      final DesignTimeDataFactoryContext factoryContext = new DesignTimeDataFactoryContext( report );
      final CompoundDataFactory cdf = CompoundDataFactory.normalize( report.getDataFactory() );
      final CompoundDataFactory derive = (CompoundDataFactory) cdf.derive();
      derive.initialize( factoryContext );

      try {
        for ( final ParameterDefinitionEntry entry : reportParameter.values() ) {
          final List<String> dependentParameter = computeNormalLineage( parameterContext, entry );
          final List<String> queryDependencies = computeListParameterLineage( derive, entry, parameterValues );

          // No dependency information at all - No need to continue
          if ( queryDependencies == null ) {
            isDependencyInfoAvail = false;
            continue;
          }

          if ( dependentParameter != null && dependentParameter.size() > 0 ) {
            dependentParameter.forEach( p -> addDependency( p, entry.getName() ) );
          }
          if ( queryDependencies != null && queryDependencies.size() > 0 ) {
            queryDependencies.forEach( p -> addDependency( p, entry.getName() ) );
          }
        }
      } finally {
        derive.close();
      }
    } catch ( ReportDataFactoryException re ) {
      DebugLog.log( "Failed to compute dependency information", re );
      isDependencyInfoAvail = false;
    }
    return isDependencyInfoAvail;
  }

  private List<String> computeListParameterLineage( CompoundDataFactory cdf,
                                                    ParameterDefinitionEntry entry,
                                                    DataRow computedParameterValues ) {
    // default list parameter is only dynamic values provider now.
    if ( !( entry instanceof DefaultListParameter ) ) {
      return Collections.emptyList();
    }

    final DefaultListParameter listParameter = (DefaultListParameter) entry;
    final String queryName = listParameter.getQueryName();
    if ( queryName == null ) {
      return Collections.emptyList();
    }

    final DataFactory dataFactoryForQuery = cdf.getDataFactoryForQuery( queryName );
    // We are also checking for TableDataFactory, because this Data Factory is a manual table that cannot
    // have or be used in a dependency query. So it's list should be empty.
    if ( dataFactoryForQuery == null || dataFactoryForQuery instanceof TableDataFactory ) {
      return Collections.emptyList();
    }

    final String[] fields = dataFactoryForQuery.getMetaData()
      .getReferencedFields( dataFactoryForQuery, queryName, computedParameterValues );

    if ( fields != null ) {
      return Arrays.stream( fields ).filter( field -> !field.startsWith( SYS_IGNORE_PARAM ) )
        .collect( Collectors.toList() );
    } else {
      // No dependency information available.
      // That means we cannot use dependent parameter at all.
      return null;
    }
  }

  public boolean areAllParametersProcessed() {
    return allParametersProcessed;
  }

  private static String extractFormula( final ParameterContext parameterContext,
                                        final ParameterDefinitionEntry pe,
                                        final String attr ) {
    return pe.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      attr, parameterContext );
  }

  static List<String> computeNormalLineage( final ParameterContext parameterContext,
                                            final ParameterDefinitionEntry pe ) {
    final ArrayList<String> retval = new ArrayList<>();

    retval.addAll(
      analyzeFormula( extractFormula( parameterContext, pe, ParameterAttributeNames.Core.DEFAULT_VALUE_FORMULA ) ) );
    retval.addAll(
      analyzeFormula( extractFormula( parameterContext, pe, ParameterAttributeNames.Core.POST_PROCESSOR_FORMULA ) ) );
    retval.addAll(
      analyzeFormula( extractFormula( parameterContext, pe, ParameterAttributeNames.Core.DISPLAY_VALUE_FORMULA ) ) );
    retval.addAll(
      analyzeFormula( extractFormula( parameterContext, pe, ParameterAttributeNames.Core.HIDDEN_FORMULA ) ) );
    return retval;
  }

  private static List<String> analyzeFormula( final String formula ) {
    if ( formula == null ) {
      return Collections.emptyList();
    }

    final FormulaExpression fe = new FormulaExpression();
    fe.setFormula( formula );

    final ExpressionMetaData md = ExpressionRegistry.getInstance().getExpressionMetaData( fe.getClass().getName() );
    final ExpressionPropertyMetaData pd = md.getPropertyDescription( "formula" );
    return Arrays.asList( pd.getReferencedFields( fe, fe.getFormula() ) );
  }

}
