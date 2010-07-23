package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 18:08:49
 *
 * @author Thomas Morgner.
 */
public class ParameterValues
{
  private HashMap<String, ArrayList<String>> backend;

  public ParameterValues()
  {
    backend = new HashMap<String,ArrayList<String>>();
  }

  public void setSelectedValue (final String parameter, final String value)
  {
    if (value == null)
    {
      setSelectedValues(parameter, new String[0]);
    }
    else
    setSelectedValues(parameter, new String[]{value});
  }

  public void setSelectedValues (final String parameter, final String[] values)
  {
    if (values.length == 0)
    {
      backend.remove(parameter);
      return;
    }

    ArrayList<String> strings = backend.get(parameter);
    if (strings == null)
    {
      strings = new ArrayList<String>();
      backend.put(parameter, strings);
    }
    strings.clear();
    for (int i = 0; i < values.length; i++)
    {
      final String value = values[i];
      strings.add(value);
    }
  }

  public void addSelectedValue (final String parameter, final String value)
  {
    if (value == null)
    {
      throw new NullPointerException();
    }

    ArrayList<String> strings = backend.get(parameter);
    if (strings == null)
    {
      strings = new ArrayList<String>();
      backend.put(parameter, strings);
    }
    strings.add(value);
  }
  
  public void removeSelectedValue (final String parameter, final String value)
  {
    if (value == null)
    {
      throw new NullPointerException();
    }

    final ArrayList<String> strings = backend.get(parameter);
    if (strings == null)
    {
      return;
    }
    strings.remove(value);
    if (strings.isEmpty())
    {
      backend.remove(parameter);
    }
  }

  public String[] getParameterNames()
  {
    return backend.keySet().toArray(new String[backend.size()]);
  }

  public String[] getParameterValues(final String name)
  {
    final ArrayList<String> strings = backend.get(name);
    if (strings == null)
    {
      return new String[0];
    }
    return strings.toArray(new String[strings.size()]);
  }

  public boolean containsParameter(final String name)
  {
    return backend.containsKey(name);
  }
}
