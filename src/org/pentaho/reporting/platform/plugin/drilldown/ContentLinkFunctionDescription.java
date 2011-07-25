package org.pentaho.reporting.platform.plugin.drilldown;

import org.pentaho.reporting.libraries.formula.function.AbstractFunctionDescription;
import org.pentaho.reporting.libraries.formula.function.FunctionCategory;
import org.pentaho.reporting.libraries.formula.function.userdefined.UserDefinedFunctionCategory;
import org.pentaho.reporting.libraries.formula.typing.Type;
import org.pentaho.reporting.libraries.formula.typing.coretypes.AnyType;
import org.pentaho.reporting.libraries.formula.typing.coretypes.TextType;

public class ContentLinkFunctionDescription extends AbstractFunctionDescription
{
  public ContentLinkFunctionDescription()
  {
    super("CONTENTLINK", "org.pentaho.reporting.platform.plugin.drilldown.ContentLink-Function.properties");
  }

  public Type getValueType()
  {
    return TextType.TYPE;
  }

  public FunctionCategory getCategory()
  {
    return UserDefinedFunctionCategory.CATEGORY;
  }

  public int getParameterCount()
  {
    return 1;
  }

  public Type getParameterType(final int position)
  {
    return AnyType.ANY_ARRAY;
  }

  public boolean isParameterMandatory(final int position)
  {
    return true;
  }
}
