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

import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeRegistry;
import org.pentaho.reporting.engine.classic.core.metadata.DefaultAttributeCore;
import org.pentaho.reporting.engine.classic.core.metadata.DefaultAttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementTypeRegistry;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.ConnectionReadHandlerFactory;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.SQLDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser
   .KettleTransformationProducerReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MondrianDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.CubeFileProviderReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DataSourceProviderReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.olap4j.Olap4JDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.olap4j.parser.OlapConnectionReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.parser.PmdConfigReadHandlerFactory;
import org.pentaho.reporting.libraries.base.boot.AbstractModule;
import org.pentaho.reporting.libraries.base.boot.ModuleInitializeException;
import org.pentaho.reporting.libraries.base.boot.SubSystem;
import org.pentaho.reporting.platform.plugin.connection.PentahoCubeFileProviderReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoJndiConnectionReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoKettleTransFromFileReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoMondrianDataSourceProviderReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoOlap4JJndiConnectionReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoPmdConfigReadHandler;

public class PentahoPlatformModule extends AbstractModule {

  public static final String PIR_NAMESPACE =
      "http://reporting.pentaho.org/namespaces/engine/attributes/pentaho/interactive-reporting";

  public PentahoPlatformModule() throws ModuleInitializeException {
    loadModuleInfo();
  }

  public void initialize( final SubSystem subSystem ) throws ModuleInitializeException {
    ConnectionReadHandlerFactory.getInstance().setElementHandler( SQLDataFactoryModule.NAMESPACE, "jndi",
        PentahoJndiConnectionReadHandler.class );
    PmdConfigReadHandlerFactory.getInstance().setElementHandler( PmdDataFactoryModule.NAMESPACE, "config",
        PentahoPmdConfigReadHandler.class );
    CubeFileProviderReadHandlerFactory.getInstance().setElementHandler( MondrianDataFactoryModule.NAMESPACE,
        "cube-file", PentahoCubeFileProviderReadHandler.class );
    DataSourceProviderReadHandlerFactory.getInstance().setElementHandler( MondrianDataFactoryModule.NAMESPACE, "jndi",
        PentahoMondrianDataSourceProviderReadHandler.class );
    OlapConnectionReadHandlerFactory.getInstance().setElementHandler( Olap4JDataFactoryModule.NAMESPACE, "jndi",
        PentahoOlap4JJndiConnectionReadHandler.class );
    KettleTransformationProducerReadHandlerFactory.getInstance().setElementHandler( KettleDataFactoryModule.NAMESPACE,
        "query-file", PentahoKettleTransFromFileReadHandler.class );

    final String bundleLocation = "org.pentaho.reporting.platform.plugin.metadata";
    final String keyPrefix = "attribute.pir.";
    final DefaultAttributeMetaData metaData =
        new DefaultAttributeMetaData( PIR_NAMESPACE, "VERSION", bundleLocation, keyPrefix, null, String.class, true,
            false, true, false, false, false, false, AttributeMetaData.VALUEROLE_VALUE, false, true,
            new DefaultAttributeCore(), false, ClassicEngineBoot.computeVersionId( 3, 8, 0 ) );

    final AttributeRegistry registry = ElementTypeRegistry.getInstance().getAttributeRegistry( "master-report" );
    registry.putAttributeDescription( metaData );
  }
}
