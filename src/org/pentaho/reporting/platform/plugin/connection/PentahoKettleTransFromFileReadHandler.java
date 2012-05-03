package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleTransFromFileReadHandler;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransFromFileProducer;
import org.xml.sax.SAXException;

/**
 * Todo: Document me!
 * <p/>
 * Date: 05.01.2010
 * Time: 17:41:25
 *
 * @author Thomas Morgner.
 */
public class PentahoKettleTransFromFileReadHandler extends KettleTransFromFileReadHandler
{
  public PentahoKettleTransFromFileReadHandler()
  {
  }

  /**
   * Returns the object for this element or null, if this element does
   * not create an object.
   *
   * @return the object.
   * @throws org.xml.sax.SAXException if an parser error occured.
   */
  public Object getObject() throws SAXException
  {
    return new PentahoKettleTransFromFileProducer
        (getRepositoryName(), getFileName(), getStepName(), getUsername(), getPassword(),
            getDefinedArgumentNames(), getDefinedVariableNames());
  }
}
