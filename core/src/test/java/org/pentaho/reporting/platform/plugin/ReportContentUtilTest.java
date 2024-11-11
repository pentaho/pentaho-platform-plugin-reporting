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