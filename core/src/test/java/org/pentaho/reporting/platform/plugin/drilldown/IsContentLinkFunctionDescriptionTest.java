/*!
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2002-2017 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin.drilldown;

import junit.framework.TestCase;
import org.pentaho.reporting.libraries.formula.function.userdefined.UserDefinedFunctionCategory;
import org.pentaho.reporting.libraries.formula.typing.coretypes.AnyType;
import org.pentaho.reporting.libraries.formula.typing.coretypes.LogicalType;

public class IsContentLinkFunctionDescriptionTest extends TestCase {
  IsContentLinkFunctionDescription functionDescription;

  protected void setUp() {
    functionDescription = new IsContentLinkFunctionDescription();
  }

  public void testGetValueType() throws Exception {
    assertTrue( functionDescription.getValueType() instanceof LogicalType );
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
