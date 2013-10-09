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

package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.IPmdConnectionProvider;
import org.pentaho.reporting.engine.classic.extensions.datasources.pmd.parser.IPmdConfigReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.AbstractXmlReadHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author Michael D'Amour
 */
public class PentahoPmdConfigReadHandler extends AbstractXmlReadHandler implements IPmdConfigReadHandler {
  private boolean labelMapping;
  private String domain;
  private String xmiFile;

  public PentahoPmdConfigReadHandler() {
  }

  /**
   * Starts parsing.
   * 
   * @param attrs
   *          the attributes.
   * @throws SAXException
   *           if there is a parsing error.
   */
  protected void startParsing( final Attributes attrs ) throws SAXException {
    super.startParsing( attrs );

    final String labelMappingAttr = attrs.getValue( getUri(), "label-mapping" ); //$NON-NLS-1$
    if ( labelMappingAttr != null ) {
      labelMapping = "true".equals( labelMappingAttr ); //$NON-NLS-1$
    }

    xmiFile = attrs.getValue( getUri(), "xmi-file" ); //$NON-NLS-1$
    domain = attrs.getValue( getUri(), "domain" ); //$NON-NLS-1$
  }

  public boolean getLabelMapping() {
    return labelMapping;
  }

  public String getDomain() {
    return domain;
  }

  public String getXmiFile() {
    return xmiFile;
  }

  public boolean isLabelMapping() {
    return labelMapping;
  }

  /**
   * Returns the object for this element or null, if this element does not create an object.
   * 
   * @return the object.
   * @throws SAXException
   *           if there is a parsing error.
   */
  public Object getObject() throws SAXException {
    return null;
  }

  public IPmdConnectionProvider getConnectionProvider() {
    return new PentahoPmdConnectionProvider();
  }

}
