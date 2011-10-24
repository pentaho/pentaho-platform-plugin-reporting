package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.SinglePageFlowSelector;

/**
 * @deprecated 
 */
public class ReportPageSelector extends SinglePageFlowSelector
{
  public ReportPageSelector(final int acceptedPage)
  {
    super(acceptedPage, true);
  }

  public ReportPageSelector(final int acceptedPage, final boolean logicalPage)
  {
    super(acceptedPage, logicalPage);
  }
}
