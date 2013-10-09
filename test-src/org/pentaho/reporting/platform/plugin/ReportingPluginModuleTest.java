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

import java.util.Locale;

import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementTypeRegistry;

public class ReportingPluginModuleTest extends TestCase {
  public ReportingPluginModuleTest() {
  }

  protected void setUp() throws Exception {
    ClassicEngineBoot.getInstance().start();
  }

  public void testPirAttribute() {
    final ElementMetaData metaData = ElementTypeRegistry.getInstance().getElementType( "master-report" );

    final AttributeMetaData attributeDescription =
        metaData.getAttributeDescription( PentahoPlatformModule.PIR_NAMESPACE, "VERSION" );
    assertEquals( PentahoPlatformModule.PIR_NAMESPACE, attributeDescription.getNameSpace() );
    assertEquals( "VERSION", attributeDescription.getName() );
    assertEquals( "pir-version", attributeDescription.getDisplayName( Locale.US ) );
  }
}
