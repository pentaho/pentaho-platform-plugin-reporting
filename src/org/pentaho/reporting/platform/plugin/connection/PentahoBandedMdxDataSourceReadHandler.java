package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.AbstractMDXDataFactory;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.MondrianConnectionProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.mondrian.parser.BandedMDXDataSourceReadHandler;
import org.xml.sax.SAXException;


public class PentahoBandedMdxDataSourceReadHandler extends BandedMDXDataSourceReadHandler
{
  public PentahoBandedMdxDataSourceReadHandler()
  {
  }

  /**
   * Done parsing.
   *
   * @throws org.xml.sax.SAXException if there is a parsing error.
   */
  protected void doneParsing() throws SAXException
  {
    super.doneParsing();
    final AbstractMDXDataFactory o = (AbstractMDXDataFactory) getObject();
    final MondrianConnectionProvider connectionProvider = PentahoSystem.get(MondrianConnectionProvider.class);
    if (connectionProvider != null)
    {
      o.setMondrianConnectionProvider(connectionProvider);
    }
  }
}
