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
 * Copyright (c) 2002-2016 Pentaho Corporation..  All rights reserved.
 */
package org.pentaho.reporting.platform.plugin;


import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.parameters.DefaultListParameter;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;
import org.pentaho.reporting.engine.classic.core.parameters.ReportParameterDefinition;
import org.pentaho.reporting.engine.classic.core.parameters.ValidationResult;
import org.pentaho.reporting.engine.classic.core.util.ReportParameterValues;
public class ReportContentUtilTest {

  @Test
  public void applyInputsToReportParametersTest() {
    try {
      ParameterContext context = mock( ParameterContext.class );
      MasterReport report = mock( MasterReport.class );
      ReportParameterDefinition paramDef = mock( ReportParameterDefinition.class );
      ReportParameterValues paramValue = mock( ReportParameterValues.class );
      ParameterDefinitionEntry[] paramDefEntry = new ParameterDefinitionEntry[1];
      DefaultListParameter dfp = new DefaultListParameter( "get prefered ranges", "is", "label", "predef_range",
        false, false, Integer.class );
      dfp.setDefaultValue( 17 );
      paramDefEntry[0] = dfp;
      doReturn( paramDef ).when( report ).getParameterDefinition();
      doReturn( paramValue ).when( report ).getParameterValues();
      doReturn( paramDefEntry ).when( paramDef ).getParameterDefinitions();

      Map<String, Object> inputs = new HashMap<String, Object>();
      inputs.put( "predef_range", "17<script>alert(33334)</script>" );

      ValidationResult validationResult = new ValidationResult();
      ValidationResult vr = ReportContentUtil.applyInputsToReportParameters( report, context, inputs, validationResult );

      assertFalse( vr.getErrors( "predef_range" )[0].getMessage().contains( "<script>alert(33334)</script>" ) );
    } catch ( Exception e ) {
      e.printStackTrace();
    }
  }
}
