/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.reporting.platform.plugin;

import java.util.Locale;

import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementTypeRegistry;

public class ReportingPluginModuleIT extends TestCase {
  public ReportingPluginModuleIT() {
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
