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


package org.pentaho.reporting.platform.plugin.cache;

import java.io.Serializable;

/**
 * Simple interface for caheable report representation
 */
public interface IReportContent extends Serializable {

  int getPageCount();

  int getStoredPageCount();

  byte[] getPageData( final int page );

}
