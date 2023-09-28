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
 * Copyright (c) 2002-2023 Hitachi Vantara..  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterAttributeNames;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterContext;
import org.pentaho.reporting.engine.classic.core.parameters.ParameterDefinitionEntry;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pentaho.di.core.util.Assert.assertTrue;

public class ReportContentUtilTest {

  @Mock
  private ParameterContext context;

  private ParameterDefinitionEntry pde = Mockito.mock( ParameterDefinitionEntry.class );

  @Test
  public void parseDateStrict_null_Test() throws Exception {

    when( pde.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.TIMEZONE, null ) ).thenReturn( null );

    validateParseDateStrict( "2018-05-20T01:12:23.456-0400", "Sun May 20 01:12:23 UTC 2018" );
  }

  @Test
  public void parseDateStrict_GMTplus3_Test() throws Exception {

    when( pde.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.TIMEZONE, null ) ).thenReturn( "Etc/GMT+3" );

    validateParseDateStrict( "2018-05-20T01:12:23.456-0400", "Sun May 20 04:12:23 UTC 2018" );
  }

  @Test
  public void parseDateStrict_GMTminus7_Test() throws Exception {

    when( pde.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.TIMEZONE, null ) ).thenReturn( "Etc/GMT-7" );

    validateParseDateStrict( "2018-05-20T01:12:23.456-0400", "Sat May 19 18:12:23 UTC 2018" );
  }

  @Test
  public void parseDateStrict_server_Test() throws Exception {

    when( pde.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.TIMEZONE, null ) ).thenReturn( "client" );

    when( pde.getParameterAttribute( ParameterAttributeNames.Core.NAMESPACE,
      ParameterAttributeNames.Core.DATA_FORMAT, null ) ).thenReturn( "yyyy-MM-dd" );

    validateParseDateStrict( "2018-05-20T00:00:00.000-0400", "Sun May 20 00:00:00 UTC 2018" );
  }

  private void validateParseDateStrict( final String dateToParse, final String dateResult ) throws Exception {
    ReportContentUtil reportContentUtil = new ReportContentUtil();

    Date date = reportContentUtil.parseDateStrict( pde, context, dateToParse );
    assertEquals( dateResult, date.toString() );
  }
}