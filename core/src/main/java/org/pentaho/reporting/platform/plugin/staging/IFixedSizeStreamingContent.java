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


package org.pentaho.reporting.platform.plugin.staging;

import java.io.InputStream;

public interface IFixedSizeStreamingContent {

  /**
   * Creates input stream from staging content.
   * This stream must be closed manually.
   *
   * @return
   */
  InputStream getStream();
  long getContentSize();
  boolean cleanContent();

}
