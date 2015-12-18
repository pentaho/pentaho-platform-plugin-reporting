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
 * Copyright (c) 2002-2015 Pentaho Corporation..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import junit.framework.TestCase;

import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultParameterDefinition;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;

public class ReportContentUtilTest extends TestCase {

  SimpleReportingComponent rc;

  protected void setUp() {
    // create an instance of the component
    rc = new SimpleReportingComponent();
  }

  public void testApplyInputsToReportParameters() throws Exception {

    Map<String, Object> inputs = new HashMap<String, Object>();
    DefaultListParameter param = new DefaultListParameter(
        "Status", "BC_ORDERS_STATUS", "BC_ORDERS_STATUS", "Status", true, true, String.class );
    String defaultValue = "item 1, item 2";
    param.setDefaultValue( defaultValue );

    DefaultParameterDefinition parameterDefinition = new DefaultParameterDefinition();
    parameterDefinition.addParameterDefinition( param );
    MasterReport report = new MasterReport();
    report.setParameterDefinition( parameterDefinition );
    DefaultParameterContext parameterContext = new DefaultParameterContext( report );

    ReportContentUtil.applyInputsToReportParameters( report, parameterContext, inputs, new ValidationResult() );

    assertTrue( report.getParameterDefinition().getParameterDefinitions()[0].getDefaultValue( parameterContext ).getClass().isArray() );
    assertEquals( 2, Array.getLength( report.getParameterDefinition().getParameterDefinitions()[0].getDefaultValue( parameterContext ) ) );
  }
}