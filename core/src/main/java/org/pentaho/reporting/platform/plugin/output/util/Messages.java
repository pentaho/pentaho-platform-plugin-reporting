/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.reporting.platform.plugin.output.util;

import org.pentaho.platform.util.messages.MessagesBase;

public class Messages extends MessagesBase {
  private static final String BUNDLE_NAME = Messages.class.getPackage().getName() + ".messages";

  private static final Messages instance = new Messages();

  private Messages() {
    super( BUNDLE_NAME );
  }

  public static Messages getInstance() {
    return instance;
  }

}
