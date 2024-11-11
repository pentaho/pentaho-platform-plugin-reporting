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

import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeRegistry;
import org.pentaho.reporting.engine.classic.core.metadata.DefaultAttributeCore;
import org.pentaho.reporting.engine.classic.core.metadata.DefaultAttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementTypeRegistry;
import org.pentaho.reporting.engine.classic.core.metadata.MaturityLevel;
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
  public static String FORCE_ALL_PAGES = "org.pentaho.reporting.platform.plugin.ForceAllPages";

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
            new DefaultAttributeCore(), MaturityLevel.Production, ClassicEngineBoot.computeVersionId( 3, 8, 0 ) );

    final AttributeRegistry registry = ElementTypeRegistry.getInstance().getAttributeRegistry( "master-report" );
    registry.putAttributeDescription( metaData );
  }
}
