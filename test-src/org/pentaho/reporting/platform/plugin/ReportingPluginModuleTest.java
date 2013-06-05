package org.pentaho.reporting.platform.plugin;

import java.util.Locale;

import junit.framework.TestCase;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.filter.types.LabelType;
import org.pentaho.reporting.engine.classic.core.filter.types.bands.MasterReportType;
import org.pentaho.reporting.engine.classic.core.metadata.AttributeMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementTypeRegistry;

public class ReportingPluginModuleTest extends TestCase
{
  public ReportingPluginModuleTest()
  {
  }

  protected void setUp() throws Exception
  {
    ClassicEngineBoot.getInstance().start();
  }

  public void testPirAttribute()
  {
    final ElementMetaData metaData = ElementTypeRegistry.getInstance().getElementType("master-report");

    final AttributeMetaData attributeDescription = metaData.getAttributeDescription
        (PentahoPlatformModule.PIR_NAMESPACE, "VERSION");
    assertEquals(PentahoPlatformModule.PIR_NAMESPACE, attributeDescription.getNameSpace());
    assertEquals("VERSION", attributeDescription.getName());
    assertEquals("pir-version", attributeDescription.getDisplayName(Locale.US));
  }
}
