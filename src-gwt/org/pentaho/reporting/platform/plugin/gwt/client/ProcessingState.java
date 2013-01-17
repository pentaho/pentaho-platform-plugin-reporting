package org.pentaho.reporting.platform.plugin.gwt.client;

public class ProcessingState
{
  private int page;
  private int totalPages;

  public ProcessingState()
  {
  }

  public int getPage()
  {
    return page;
  }

  public void setPage(final int page)
  {
    this.page = page;
  }

  public int getTotalPages()
  {
    return totalPages;
  }

  public void setTotalPages(final int totalPages)
  {
    this.totalPages = totalPages;
  }
}
