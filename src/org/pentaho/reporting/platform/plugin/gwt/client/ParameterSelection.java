package org.pentaho.reporting.platform.plugin.gwt.client;

public class ParameterSelection
{
  private String label;
  private String type;
  private boolean selected;
  private String value;

  public ParameterSelection(final String type, final String value, final boolean selected, final String label)
  {
    this.type = type;
    this.value = value;
    this.selected = selected;
    this.label = label;
  }

  public String getLabel()
  {
    return label;
  }

  public String getType()
  {
    return type;
  }

  public boolean isSelected()
  {
    return selected;
  }

  public String getValue()
  {
    return value;
  }
}
