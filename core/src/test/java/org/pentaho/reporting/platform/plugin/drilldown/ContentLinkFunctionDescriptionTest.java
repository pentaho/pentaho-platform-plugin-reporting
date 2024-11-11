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

import junit.framework.TestCase;
import org.pentaho.reporting.libraries.formula.function.userdefined.UserDefinedFunctionCategory;
import org.pentaho.reporting.libraries.formula.typing.coretypes.AnyType;
import org.pentaho.reporting.libraries.formula.typing.coretypes.TextType;

public class ContentLinkFunctionDescriptionTest extends TestCase {
  ContentLinkFunctionDescription functionDescription;

  protected void setUp() {
    functionDescription = new ContentLinkFunctionDescription();
  }

  public void testGetValueType() throws Exception {
    assertTrue( functionDescription.getValueType() instanceof TextType );
  }

  public void testGetCategory() throws Exception {
    assertTrue( functionDescription.getCategory() instanceof UserDefinedFunctionCategory );
  }

  public void testGetParameterType() throws Exception {
    assertTrue( functionDescription.getParameterType( 0 ) instanceof AnyType );
  }

  public void testGetParameterCount() throws Exception {
    assertEquals( 1, functionDescription.getParameterCount() );
  }

  public void testIsParameterMandatory() throws Exception {
    assertTrue( functionDescription.isParameterMandatory( 0 ) );
  }
}
