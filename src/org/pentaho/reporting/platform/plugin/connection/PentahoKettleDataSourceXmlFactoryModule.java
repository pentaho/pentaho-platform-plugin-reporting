package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleDataSourceXmlFactoryModule;
import org.pentaho.reporting.libraries.xmlns.parser.XmlReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.XmlDocumentInfo;

public class PentahoKettleDataSourceXmlFactoryModule extends KettleDataSourceXmlFactoryModule
{
  public PentahoKettleDataSourceXmlFactoryModule()
  {
  }

  public XmlReadHandler createReadHandler(final XmlDocumentInfo documentInfo)
  {
    return new PentahoKettleDataSourceReadHandler();
  }
}
