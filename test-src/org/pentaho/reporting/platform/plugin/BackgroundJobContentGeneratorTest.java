/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License, version 2 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/gpl-2.0.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 *
 * Copyright 2006 - 2016 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.platform.plugin;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.audit.MessageTypes;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlTableModule;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportExecution;
import org.pentaho.reporting.platform.plugin.async.IAsyncReportState;
import org.pentaho.reporting.platform.plugin.async.IPentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.PentahoAsyncExecutor;
import org.pentaho.reporting.platform.plugin.async.ReportListenerThreadHolder;
import org.pentaho.reporting.platform.plugin.staging.IFixedSizeStreamingContent;
import org.pentaho.test.platform.engine.core.SimpleObjectFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test basic functionality and audit usage.
 * <p>
 * Created by dima.prokopenko@gmail.com on 4/21/2016.
 */
public class BackgroundJobContentGeneratorTest {

  public static final String instanceId = "instanceId";
  public static final String sessionId = "sessionId";
  public static final String sessionName = "junitName";
  public static final String path = "junitPath";
  public static final String fieldId = "junitFieldId";

  public static final UUID uuid = UUID.randomUUID();

  IApplicationContext appContext = mock( IApplicationContext.class );
  SimpleObjectFactory factory = new SimpleObjectFactory();
  SimpleReportingComponent component = mock( SimpleReportingComponent.class );

  IPentahoSession session = mock( IPentahoSession.class );
  IParameterProvider paramProvider = mock( IParameterProvider.class );
  HttpServletResponse httpResponse = mock( HttpServletResponse.class );

  Map<String, IParameterProvider> parameterProviders;

  @Before
  public void before() throws ObjectFactoryException {
    Path customStaging =
      Paths.get( System.getProperty( "java.io.tmpdir" ), BackgroundJobContentGeneratorTest.class.getSimpleName() );

    when( appContext.getSolutionPath( anyString() ) ).thenReturn( customStaging.toString() );

    //register mock executor to not execute anything
    factory.defineObject( PentahoAsyncExecutor.BEAN_NAME, MockPentahoAsyncExecutor.class.getName() );

    PentahoSystem.setApplicationContext( appContext );
    PentahoSystem.registerPrimaryObjectFactory( factory );

    when( session.getId() ).thenReturn( sessionId );
    when( session.getName() ).thenReturn( sessionName );

    parameterProviders = new HashMap<>();
    parameterProviders.put( "path", paramProvider );
    when( paramProvider.getParameter( "httpresponse" ) ).thenReturn( httpResponse );
  }

  @Test
  public void testSuccessExecutionAudit() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );

    when( component.validate() ).thenReturn( true );

    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    // execution attempt registered
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.EXECUTION ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    verify( generator, times( 0 ) ).sendErrorResponse();
    verify( generator, times( 1 ) ).sendSuccessRedirect( eq( uuid ) );

    verify( httpResponse ).sendRedirect( eq( BackgroundJobReportContentGenerator.REDIRECT_PREFIX + uuid.toString()
      + BackgroundJobReportContentGenerator.REDIRECT_POSTFIX ) );

    assertEquals( "BACKLOG-6745", instanceId, ReportListenerThreadHolder.getRequestId() );

  }

  @Test
  public void testFailedExecutionAudit() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );

    // validation will not pass
    when( component.validate() ).thenReturn( false );

    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    // execution attempt registered
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.EXECUTION ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    // then fail registered also.
    verify( wrapper, times( 1 ) ).audit(
      eq( sessionId ),
      eq( sessionName ),
      eq( path ),
      eq( generator.getClass().getName() ),
      eq( generator.getClass().getName() ),
      eq( MessageTypes.FAILED ),
      eq( instanceId ),
      eq( "" ),
      eq( (float) 0 ),
      eq( generator )
    );

    verify( generator, times( 0 ) ).sendSuccessRedirect( any( UUID.class ) );
    verify( generator, times( 1 ) ).sendErrorResponse();
  }

  @Test
  public void testSimpleReportingComponentInitialized() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    AuditWrapper wrapper = mock( AuditWrapper.class );

    generator.createReportContent( fieldId, path, component, wrapper );

    verify( component, times( 1 ) ).setReportFileId( eq( fieldId ) );
    verify( component, times( 0 ) ).setReportDefinitionPath( anyString() );
    verify( component, times( 0 ) ).setReportDefinitionInputStream( any( InputStream.class ) );

    verify( component, times( 1 ) ).setPaginateOutput( eq( true ) );
    verify( component, times( 1 ) ).setForceDefaultOutputTarget( eq( false ) );
    verify( component, times( 1 ) ).setDefaultOutputTarget( eq( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE ) );

    // this is not a prpti report
    verify( component, times( 0 ) ).setForceUnlockPreferredOutput( anyBoolean() );
  }

  @Test
  public void testSimpleReportingComponentInitializedPrpti() throws Exception {
    BackgroundJobReportContentGenerator generator = spy( new BackgroundJobReportContentGenerator() );
    generator.setParameterProviders( parameterProviders );
    generator.setSession( session );
    generator.setInstanceId( instanceId );
    when( component.validate() ).thenReturn( true );
    AuditWrapper wrapper = mock( AuditWrapper.class );

    // we determine prpti report by the enc of the string
    generator.createReportContent( fieldId, path + ".prpti", component, wrapper );

    verify( component, times( 1 ) ).setReportFileId( eq( fieldId ) );
    verify( component, times( 0 ) ).setReportDefinitionPath( anyString() );
    verify( component, times( 0 ) ).setReportDefinitionInputStream( any( InputStream.class ) );

    verify( component, times( 1 ) ).setPaginateOutput( eq( true ) );
    verify( component, times( 1 ) ).setForceDefaultOutputTarget( eq( false ) );
    verify( component, times( 1 ) ).setDefaultOutputTarget( eq( HtmlTableModule.TABLE_HTML_PAGE_EXPORT_TYPE ) );

    // this is prpti report
    verify( component, times( 1 ) ).setForceUnlockPreferredOutput( eq( true ) );
  }

  @AfterClass
  public static void afterClass() {
    PentahoSystem.shutdown();
  }

  /**
   * We can't use mock for this class. Since we obtain it through pentaho factory - it should be public available class,
   * not a particular mocked object.
   */
  public static class MockPentahoAsyncExecutor implements IPentahoAsyncExecutor {

    @Override public Future<IFixedSizeStreamingContent> getFuture( UUID id, IPentahoSession session ) {
      return null;
    }

    @Override public void cleanFuture( UUID id, IPentahoSession session ) {

    }

    @Override public UUID addTask( IAsyncReportExecution task, IPentahoSession session ) {
      return uuid;
    }

    @Override public IAsyncReportState getReportState( UUID id, IPentahoSession session ) {
      return null;
    }

    @Override public void requestPage( UUID id, IPentahoSession session, int page ) {

    }

    @Override public void schedule( UUID uuid, IPentahoSession session ) {

    }

    @Override
    public void shutdown() {

    }
  }
}
