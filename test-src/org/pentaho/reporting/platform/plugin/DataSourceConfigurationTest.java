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

import junit.framework.TestCase;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.ConnectionReadHandler;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.ConnectionReadHandlerFactory;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.SQLDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.cda.CdaQueryBackend;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleTransformationProducerReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleTransformationProducerReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.CubeFileProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MondrianConnectionProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MondrianDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.CubeFileProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.CubeFileProviderReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DataSourceProviderReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.DataSourceProviderReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.olap4j.Olap4JDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.olap4j.parser.OlapConnectionReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.olap4j.parser.OlapConnectionReadHandlerFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.PmdDataFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.parser.IPmdConfigReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.parser.PmdConfigReadHandlerFactory;
import org.pentaho.reporting.libraries.base.boot.ObjectFactory;
import org.pentaho.reporting.platform.plugin.connection.CdaPluginLocalQueryBackend;
import org.pentaho.reporting.platform.plugin.connection.PentahoCubeFileProvider;
import org.pentaho.reporting.platform.plugin.connection.PentahoCubeFileProviderReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoJndiConnectionReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoKettleTransFromFileReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoMondrianConnectionProvider;
import org.pentaho.reporting.platform.plugin.connection.PentahoMondrianDataSourceProviderReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoOlap4JJndiConnectionReadHandler;
import org.pentaho.reporting.platform.plugin.connection.PentahoPmdConfigReadHandler;
import org.pentaho.test.platform.engine.core.MicroPlatform;

public class DataSourceConfigurationTest extends TestCase
{

  private MicroPlatform microPlatform;
  private ObjectFactory objectFactory;

  public DataSourceConfigurationTest()
  {
  }

  @Override
  protected void setUp() throws Exception
  {
    microPlatform = MicroPlatformFactory.create();
    microPlatform.start();

    // micro-platform does not boot the engine ..
    ClassicEngineBoot.getInstance().start();

    IPentahoSession session = new StandaloneSession();
    PentahoSessionHolder.setSession(session);

    objectFactory = ClassicEngineBoot.getInstance().getObjectFactory();
  }

  @Override
  protected void tearDown() throws Exception
  {
    microPlatform.stop();
  }

  public void testCdaQueryBackend()
  {
    assertInstanceOf(objectFactory.get(CdaQueryBackend.class), CdaPluginLocalQueryBackend.class);
  }

  private void assertInstanceOf(final Object returnedType, final Class expectedType)
  {
    if (expectedType.isInstance(returnedType))
    {
      return;
    }
    if (returnedType == null)
    {
      fail("Expected object of type " + expectedType + " but got <null>");
    }
    else
    {
      fail("Expected object of type " + expectedType + " but got " + returnedType.getClass());
    }
  }

  public void testMondrianConnectionProvider()
  {
    assertInstanceOf(objectFactory.get(MondrianConnectionProvider.class), PentahoMondrianConnectionProvider.class);
    assertInstanceOf(objectFactory.get(CubeFileProvider.class), PentahoCubeFileProvider.class);
  }

  public void testPentahoCubeFileProviderReadHandler()
  {
    final CubeFileProviderReadHandler handler =
        CubeFileProviderReadHandlerFactory.getInstance().getHandler(MondrianDataFactoryModule.NAMESPACE, "cube-file");
    assertInstanceOf(handler, PentahoCubeFileProviderReadHandler.class);
  }

  public void testPentahoJndiConnectionReadHandler()
  {
    final ConnectionReadHandler handler =
        ConnectionReadHandlerFactory.getInstance().getHandler(SQLDataFactoryModule.NAMESPACE, "jndi");
    assertInstanceOf(handler, PentahoJndiConnectionReadHandler.class);
  }

  public void testKettleTransFromFileReadHandler()
  {
    final KettleTransformationProducerReadHandler handler =
        KettleTransformationProducerReadHandlerFactory.getInstance().getHandler(KettleDataFactoryModule.NAMESPACE,
            "query-file");
    assertInstanceOf(handler, PentahoKettleTransFromFileReadHandler.class);
  }

  public void testMondrianDataSourceProvider()
  {
    final DataSourceProviderReadHandler handler =
        DataSourceProviderReadHandlerFactory.getInstance().getHandler(MondrianDataFactoryModule.NAMESPACE, "jndi");
    assertInstanceOf(handler, PentahoMondrianDataSourceProviderReadHandler.class);
  }

  public void testOlap4JDataSourceProvider()
  {
    final OlapConnectionReadHandler handler =
        OlapConnectionReadHandlerFactory.getInstance().getHandler(Olap4JDataFactoryModule.NAMESPACE, "jndi");
    assertInstanceOf(handler, PentahoOlap4JJndiConnectionReadHandler.class);
  }

  public void testPmdConnectionReadHandler()
  {
    final IPmdConfigReadHandler handler =
        PmdConfigReadHandlerFactory.getInstance().getHandler(PmdDataFactoryModule.NAMESPACE, "config");
    assertInstanceOf(handler, PentahoPmdConfigReadHandler.class);
  }

}
