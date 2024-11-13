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


package org.pentaho.reporting.platform.plugin.drilldown;

import org.pentaho.reporting.libraries.formula.function.AbstractFunctionDescription;
import org.pentaho.reporting.libraries.formula.function.FunctionCategory;
import org.pentaho.reporting.libraries.formula.function.userdefined.UserDefinedFunctionCategory;
import org.pentaho.reporting.libraries.formula.typing.Type;
import org.pentaho.reporting.libraries.formula.typing.coretypes.AnyType;
import org.pentaho.reporting.libraries.formula.typing.coretypes.LogicalType;

public class IsContentLinkFunctionDescription extends AbstractFunctionDescription {
  public IsContentLinkFunctionDescription() {
    super( "ISCONTENTLINK", "org.pentaho.reporting.platform.plugin.drilldown.IsContentLink-Function.properties" );
  }

  public Type getValueType() {
    return LogicalType.TYPE;
  }

  public FunctionCategory getCategory() {
    return UserDefinedFunctionCategory.CATEGORY;
  }

  public int getParameterCount() {
    return 1;
  }

  public Type getParameterType( final int position ) {
    return AnyType.ANY_ARRAY;
  }

  public boolean isParameterMandatory( final int position ) {
    return true;
  }
}
