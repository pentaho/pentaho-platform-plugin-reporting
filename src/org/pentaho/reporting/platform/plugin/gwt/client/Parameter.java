package org.pentaho.reporting.platform.plugin.gwt.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Todo: Document me!
 * <p/>
 * Date: 22.07.2010
 * Time: 13:47:30
 *
 * @author Thomas Morgner.
 */
public class Parameter
{
  public static final String CORE_NAMESPACE =
      "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core";

  private ArrayList<ParameterSelection> selections;
  private String name;
  private String type;
  private HashMap<String, HashMap<String, String>> attributes;
  private boolean strict;
  private boolean multiSelect;
  private boolean mandatory;
  private String timezoneHint;
  private boolean list;

  public Parameter(final String name)
  {
    this.name = name;
    this.selections = new ArrayList<ParameterSelection>();
    this.attributes = new HashMap<String, HashMap<String, String>>();
  }

  public void addSelection(final ParameterSelection selection)
  {
    this.selections.add(selection);
  }

  public boolean hasValues()
  {
    return selections.isEmpty() == false;
  }

  public String getTimezoneHint()
  {
    return timezoneHint;
  }

  public void setTimezoneHint(final String timezoneHint)
  {
    this.timezoneHint = timezoneHint;
  }

  public void setAttribute(final String namespace, final String name, final String value)
  {
    HashMap<String, String> hashMap = attributes.get(namespace);
    if (hashMap == null)
    {
      hashMap = new HashMap<String, String>();
      attributes.put(namespace, hashMap);
    }
    hashMap.put(name, value);
  }

  public String getAttribute(final String namespace, final String name)
  {
    final HashMap<String, String> hashMap = attributes.get(namespace);
    if (hashMap == null)
    {
      return null;
    }
    return hashMap.get(name);
  }

  public String getName()
  {
    return name;
  }

  public String getLabel()
  {
    final String attribute = getAttribute(CORE_NAMESPACE, "label"); // NON-NLS
    if (ReportViewerUtil.isEmpty(attribute))
    {
      return name;
    }
    return attribute;
  }

  public String getTooltip()
  {
    return getAttribute(CORE_NAMESPACE, "tooltip"); // NON-NLS
  }

  public boolean isStrict()
  {
    return strict; // NON-NLS
  }

  public boolean isMultiSelect()
  {
    return multiSelect; // NON-NLS
  }

  public boolean isMandatory()
  {
    return mandatory; // NON-NLS
  }

  public void setStrict(final boolean strict)
  {
    this.strict = strict;
  }

  public void setMultiSelect(final boolean multiSelect)
  {
    this.multiSelect = multiSelect;
  }

  public void setMandatory(final boolean mandatory)
  {
    this.mandatory = mandatory;
  }

  public boolean isList()
  {
    return list;
  }

  public void setList(final boolean list)
  {
    this.list = list;
  }

  public String getAttribute(final String name)
  {
    return getAttribute(CORE_NAMESPACE, name);
  }

  public List<ParameterSelection> getSelections()
  {
    return selections;
  }

  public boolean isSelectedValue(final String selection)
  {
    for (int i = 0; i < selections.size(); i++)
    {
      final ParameterSelection parameterSelection = selections.get(i);
      if (parameterSelection.isSelected() == false)
      {
        continue;
      }
      if (selection == null && parameterSelection.getValue() == null)
      {
        return true;
      }
      if (selection != null && selection.equals(parameterSelection.getValue()))
      {
        return true;
      }
    }
    return false;
  }

  public boolean isHidden()
  {
    return "true".equals(getAttribute(CORE_NAMESPACE, "hidden"));
  }

  public String getType()
  {
    return type;
  }

  public void setType(final String type)
  {
    this.type = type;
  }
}
