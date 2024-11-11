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

import org.junit.Assert;
import org.junit.Test;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.TenantUtils;
import org.pentaho.reporting.libraries.base.config.DefaultConfiguration;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.mockito.Mockito.mock;

public class PentahoReportEnvironmentTest {

  @Test( expected = NullPointerException.class )
  public void getNullProp() {
    new PentahoReportEnvironment( new DefaultConfiguration() ).getEnvironmentProperty( null );
  }

  @Test
  public void getCl() {
    final String clText = UUID.randomUUID().toString();
    Assert.assertEquals( clText, new PentahoReportEnvironment( new DefaultConfiguration(), clText )
      .getEnvironmentProperty( "contentLink" ) );
  }

  @Test
  public void testNotAppContext() {
    final String[] props = new String[] { "serverBaseURL",
      "pentahoBaseURL",
      "solutionRoot",
      "hostColonPort",
      "requestContextPath" };

    for ( final String prop : props ) {
      Assert.assertNull( new PentahoReportEnvironment( new DefaultConfiguration() )
        .getEnvironmentProperty( prop ) );
    }
  }

  @Test( expected = NullPointerException.class )
  public void testNull_1() {
    new PentahoReportEnvironment( null, "", "" );
  }

  @Test
  public void testNull_2() {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), null, "" );
    Assert.assertNotNull( pre );
    Assert.assertEquals( "", pre.getRepositoryPath() );
    Assert.assertNull( pre.getEnvironmentProperty( "contentLink" ) );
  }

  @Test
  public void testNull_3() {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", null );
    Assert.assertNotNull( pre );
    Assert.assertEquals( "", pre.getEnvironmentProperty( "contentLink" ) );
    Assert.assertNull( pre.getRepositoryPath() );
  }

  @Test
  public void testRepoPath_1() throws Exception {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", "/123/qwe/123.prpt" );
    Method m = PentahoReportEnvironment.class.getDeclaredMethod( "getRepositoryPath", new Class[] { boolean.class } );
    m.setAccessible( true );
    String result = String.valueOf( m.invoke( pre, new Object[] { false } ) );
    Assert.assertEquals( result, ":123:qwe:123.prpt" );
  }

  @Test
  public void testRepoPath_2() throws Exception {
    PentahoReportEnvironment pre = new PentahoReportEnvironment( new DefaultConfiguration(), "", "/123/qwe/123.prpt" );
    Method m = PentahoReportEnvironment.class.getDeclaredMethod( "getRepositoryPath", new Class[] { boolean.class } );
    m.setAccessible( true );
    String result = String.valueOf( m.invoke( pre, new Object[] { true } ) );
    Assert.assertEquals( result, "%3A123%3Aqwe%3A123.prpt" );
  }

  @Test
  public void testSessionTenantId() {
    String tenantIdProperty = "session:" + IPentahoSession.TENANT_ID_KEY;
    String defaultTenantId = "/pentaho/" + TenantUtils.TENANTID_SINGLE_TENANT;
    IPentahoSession pentahoSession = mock( IPentahoSession.class );
    PentahoSessionHolder.setSession( pentahoSession );
    Assert.assertEquals( defaultTenantId, new PentahoReportEnvironment( new DefaultConfiguration() )
            .getEnvironmentProperty( tenantIdProperty ) );
    PentahoSessionHolder.removeSession();
  }

  @Test
  public void testNullSessionTenantId() {
    String tenantIdProperty = "session:" + IPentahoSession.TENANT_ID_KEY;
    PentahoSessionHolder.removeSession();
    Assert.assertNull( new PentahoReportEnvironment( new DefaultConfiguration() )
            .getEnvironmentProperty( tenantIdProperty ) );
  }
}
