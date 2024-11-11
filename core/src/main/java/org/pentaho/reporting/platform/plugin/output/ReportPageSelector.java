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


package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;

/**
 * @deprecated
 */
public class ReportPageSelector extends SinglePageFlowSelector {
  public ReportPageSelector( final int acceptedPage ) {
    super( acceptedPage, true );
  }

  public ReportPageSelector( final int acceptedPage, final boolean logicalPage ) {
    super( acceptedPage, logicalPage );
  }
}
