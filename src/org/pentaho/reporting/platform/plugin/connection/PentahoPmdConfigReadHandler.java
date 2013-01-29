package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.IPmdConnectionProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.parser.IPmdConfigReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.AbstractXmlReadHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Michael D'Amour
 */
public class PentahoPmdConfigReadHandler extends AbstractXmlReadHandler implements IPmdConfigReadHandler
{
  private boolean labelMapping;
  private String domain;
  private String xmiFile;

  public PentahoPmdConfigReadHandler()
  {
  }

  /**
   * Starts parsing.
   * 
   * @param attrs
   *          the attributes.
   * @throws SAXException
   *           if there is a parsing error.
   */
  protected void startParsing(final Attributes attrs) throws SAXException
  {
    super.startParsing(attrs);

    final String labelMappingAttr = attrs.getValue(getUri(), "label-mapping"); //$NON-NLS-1$
    if (labelMappingAttr != null)
    {
      labelMapping = "true".equals(labelMappingAttr); //$NON-NLS-1$
    }

    xmiFile = attrs.getValue(getUri(), "xmi-file"); //$NON-NLS-1$
    domain = attrs.getValue(getUri(), "domain"); //$NON-NLS-1$
  }

  public boolean getLabelMapping()
  {
    return labelMapping;
  }

  public String getDomain()
  {
    return domain;
  }

  public String getXmiFile()
  {
    return xmiFile;
  }

  public boolean isLabelMapping()
  {
    return labelMapping;
  }

  /**
   * Returns the object for this element or null, if this element does not create an object.
   * 
   * @return the object.
   * @throws SAXException
   *           if there is a parsing error.
   */
  public Object getObject() throws SAXException
  {
    return null;
  }

  public IPmdConnectionProvider getConnectionProvider()
  {
    return new PentahoPmdConnectionProvider();
  }

}