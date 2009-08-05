package org.pentaho.reporting.platform.plugin.output;

import org.pentaho.reporting.engine.classic.core.layout.output.FlowSelector;
import org.pentaho.reporting.engine.classic.core.layout.output.LogicalPageKey;

public class ReportPageSelector implements FlowSelector
{

  private int acceptedPage;

  public ReportPageSelector(int acceptedPage)
  {
    this.acceptedPage = acceptedPage;
  }

  public boolean isLogicalPageAccepted(LogicalPageKey key)
  {
    if (key == null)
    {
      return false;
    }
    return key.getPosition() == acceptedPage;
  }
}
