/*
 * Copyright 2007 Pentaho Corporation.  All rights reserved. 
 * This software was developed by Pentaho Corporation and is provided under the terms 
 * of the Mozilla Public License, Version 1.1, or any later version. You may not use 
 * this file except in compliance with the license. If you need a copy of the license, 
 * please go to http://www.mozilla.org/MPL/MPL-1.1.txt. The Original Code is the Pentaho 
 * BI Platform.  The Initial Developer is Pentaho Corporation.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS" 
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or  implied. Please refer to 
 * the license for the specific language governing your rights and limitations.
 *
 * @created Apr 8, 2009 
 * @author wseyler
 */

package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.ConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.parser.data.sql.ConnectionReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.AbstractXmlReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.StringReadHandler;
import org.pentaho.reporting.libraries.xmlns.parser.XmlReadHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


/**
 * @author wseyler
 */
public class PentahoJndiConnectionReadHandler extends AbstractXmlReadHandler implements ConnectionReadHandler
{

  private ConnectionProvider connectionProvider;
  private StringReadHandler pathReadHandler;
  private StringReadHandler usernameReadHandler;
  private StringReadHandler passwordReadHandler;

  /*
  * Default constructor
  */
  public PentahoJndiConnectionReadHandler()
  {
    super();
  }


  /**
   * Returns the handler for a child element.
   *
   * @param tagName the tag name.
   * @param atts    the attributes.
   * @return the handler or null, if the tagname is invalid.
   * @throws SAXException if there is a parsing error.
   */
  protected XmlReadHandler getHandlerForChild(final String uri,
                                              final String tagName,
                                              final Attributes atts)
      throws SAXException
  {
    if (isSameNamespace(uri) == false)
    {
      return null;
    }
    if ("path".equals(tagName)) //$NON-NLS-1$
    {
      pathReadHandler = new StringReadHandler();
      return pathReadHandler;
    }
    if ("username".equals(tagName)) //$NON-NLS-1$
    {
      usernameReadHandler = new StringReadHandler();
      return usernameReadHandler;
    }
    if ("password".equals(tagName)) //$NON-NLS-1$
    {
      passwordReadHandler = new StringReadHandler();
      return passwordReadHandler;
    }
    return null;
  }

  /**
   * Done parsing.
   *
   * @throws SAXException if there is a parsing error.
   */
  protected void doneParsing() throws SAXException
  {
    final PentahoJndiDatasourceConnectionProvider provider = new PentahoJndiDatasourceConnectionProvider();
    if (pathReadHandler != null)
    {
      provider.setJndiName(pathReadHandler.getResult());
    }
    if (usernameReadHandler != null)
    {
      provider.setUsername(usernameReadHandler.getResult());
    }
    if (passwordReadHandler != null)
    {
      provider.setPassword(passwordReadHandler.getResult());
    }
    this.connectionProvider = provider;
  }

  /* (non-Javadoc)
   * @see org.jfree.report.modules.parser.sql.ConnectionReadHandler#getProvider()
   */
  public ConnectionProvider getProvider()
  {
    return connectionProvider;
  }

  /* (non-Javadoc)
   * @see org.jfree.xmlns.parser.XmlReadHandler#getObject()
   */
  public Object getObject() throws SAXException
  {
    return connectionProvider;
  }

}
