package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleDataSourceReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.AbstractKettleTransformationReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleTransFromFileReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.XmlReadHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * Todo: Document me!
 * <p/>
 * Date: 05.01.2010
 * Time: 19:49:12
 *
 * @author Thomas Morgner.
 */
public class PentahoKettleDataSourceReadHandler extends KettleDataSourceReadHandler
{
  public PentahoKettleDataSourceReadHandler()
  {
  }

  /**
   * Returns the handler for a child element.
   *
   * @param tagName the tag name.
   * @param atts    the attributes.
   * @return the handler or null, if the tagname is invalid.
   * @throws org.xml.sax.SAXException if there is a parsing error.
   */
  protected XmlReadHandler getHandlerForChild(final String uri,
                                              final String tagName,
                                              final Attributes atts) throws SAXException
  {
    if (tagName.equals("query-file"))
    {
      final AbstractKettleTransformationReadHandler queryReadHandler = new PentahoKettleTransFromFileReadHandler();
      addQueryHandler(queryReadHandler);
      return queryReadHandler;
    }
    return super.getHandlerForChild(uri, tagName, atts);
  }
}

