package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.LinkedHashMap;

public class ParameterGroup
{
  private String name;
  private String label;
  private LinkedHashMap<String, Parameter> parameters;

  public ParameterGroup(final String name, final String parameterGroupLabel)
  {
    this.name = name;
    this.label = parameterGroupLabel;
    this.parameters = new LinkedHashMap<String, Parameter>();
  }

  public String getLabel()
  {
    return label;
  }

  public String getName()
  {
    return name;
  }

  public void addParameter(final Parameter parameter)
  {
    parameters.put(parameter.getName(), parameter);
  }

  public Parameter getParameter(final String parameter)
  {
    return parameters.get(parameter);
  }

  public Parameter[] getParameters()
  {
    return parameters.values().toArray(new Parameter[parameters.size()]);
  }
}
