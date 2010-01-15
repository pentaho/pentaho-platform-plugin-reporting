package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleDataSourceXmlFactoryModule;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleDataSourceReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.XmlReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.XmlDocumentInfo;

/**
 * Todo: Document me!
 * <p/>
 * Date: 15.01.2010
 * Time: 19:07:17
 *
 * @author Thomas Morgner.
 */
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
