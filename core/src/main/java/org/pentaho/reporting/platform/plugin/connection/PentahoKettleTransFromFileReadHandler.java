/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2028-08-13
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin.connection;

import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.KettleTransformationProducer;
import org.pentaho.reporting.engine.classic.extensions.datasources.kettle.parser.KettleTransFromFileReadHandler;

public class PentahoKettleTransFromFileReadHandler extends KettleTransFromFileReadHandler {
  public PentahoKettleTransFromFileReadHandler() {
  }

  /**
   * Returns the object for this element or null, if this element does not create an object.
   * 
   * @return the object.
   */
  public KettleTransformationProducer getObject() {
    return new PentahoKettleTransFromFileProducer( getRepositoryName(), getFileName(), getStepName(), getUsername(),
        getPassword(), getDefinedArgumentNames(), getDefinedVariableNames() );
  }
}
